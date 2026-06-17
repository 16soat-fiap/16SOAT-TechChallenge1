-- V1__schema_inicial.sql
-- Schema inicial do sistema de oficina mecânica

-- ─── Clientes ────────────────────────────────────────────────────────────────

CREATE TABLE clientes (
    id UUID NOT NULL,
    tipo_cliente VARCHAR(2) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    telefone VARCHAR(20),
    aceita_notificacoes BOOLEAN NOT NULL DEFAULT true,
    ativo BOOLEAN NOT NULL DEFAULT true,
    endereco_cep VARCHAR(9),
    endereco_logradouro VARCHAR(255),
    endereco_numero VARCHAR(20),
    endereco_complemento VARCHAR(100),
    endereco_bairro VARCHAR(100),
    endereco_cidade VARCHAR(100),
    endereco_uf VARCHAR(2),
    created_at TIMESTAMP NOT NULL,
    update_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_clientes PRIMARY KEY (id),
    CONSTRAINT uk_cliente_email UNIQUE (email)
);

CREATE TABLE clientes_pf (
    id UUID NOT NULL,
    cpf VARCHAR(14) NOT NULL,
    data_nascimento DATE,
    rg VARCHAR(20),
    genero VARCHAR(20),
    profissao VARCHAR(100),
    CONSTRAINT pk_clientes_pf PRIMARY KEY (id),
    CONSTRAINT fk_clientes_pf FOREIGN KEY (id) REFERENCES clientes(id),
    CONSTRAINT uk_cliente_pf_cpf UNIQUE (cpf)
);

CREATE TABLE clientes_pj (
    id UUID NOT NULL,
    cnpj VARCHAR(18) NOT NULL,
    razao_social VARCHAR(150) NOT NULL,
    telefone VARCHAR(20),
    inscricao_estadual VARCHAR(30),
    inscricao_municipal VARCHAR(30),
    contato_responsavel VARCHAR(100),
    CONSTRAINT pk_clientes_pj PRIMARY KEY (id),
    CONSTRAINT fk_clientes_pj FOREIGN KEY (id) REFERENCES clientes(id),
    CONSTRAINT uk_cliente_pj_cnpj UNIQUE (cnpj)
);

-- ─── Veículos ────────────────────────────────────────────────────────────────

CREATE TABLE veiculos (
    id UUID NOT NULL,
    placa VARCHAR(7) NOT NULL,
    chassi VARCHAR(17),
    renavam VARCHAR(11),
    marca VARCHAR(60) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    ano_modelo INTEGER,
    cor VARCHAR(50),
    observacoes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    cliente_id UUID NOT NULL,
    CONSTRAINT pk_veiculos PRIMARY KEY (id),
    CONSTRAINT fk_veiculo_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT uk_veiculo_placa UNIQUE (placa),
    CONSTRAINT uk_veiculo_chassi UNIQUE (chassi),
    CONSTRAINT uk_veiculo_renavam UNIQUE (renavam)
);

-- ─── Funcionários ────────────────────────────────────────────────────────────

