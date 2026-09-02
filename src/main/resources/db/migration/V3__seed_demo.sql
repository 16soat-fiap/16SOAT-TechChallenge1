-- ─── Clientes ────────────────────────────────────────────────────────────────

INSERT INTO clientes (id, tipo_cliente, nome, email, telefone, aceita_notificacoes, ativo,
                      created_at, update_at)
VALUES
    ('11111111-1111-4111-8111-111111111101', 'PF', 'Maria Silva', 'maria.silva@demo.com',
     '11999990001', true, true, TIMESTAMP '2026-09-01 07:00:00', TIMESTAMP '2026-09-01 07:00:00'),
    ('11111111-1111-4111-8111-111111111102', 'PF', 'João Santos', 'joao.santos@demo.com',
     '11999990002', true, true, TIMESTAMP '2026-09-01 07:05:00', TIMESTAMP '2026-09-01 07:05:00'),
    ('11111111-1111-4111-8111-111111111103', 'PJ', 'Transportes Rápido Ltda',
     'frota@transportesrapido.demo', '1133334444', true, true,
     TIMESTAMP '2026-09-01 07:10:00', TIMESTAMP '2026-09-01 07:10:00');

INSERT INTO clientes_pf (id, cpf, data_nascimento)
VALUES
    ('11111111-1111-4111-8111-111111111101', '52998224725', DATE '1988-04-15'),
    ('11111111-1111-4111-8111-111111111102', '39053344705', DATE '1975-09-20');

INSERT INTO clientes_pj (id, cnpj, razao_social, contato_responsavel)
VALUES
    ('11111111-1111-4111-8111-111111111103', '00000000000191', 'Transportes Rápido Ltda',
     'Roberto Frota');

-- ─── Veículos ────────────────────────────────────────────────────────────────

INSERT INTO veiculos (id, placa, marca, modelo, ano_modelo, cor, ativo, cliente_id,
                      created_at, updated_at)
VALUES
    ('22222222-2222-4222-8222-222222222201', 'ABC1D23', 'Volkswagen', 'Gol', 2019, 'Prata',
     true, '11111111-1111-4111-8111-111111111101',
     TIMESTAMP '2026-09-01 07:15:00', TIMESTAMP '2026-09-01 07:15:00'),
    ('22222222-2222-4222-8222-222222222202', 'DEF4G56', 'Chevrolet', 'Onix', 2022, 'Preto',
     true, '11111111-1111-4111-8111-111111111102',
     TIMESTAMP '2026-09-01 07:20:00', TIMESTAMP '2026-09-01 07:20:00'),
    ('22222222-2222-4222-8222-222222222203', 'GHI7J89', 'Fiat', 'Strada', 2021, 'Branco',
     true, '11111111-1111-4111-8111-111111111103',
     TIMESTAMP '2026-09-01 07:25:00', TIMESTAMP '2026-09-01 07:25:00');

-- ─── Funcionários ────────────────────────────────────────────────────────────

INSERT INTO funcionarios (id, tipo_funcionario, matricula, cpf, nome, email, telefone,
                          data_nascimento, ativo, created_at, updated_at)
VALUES
    ('33333333-3333-4333-8333-333333333301', 'MECANICO', 'MEC-0001', '11144477735',
     'Carlos Mecânico', 'carlos.mec@oficina.demo', '11988880001', DATE '1985-03-12', true,
     TIMESTAMP '2026-09-01 07:30:00', TIMESTAMP '2026-09-01 07:30:00'),
    ('33333333-3333-4333-8333-333333333302', 'MECANICO', 'MEC-0002', '28625587887',
     'Ana Mecânica', 'ana.mec@oficina.demo', '11988880002', DATE '1990-07-22', true,
     TIMESTAMP '2026-09-01 07:31:00', TIMESTAMP '2026-09-01 07:31:00'),
    ('33333333-3333-4333-8333-333333333303', 'ATENDENTE', 'ATD-0001', '15350946056',
     'Paula Atendente', 'paula.atd@oficina.demo', '11988880003', DATE '1992-11-05', true,
     TIMESTAMP '2026-09-01 07:32:00', TIMESTAMP '2026-09-01 07:32:00');

INSERT INTO mecanicos (id) VALUES
    ('33333333-3333-4333-8333-333333333301'),
    ('33333333-3333-4333-8333-333333333302');

INSERT INTO atendentes (id) VALUES
    ('33333333-3333-4333-8333-333333333303');

-- ─── Catálogo ────────────────────────────────────────────────────────────────

INSERT INTO servicos (id, nome, descricao, preco_base, tempo_estimado_minutos, ativo,
                      created_at, updated_at)
