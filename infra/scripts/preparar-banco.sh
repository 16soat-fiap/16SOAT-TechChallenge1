#!/usr/bin/env bash
# =============================================================================
# Prepara um banco recém-migrado: cria os usuários da aplicação e da Lambda,
# dá as permissões e, em homologação, carrega os dados de demonstração.
#
#   AMBIENTE=homologacao ./infra/scripts/preparar-banco.sh
#
# É o passo 2 do primeiro deploy de cada banco (ver deploy.sh):
#
#   SOMENTE_MIGRACAO=sim ./infra/scripts/deploy.sh   # 1. cria as tabelas
#   ./infra/scripts/preparar-banco.sh                # 2. usuários e permissões
#   PULAR_MIGRACAO=sim   ./infra/scripts/deploy.sh   # 3. sobe a aplicação
#
# Pode rodar de novo: os scripts do banco são idempotentes, e os dados de
# demonstração só entram se o banco ainda não tiver clientes.
#
# ─── Por que um Job no cluster ───────────────────────────────────────────────
#
# O RDS fica em subnet privada, sem bastion: o único lugar de onde se alcança o
# banco é de dentro da VPC. O Job roda o psql lá, com os scripts do repositório
# autopecas-infra-db SEM alteração — exceto a linha `\password <usuário>`, que
# pede a senha no terminal e não funciona sem um. Ela é trocada pelo mesmo
# comando que o `\password` enviaria: um ALTER ROLE com o verificador
# SCRAM-SHA-256, calculado num initContainer. A senha em si não chega ao
# servidor nem aparece em SQL.
#
# ─── Entradas ────────────────────────────────────────────────────────────────
#
#   DB_REPO              clone do autopecas-infra-db (padrão: ../autopecas-infra-db)
#   DB_ADMIN_USERNAME    dono do banco, que executa os scripts
#   DB_ADMIN_PASSWORD
#   APP_DB_PASSWORD      senha que o autopecas_app passa a ter (a mesma do deploy.sh)
#   LAMBDA_DB_PASSWORD   senha que o lambda_auth passa a ter (a mesma do
#                        TF_VAR_db_password no apply do autopecas-lambda-auth)
#   DADOS_DEMO           sim | nao. Padrão: sim em homologação, nao em produção.
#                        Clientes, veículos, funcionários e OS de exemplo do
#                        V3__seed_demo.sql, e as permissões dos funcionários
#                        de exemplo — sem elas, nenhum funcionário consegue
#                        login na Lambda.
#
# As senhas viram um Secret por entrada padrão (stdin), que é apagado ao final
# junto com o Job.
# =============================================================================

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

AMBIENTE="${AMBIENTE:-homologacao}"
case "$AMBIENTE" in
  homologacao) DADOS_DEMO="${DADOS_DEMO:-sim}" ;;
  producao)    DADOS_DEMO="${DADOS_DEMO:-nao}" ;;
  *) morrer "AMBIENTE deve ser 'homologacao' ou 'producao' (recebido: '${AMBIENTE}')." ;;
esac
NAMESPACE="$AMBIENTE"
DB_REPO="${DB_REPO:-${REPO_ROOT}/../autopecas-infra-db}"

for var in DB_ADMIN_USERNAME DB_ADMIN_PASSWORD APP_DB_PASSWORD LAMBDA_DB_PASSWORD; do
  if [ -z "${!var:-}" ]; then
    morrer "${var} não definida (ver o cabeçalho deste script)."
  fi
done

SCRIPTS_DB="${DB_REPO}/scripts"
for f in criar-usuario-lambda.sql criar-usuario-aplicacao.sql permissoes-aplicacao.sql verificar-permissoes.sql; do
  if [ ! -f "${SCRIPTS_DB}/${f}" ]; then
    morrer "Não encontrei ${SCRIPTS_DB}/${f} — clone o autopecas-infra-db e aponte DB_REPO para ele."
  fi
done
SEED="${REPO_ROOT}/src/main/resources/db/demo/V3__seed_demo.sql"

exigir_docker
exigir_aws_configurada

# ─────────────────────────────────────────────────────────────────────────────
titulo "1/3  Lendo o ambiente ${AMBIENTE}"

CLUSTER="$(output_remoto k8s/cluster/terraform.tfstate cluster_name)"
DB_HOST="$(output_remoto "db/${AMBIENTE}/terraform.tfstate" db_endpoint)"
[ -n "$CLUSTER" ] || morrer "Cluster não encontrado — o autopecas-infra-k8s foi aplicado nesta conta?"
[ -n "$DB_HOST" ] || morrer "Banco de ${AMBIENTE} não encontrado — o autopecas-infra-db foi aplicado?"
# O output pode vir como host:porta; o psql quer os dois separados.
DB_PORTA="5432"
if [[ "$DB_HOST" == *:* ]]; then
  DB_PORTA="${DB_HOST##*:}"
  DB_HOST="${DB_HOST%:*}"
