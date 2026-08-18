CREATE TABLE IF NOT EXISTS clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    loja TEXT NOT NULL UNIQUE,
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
)