VALUES
    ('44444444-4444-4444-8444-444444444401', 'Troca de Óleo',
     'Troca de óleo do motor + filtro', 180.00, 45, true,
     TIMESTAMP '2026-09-01 07:40:00', TIMESTAMP '2026-09-01 07:40:00'),
    ('44444444-4444-4444-8444-444444444402', 'Alinhamento e Balanceamento',
     'Alinhamento computadorizado + balanceamento das 4 rodas', 220.00, 90, true,
     TIMESTAMP '2026-09-01 07:41:00', TIMESTAMP '2026-09-01 07:41:00'),
    ('44444444-4444-4444-8444-444444444403', 'Revisão de Freios',
     'Inspeção e troca de pastilhas/discos', 350.00, 120, true,
     TIMESTAMP '2026-09-01 07:42:00', TIMESTAMP '2026-09-01 07:42:00');

INSERT INTO pecas (id, codigo, nome, descricao, preco_venda, quantidade_estoque,
                   quantidade_minima, unidade, ativo, created_at, updated_at)
VALUES
    ('55555555-5555-4555-8555-555555555501', 'DEMO-OLEO-5W30', 'Óleo Motor 5W30 Sintético',
     '4 litros', 89.90, 50, 5, 'un', true,
     TIMESTAMP '2026-09-01 07:45:00', TIMESTAMP '2026-09-01 07:45:00'),
    ('55555555-5555-4555-8555-555555555502', 'DEMO-FILTRO-OLEO', 'Filtro de Óleo',
     'Filtro padrão', 45.00, 40, 5, 'un', true,
     TIMESTAMP '2026-09-01 07:46:00', TIMESTAMP '2026-09-01 07:46:00'),
    ('55555555-5555-4555-8555-555555555503', 'DEMO-PASTILHA-FREIO', 'Pastilha de Freio Dianteira',
     'Par dianteiro', 189.00, 30, 4, 'par', true,
     TIMESTAMP '2026-09-01 07:47:00', TIMESTAMP '2026-09-01 07:47:00'),
    ('55555555-5555-4555-8555-555555555504', 'DEMO-CORREIA-DENTADA', 'Correia Dentada',
     'Correia original', 320.00, 15, 2, 'un', true,
     TIMESTAMP '2026-09-01 07:48:00', TIMESTAMP '2026-09-01 07:48:00');

-- ─── Ordens de serviço ───────────────────────────────────────────────────────

INSERT INTO ordens_servico (id, numero, version, status, quilometragem_entrada,
                            observacoes_entrada, diagnostico, queixa_cliente,
                            valor_total_aprovado, data_inicio_execucao, data_finalizacao,
                            data_entrega, created_at, updated_at, cliente_id, veiculo_id,
                            atendente_recepcao_id, mecanico_responsavel_id)