fi
ok "Banco     ${DB_HOST}:${DB_PORTA}"
ok "Scripts   ${SCRIPTS_DB}"
if [ "$DADOS_DEMO" = "sim" ]; then
  ok "Dados de demonstração: sim (só entram se o banco não tiver clientes)"
else
  aviso "Dados de demonstração: não. Para um funcionário logar, conceda as permissões dele"
  aviso "com o ProvisionarFuncionario do repositório autopecas-lambda-auth."
fi

awscli eks update-kubeconfig --name "$CLUSTER" --region "$(regiao)" >/dev/null
if ! kubectl_ get namespace "$NAMESPACE" >/dev/null 2>&1; then
  morrer "Namespace ${NAMESPACE} não existe — rode antes: AMBIENTE=${AMBIENTE} SOMENTE_MIGRACAO=sim ./infra/scripts/deploy.sh"
fi

# ─────────────────────────────────────────────────────────────────────────────
titulo "2/3  Executando os scripts no cluster"

b64() { printf '%s' "$1" | base64 | tr -d '\r\n'; }
b64_arquivo() { base64 < "$1" | tr -d '\r\n'; }

# Troca `\password <usuário>` pelo ALTER ROLE com o verificador SCRAM-SHA-256 —
# o mesmo cálculo que o psql faz no cliente (RFC 5802/7677, 4096 iterações) —
# e normaliza os finais de linha, porque os scripts do banco são CRLF.
PREPARAR_PY="$(cat <<'PY'
import base64, hashlib, hmac, os

def verificador(senha):
    sal = os.urandom(16)
    salgada = hashlib.pbkdf2_hmac("sha256", senha.encode(), sal, 4096)
    cliente = hmac.new(salgada, b"Client Key", hashlib.sha256).digest()
    servidor = hmac.new(salgada, b"Server Key", hashlib.sha256).digest()
    b = lambda x: base64.b64encode(x).decode()
    return f"SCRAM-SHA-256$4096:{b(sal)}${b(hashlib.sha256(cliente).digest())}:{b(servidor)}"

senhas = {"lambda_auth": os.environ["LAMBDA_DB_PASSWORD"],
          "autopecas_app": os.environ["APP_DB_PASSWORD"]}
for nome in sorted(os.listdir("/entrada")):
    if not nome.endswith(".sql"):
        continue
    linhas = open(f"/entrada/{nome}", encoding="utf-8").read().replace("\r\n", "\n").split("\n")
    for i, linha in enumerate(linhas):
        partes = linha.strip().split()
        if len(partes) == 2 and partes[0] == "\\password":
            usuario = partes[1]
            if usuario not in senhas:
                raise SystemExit(f"{nome}: senha desconhecida para {usuario}")
            linhas[i] = f"ALTER ROLE {usuario} PASSWORD '{verificador(senhas.pop(usuario))}';"
    open(f"/trabalho/{nome}", "w", encoding="utf-8").write("\n".join(linhas))
if senhas:
    raise SystemExit(f"nenhum \\password encontrado para: {', '.join(senhas)}")
print("verificadores prontos")
PY
)"

# Os dados de demonstração não são idempotentes (chaves fixas): só entram com o
# banco sem clientes, para o script poder rodar de novo.
DEMO_SQL="$(cat <<'SQL'
SELECT NOT EXISTS (SELECT 1 FROM clientes) AS banco_vazio \gset
\if :banco_vazio
  -- O seed termina com setval(); o resultado não interessa ao log.
  \o /dev/null
  \ir seed-demo.sql
  \o
  INSERT INTO funcionario_roles (funcionario_id, role) VALUES
    ('33333333-3333-4333-8333-333333333303', 'ADMIN'),
    ('33333333-3333-4333-8333-333333333303', 'ATENDENTE'),
    ('33333333-3333-4333-8333-333333333301', 'MECANICO'),
    ('33333333-3333-4333-8333-333333333302', 'MECANICO')
  ON CONFLICT DO NOTHING;
  \echo 'dados de demonstracao carregados'
\else
  \echo 'banco ja tem clientes: dados de demonstracao mantidos'
\endif
SQL
)"

ORDEM="01-lambda 02-aplicacao"
if [ "$DADOS_DEMO" = "sim" ]; then
  ORDEM="${ORDEM} 03-demo"
fi
ORDEM="${ORDEM} 04-verificar"

limpar() {
  kubectl_ delete job/preparo-banco secret/preparo-banco -n "$NAMESPACE" --ignore-not-found >/dev/null 2>&1 || true
}
limpar
trap limpar EXIT

kubectl_ apply -f - >/dev/null <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: preparo-banco
  namespace: ${NAMESPACE}
  labels:
    app.kubernetes.io/part-of: autopecas
