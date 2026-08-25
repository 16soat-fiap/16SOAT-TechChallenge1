#!/bin/bash
# Executado uma única vez, na primeira inicialização do volume do Postgres.
# Cria o banco do Keycloak separado do banco da aplicação: o Flyway valida o
# schema de app_db e o Keycloak gerencia o seu, então dividir a mesma base faria
# `ddl-auto: validate` tropeçar nas tabelas do Keycloak.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE keycloak_db'
     WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak_db')\gexec
EOSQL

echo "Banco keycloak_db pronto."