CREATE TABLE funcionarios (
    id UUID NOT NULL,
    tipo_funcionario VARCHAR(20) NOT NULL,
    matricula VARCHAR(20) NOT NULL,
    cpf VARCHAR(14) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    telefone VARCHAR(20),
    data_nascimento DATE,
    ativo BOOLEAN NOT NULL DEFAULT true,
    endereco_cep VARCHAR(9),
    endereco_logradouro VARCHAR(200),
    endereco_numero VARCHAR(20),
    endereco_complemento VARCHAR(100),
    endereco_bairro VARCHAR(100),
    endereco_cidade VARCHAR(100),
    endereco_uf VARCHAR(2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_funcionarios PRIMARY KEY (id),
    CONSTRAINT uk_funcionario_matricula UNIQUE (matricula),
    CONSTRAINT uk_funcionario_cpf UNIQUE (cpf)
);

CREATE TABLE mecanicos (
    id UUID NOT NULL,
    CONSTRAINT pk_mecanicos PRIMARY KEY (id),
    CONSTRAINT fk_mecanicos FOREIGN KEY (id) REFERENCES funcionarios(id)
);

CREATE TABLE atendentes (
    id UUID NOT NULL,
    CONSTRAINT pk_atendentes PRIMARY KEY (id),
    CONSTRAINT fk_atendentes FOREIGN KEY (id) REFERENCES funcionarios(id)
);

-- ─── Peças ───────────────────────────────────────────────────────────────────

CREATE TABLE pecas (
    id UUID NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    marca VARCHAR(100),
    preco_venda NUMERIC(10, 2) NOT NULL,
    quantidade_estoque INTEGER NOT NULL DEFAULT 0,
    quantidade_minima INTEGER NOT NULL DEFAULT 1,
    unidade VARCHAR(10) NOT NULL DEFAULT 'un',
    ativo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_pecas PRIMARY KEY (id),
    CONSTRAINT uk_peca_codigo UNIQUE (codigo)
);

-- ─── Serviços ────────────────────────────────────────────────────────────────

CREATE TABLE servicos (
    id UUID NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    preco_base NUMERIC(10, 2) NOT NULL,
    tempo_estimado_minutos INTEGER,
    ativo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_servicos PRIMARY KEY (id)
);

-- ─── Ordens de Serviço ───────────────────────────────────────────────────────

CREATE TABLE ordens_servico (
    id UUID NOT NULL,
    numero VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    quilometragem_entrada INTEGER,
    observacoes_entrada VARCHAR(1000),
    diagnostico VARCHAR(2000),
    queixa_cliente VARCHAR(1000),
    valor_total_aprovado NUMERIC(10, 2) NOT NULL DEFAULT 0,
    data_inicio_execucao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    data_entrega TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    cliente_id UUID NOT NULL,
    veiculo_id UUID NOT NULL,
    atendente_recepcao_id UUID,
    atendente_entrega_id UUID,
    mecanico_responsavel_id UUID,
    CONSTRAINT pk_ordens_servico PRIMARY KEY (id),
    CONSTRAINT fk_os_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_os_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id),
    CONSTRAINT fk_os_atendente_recepcao FOREIGN KEY (atendente_recepcao_id) REFERENCES funcionarios(id),
    CONSTRAINT fk_os_atendente_entrega FOREIGN KEY (atendente_entrega_id) REFERENCES funcionarios(id),
    CONSTRAINT fk_os_mecanico FOREIGN KEY (mecanico_responsavel_id) REFERENCES funcionarios(id),
    CONSTRAINT uk_os_numero UNIQUE (numero)
);

CREATE INDEX idx_os_cliente  ON ordens_servico(cliente_id);
CREATE INDEX idx_os_veiculo  ON ordens_servico(veiculo_id);
CREATE INDEX idx_os_status   ON ordens_servico(status);
CREATE INDEX idx_os_mecanico ON ordens_servico(mecanico_responsavel_id);

-- ─── Orçamentos ──────────────────────────────────────────────────────────────

CREATE TABLE orcamentos (
    id UUID NOT NULL,
    versao INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',
    version_lock BIGINT NOT NULL DEFAULT 0,
    valor_mao_obra NUMERIC(10, 2) NOT NULL DEFAULT 0,
    valor_pecas NUMERIC(10, 2) NOT NULL DEFAULT 0,
    valor_acrescimo NUMERIC(10, 2) NOT NULL DEFAULT 0,
    valor_total NUMERIC(10, 2) NOT NULL DEFAULT 0,
    condicoes_pagamento VARCHAR(500),
    prazo_execucao_dias INTEGER,
    data_validade DATE,
    observacoes VARCHAR(2000),
    data_envio TIMESTAMP,
    data_resposta_cliente TIMESTAMP,
    motivo_rejeicao VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    ordem_servico_id UUID NOT NULL,
    elaborado_por_id UUID,
    CONSTRAINT pk_orcamentos PRIMARY KEY (id),
    CONSTRAINT fk_orcamento_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id),
    CONSTRAINT fk_orcamento_elaborador FOREIGN KEY (elaborado_por_id) REFERENCES funcionarios(id),
    CONSTRAINT uk_orcamento_os_versao UNIQUE (ordem_servico_id, versao)
);

CREATE INDEX idx_orcamento_os     ON orcamentos(ordem_servico_id);
CREATE INDEX idx_orcamento_status ON orcamentos(status);

-- ─── Itens de Orçamento ──────────────────────────────────────────────────────

