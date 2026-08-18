CREATE TABLE IF NOT EXISTS conexao_balanca (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo_conexao TEXT NOT NULL,
    porta_com TEXT,
    baud_rate INTEGER,
    ip_address TEXT,
    ip_port INTEGER,
    dataCriacao TIMESTAMP NOT NULL
)