VALUES
    -- OS-000001: EM_EXECUCAO (mais antiga da faixa)
    ('66666666-6666-4666-8666-666666666601', 'OS-000001', 0, 'EM_EXECUCAO', 45000,
     'Veículo recebido para demonstração', NULL, 'Barulho metálico ao frear',
     0.00, TIMESTAMP '2026-09-01 09:00:00', NULL, NULL,
     TIMESTAMP '2026-09-01 08:00:00', TIMESTAMP '2026-09-01 09:00:00',
     '11111111-1111-4111-8111-111111111101', '22222222-2222-4222-8222-222222222201',
     '33333333-3333-4333-8333-333333333303', '33333333-3333-4333-8333-333333333301'),

    -- OS-000002: EM_EXECUCAO
    ('66666666-6666-4666-8666-666666666602', 'OS-000002', 0, 'EM_EXECUCAO', 52000,
     'Veículo recebido para demonstração', NULL, 'Vibração no volante em alta velocidade',
     0.00, TIMESTAMP '2026-09-01 10:00:00', NULL, NULL,
     TIMESTAMP '2026-09-01 09:00:00', TIMESTAMP '2026-09-01 10:00:00',
     '11111111-1111-4111-8111-111111111102', '22222222-2222-4222-8222-222222222202',
     '33333333-3333-4333-8333-333333333303', '33333333-3333-4333-8333-333333333302'),

    -- OS-000003: AGUARDANDO_APROVACAO (orçamento enviado — aprovar no vídeo)
    ('66666666-6666-4666-8666-666666666603', 'OS-000003', 0, 'AGUARDANDO_APROVACAO', 78000,
     'Veículo recebido para demonstração',
     'Correia dentada ressecada e tensor com folga. Recomendada troca imediata.',
     'Correia dentada com desgaste',
     0.00, NULL, NULL, NULL,
     TIMESTAMP '2026-09-01 10:00:00', TIMESTAMP '2026-09-01 11:00:00',
     '11111111-1111-4111-8111-111111111103', '22222222-2222-4222-8222-222222222203',
     '33333333-3333-4333-8333-333333333303', NULL),

    -- OS-000004: EM_DIAGNOSTICO
    ('66666666-6666-4666-8666-666666666604', 'OS-000004', 0, 'EM_DIAGNOSTICO', 61000,
     'Veículo recebido para demonstração',
     'Leitura OBD: sensor de oxigênio com leitura irregular.',
     'Luz da injeção acesa no painel',
     0.00, NULL, NULL, NULL,
     TIMESTAMP '2026-09-01 11:00:00', TIMESTAMP '2026-09-01 11:30:00',
     '11111111-1111-4111-8111-111111111101', '22222222-2222-4222-8222-222222222201',
     '33333333-3333-4333-8333-333333333303', NULL),

    -- OS-000005: RECEBIDA (abrir nova OS ao vivo ou consultar esta)
    ('66666666-6666-4666-8666-666666666605', 'OS-000005', 0, 'RECEBIDA', 38000,
     'Veículo recebido para demonstração', NULL, 'Ar-condicionado não gela',
     0.00, NULL, NULL, NULL,
     TIMESTAMP '2026-09-01 12:00:00', TIMESTAMP '2026-09-01 12:00:00',
     '11111111-1111-4111-8111-111111111102', '22222222-2222-4222-8222-222222222202',
     '33333333-3333-4333-8333-333333333303', NULL),

    -- OS-000006: FINALIZADA (oculta na listagem padrão)
    ('66666666-6666-4666-8666-666666666606', 'OS-000006', 0, 'FINALIZADA', 40000,
     'Veículo recebido para demonstração', NULL, 'Troca de óleo programada',
     269.90, TIMESTAMP '2026-08-30 09:00:00', TIMESTAMP '2026-08-30 11:00:00', NULL,
     TIMESTAMP '2026-08-30 08:00:00', TIMESTAMP '2026-08-30 11:00:00',
     '11111111-1111-4111-8111-111111111101', '22222222-2222-4222-8222-222222222201',
     '33333333-3333-4333-8333-333333333303', '33333333-3333-4333-8333-333333333301'),

    -- OS-000007: ENTREGUE (oculta na listagem padrão)
    ('66666666-6666-4666-8666-666666666607', 'OS-000007', 0, 'ENTREGUE', 40500,
     'Veículo recebido para demonstração', NULL, 'Revisão dos 40.000 km',
     400.00, TIMESTAMP '2026-08-28 09:00:00', TIMESTAMP '2026-08-28 14:00:00',
     TIMESTAMP '2026-08-28 16:00:00',
     TIMESTAMP '2026-08-28 08:00:00', TIMESTAMP '2026-08-28 16:00:00',
     '11111111-1111-4111-8111-111111111102', '22222222-2222-4222-8222-222222222202',
     '33333333-3333-4333-8333-333333333303', '33333333-3333-4333-8333-333333333302');

-- ─── Orçamento da OS-000003 (ENVIADA — aprovar ao vivo) ─────────────────────

INSERT INTO orcamentos (id, versao, status, version_lock, valor_mao_obra, valor_pecas,
                        valor_acrescimo, valor_total, condicoes_pagamento, prazo_execucao_dias,
                        data_validade, observacoes, data_envio, created_at, updated_at,
                        ordem_servico_id, elaborado_por_id)
VALUES
    ('77777777-7777-4777-8777-777777777701', 1, 'ENVIADA', 0,
     350.00, 320.00, 0.00, 670.00,
     'PIX ou cartão em até 3x', 2, DATE '2026-09-08',
     'Orçamento aguardando aprovação do cliente — usar no vídeo',
     TIMESTAMP '2026-09-01 11:30:00',
     TIMESTAMP '2026-09-01 11:00:00', TIMESTAMP '2026-09-01 11:30:00',
     '66666666-6666-4666-8666-666666666603',
     '33333333-3333-4333-8333-333333333303');

INSERT INTO itens_orcamento_servico (orcamento_id, servico_id, quantidade, preco_unitario)
VALUES
    ('77777777-7777-4777-8777-777777777701', '44444444-4444-4444-8444-444444444403', 1, 350.00);

INSERT INTO itens_orcamento_peca (orcamento_id, peca_id, quantidade, preco_unitario)
VALUES
    ('77777777-7777-4777-8777-777777777701', '55555555-5555-4555-8555-555555555504', 1, 320.00);

-- ─── Sequences (próximo número gerado pela API) ─────────────────────────────

SELECT setval('os_numero_seq', 7, true);
SELECT setval('mecanico_seq', 2, true);
SELECT setval('atendente_seq', 1, true);