CREATE TABLE itens_orcamento_servico (
    id BIGSERIAL NOT NULL,
    orcamento_id UUID NOT NULL,
    servico_id UUID NOT NULL,
    quantidade INTEGER NOT NULL DEFAULT 1,
    preco_unitario NUMERIC(10, 2) NOT NULL,
    observacao VARCHAR(300),
    CONSTRAINT pk_itens_orcamento_servico PRIMARY KEY (id),
    CONSTRAINT fk_item_orc_serv_orcamento FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id),
    CONSTRAINT fk_item_orc_serv_servico FOREIGN KEY (servico_id) REFERENCES servicos(id)
);

CREATE TABLE itens_orcamento_peca (
    id BIGSERIAL NOT NULL,
    orcamento_id UUID NOT NULL,
    peca_id UUID NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(10, 2) NOT NULL,
    CONSTRAINT pk_itens_orcamento_peca PRIMARY KEY (id),
    CONSTRAINT fk_item_orc_peca_orcamento FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id),
    CONSTRAINT fk_item_orc_peca_catalogo FOREIGN KEY (peca_id) REFERENCES pecas(id)
);

-- ─── Itens de Execução da OS ─────────────────────────────────────────────────

CREATE TABLE itens_servico_os (
    id BIGSERIAL NOT NULL,
    ordem_servico_id UUID NOT NULL,
    servico_id UUID NOT NULL,
    quantidade INTEGER NOT NULL DEFAULT 1,
    preco_unitario NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    executado_por_id UUID,
    data_inicio_execucao TIMESTAMP,
    data_fim_execucao TIMESTAMP,
    observacao VARCHAR(500),
    CONSTRAINT pk_itens_servico_os PRIMARY KEY (id),
    CONSTRAINT fk_item_servico_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id),
    CONSTRAINT fk_item_servico_catalogo FOREIGN KEY (servico_id) REFERENCES servicos(id),
    CONSTRAINT fk_item_servico_mecanico FOREIGN KEY (executado_por_id) REFERENCES funcionarios(id)
);

CREATE TABLE itens_peca_os (
    id BIGSERIAL NOT NULL,
    ordem_servico_id UUID NOT NULL,
    peca_id UUID NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    instalado_por_id UUID,
    data_instalacao TIMESTAMP,
    CONSTRAINT pk_itens_peca_os PRIMARY KEY (id),
    CONSTRAINT fk_item_peca_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id),
    CONSTRAINT fk_item_peca_catalogo FOREIGN KEY (peca_id) REFERENCES pecas(id),
    CONSTRAINT fk_item_peca_mecanico FOREIGN KEY (instalado_por_id) REFERENCES funcionarios(id)
);

-- ─── Movimentações de Estoque ────────────────────────────────────────────────

CREATE TABLE movimentacoes_estoque (
    id UUID NOT NULL,
    peca_id UUID NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    quantidade INTEGER NOT NULL,
    saldo_apos INTEGER NOT NULL,
    valor_unitario_momento NUMERIC(10, 2),
    motivo VARCHAR(300),
    ordem_servico_id UUID,
    executado_por_id UUID,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_movimentacoes_estoque PRIMARY KEY (id),
    CONSTRAINT fk_movimentacao_peca FOREIGN KEY (peca_id) REFERENCES pecas(id),
    CONSTRAINT fk_movimentacao_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id),
    CONSTRAINT fk_movimentacao_funcionario FOREIGN KEY (executado_por_id) REFERENCES funcionarios(id)
);

CREATE INDEX idx_movimentacao_peca ON movimentacoes_estoque(peca_id);
CREATE INDEX idx_movimentacao_data ON movimentacoes_estoque(created_at);
CREATE INDEX idx_movimentacao_os   ON movimentacoes_estoque(ordem_servico_id);
CREATE INDEX idx_movimentacao_tipo ON movimentacoes_estoque(tipo);

-- ─── Histórico de Status da OS ───────────────────────────────────────────────

CREATE TABLE historico_status_os (
    id BIGSERIAL NOT NULL,
    ordem_servico_id UUID NOT NULL,
    status_anterior VARCHAR(30),
    status_novo VARCHAR(30) NOT NULL,
    observacao VARCHAR(500),
    alterado_por VARCHAR(255) NOT NULL,
    executado_por_id UUID,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_historico_status_os PRIMARY KEY (id),
    CONSTRAINT fk_historico_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id),
    CONSTRAINT fk_historico_funcionario FOREIGN KEY (executado_por_id) REFERENCES funcionarios(id)
);

CREATE INDEX idx_historico_os       ON historico_status_os(ordem_servico_id);
CREATE INDEX idx_historico_executor ON historico_status_os(executado_por_id);
