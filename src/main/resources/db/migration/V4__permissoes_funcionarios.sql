-- Permissoes explicitas; o login usa o CPF do cadastro e seu estado ativo.
CREATE TABLE funcionario_roles (
    funcionario_id UUID NOT NULL REFERENCES funcionarios(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','ATENDENTE','MECANICO')),
    PRIMARY KEY (funcionario_id, role)
);
