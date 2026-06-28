-- Script executado pelo Testcontainers ao criar o container PostgreSQL.
-- O Hibernate (create-drop) ou Flyway cuida do schema depois.
-- Aqui ficam apenas configurações que precisam existir ANTES das migrations.

-- Extensão para UUID (usada como tipo de PK nas entidades)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Extensão para busca textual (opcional, mas boa prática)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";