CREATE TABLE IF NOT EXISTS empresas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT,
    cpfCnpj TEXT,
    telefone TEXT,
    email TEXT,
    endereco_cep TEXT,
    endereco_cidade TEXT,
    endereco_estado TEXT,
    endereco_bairro TEXT,
    endereco_rua TEXT,
    endereco_numero TEXT,
    logomarca TEXT,
    dataCriacao TIMESTAMP NOT NULL
)