type: Opaque
data:
  PGPASSWORD: $(b64 "$DB_ADMIN_PASSWORD")
  APP_DB_PASSWORD: $(b64 "$APP_DB_PASSWORD")
  LAMBDA_DB_PASSWORD: $(b64 "$LAMBDA_DB_PASSWORD")
  preparar.py: $(b64 "$PREPARAR_PY")
  01-lambda.sql: $(b64_arquivo "${SCRIPTS_DB}/criar-usuario-lambda.sql")
  02-aplicacao.sql: $(b64_arquivo "${SCRIPTS_DB}/criar-usuario-aplicacao.sql")
  permissoes-aplicacao.sql: $(b64_arquivo "${SCRIPTS_DB}/permissoes-aplicacao.sql")
  03-demo.sql: $(b64 "$DEMO_SQL")
  seed-demo.sql: $(b64_arquivo "$SEED")
  04-verificar.sql: $(b64_arquivo "${SCRIPTS_DB}/verificar-permissoes.sql")
---
apiVersion: batch/v1
kind: Job
metadata:
  name: preparo-banco
  namespace: ${NAMESPACE}
  labels:
    app.kubernetes.io/part-of: autopecas
spec:
  backoffLimit: 0
  activeDeadlineSeconds: 300
  template:
    spec:
      restartPolicy: Never
      securityContext:
        runAsNonRoot: true
        runAsUser: 999
        seccompProfile: { type: RuntimeDefault }
      volumes:
        - name: entrada
          secret:
            secretName: preparo-banco
            items:
              - { key: preparar.py, path: preparar.py }
              - { key: 01-lambda.sql, path: 01-lambda.sql }
              - { key: 02-aplicacao.sql, path: 02-aplicacao.sql }
              - { key: permissoes-aplicacao.sql, path: permissoes-aplicacao.sql }
              - { key: 03-demo.sql, path: 03-demo.sql }
              - { key: seed-demo.sql, path: seed-demo.sql }
              - { key: 04-verificar.sql, path: 04-verificar.sql }
        - name: trabalho
          emptyDir: { medium: Memory }
      initContainers:
        - name: verificadores
          image: python:3.12-alpine
          command: ["python3", "-I", "/entrada/preparar.py"]
          env:
            - { name: APP_DB_PASSWORD, valueFrom: { secretKeyRef: { name: preparo-banco, key: APP_DB_PASSWORD } } }
            - { name: LAMBDA_DB_PASSWORD, valueFrom: { secretKeyRef: { name: preparo-banco, key: LAMBDA_DB_PASSWORD } } }
          volumeMounts:
            - { name: entrada, mountPath: /entrada, readOnly: true }
            - { name: trabalho, mountPath: /trabalho }
          securityContext: { allowPrivilegeEscalation: false, capabilities: { drop: ["ALL"] } }
      containers:
        - name: psql
          image: postgres:16-alpine
          workingDir: /trabalho
          command: ["sh", "-c"]
          args:
            - |
              for f in ${ORDEM}; do
                echo "== \$f"
                psql -X -q -v ON_ERROR_STOP=1 -f "/trabalho/\$f.sql" || exit 1
              done
              echo PREPARO_OK
          env:
            - { name: PGHOST, value: "${DB_HOST}" }
            - { name: PGPORT, value: "${DB_PORTA}" }
            - { name: PGUSER, value: "${DB_ADMIN_USERNAME}" }
            - { name: PGDATABASE, value: autopecas }
            - { name: PGSSLMODE, value: require }
            - { name: PGPASSWORD, valueFrom: { secretKeyRef: { name: preparo-banco, key: PGPASSWORD } } }
          volumeMounts:
            - { name: trabalho, mountPath: /trabalho, readOnly: true }
          securityContext: { allowPrivilegeEscalation: false, capabilities: { drop: ["ALL"] } }
EOF

kubectl_ wait --for=condition=complete job/preparo-banco -n "$NAMESPACE" --timeout=5m >/dev/null 2>&1 || true
LOG="$(kubectl_ logs job/preparo-banco -n "$NAMESPACE" --all-containers=true 2>&1 || true)"
printf '%s\n' "$LOG" | sed 's/^/    /'

# ─────────────────────────────────────────────────────────────────────────────
titulo "3/3  Resultado"

if ! printf '%s' "$LOG" | grep -q PREPARO_OK; then
  morrer "O preparo falhou (log acima). Os scripts são idempotentes: corrija a causa e rode de novo."
fi
ok "Usuários lambda_auth e autopecas_app prontos; verificador de permissões aprovado"
cat <<EOF

  Próximo passo:

      AMBIENTE=${AMBIENTE} PULAR_MIGRACAO=sim ./infra/scripts/deploy.sh

EOF
