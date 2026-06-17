-- V2__add_sequences.sql
-- Sequências necessárias para geração de identificadores únicos

-- Sequência para numeração das Ordens de Serviço (OS-000001, OS-000002, ...)
CREATE SEQUENCE IF NOT EXISTS os_numero_seq START 1;

-- Sequências para matrícula de funcionários (evita colisão em ambientes concorrentes)
CREATE SEQUENCE IF NOT EXISTS mecanico_seq START 1;
CREATE SEQUENCE IF NOT EXISTS atendente_seq START 1;
