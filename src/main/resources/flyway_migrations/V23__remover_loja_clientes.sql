-- "Razão social" passa a ser o único nome do cliente. A tabela é reconstruída porque a coluna
-- loja possuía uma restrição UNIQUE, o que impede DROP COLUMN direto no SQLite.
CREATE TABLE clientes_nova (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    razao_social TEXT NOT NULL,
    cpfCnpj TEXT,
    telefone TEXT,
    cep TEXT,
    uf TEXT,
    cidade TEXT,
    bairro TEXT,
    rua TEXT,
    numero TEXT,
    complemento TEXT,
    ativo BIT NOT NULL DEFAULT 1,
    dataCriacao TIMESTAMP NOT NULL
);

INSERT INTO clientes_nova (
    id, razao_social, cpfCnpj, telefone, cep, uf, cidade, bairro, rua, numero, complemento, ativo,
    dataCriacao
)
SELECT
    id, razao_social, cpfCnpj, telefone, cep, uf, cidade, bairro, rua, numero, complemento, ativo,
    dataCriacao
FROM clientes;

DROP TABLE clientes;
ALTER TABLE clientes_nova RENAME TO clientes;
