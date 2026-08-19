CREATE TABLE IF NOT EXISTS conexao_camera (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    frente_ip TEXT,
    frente_porta INTEGER,
    frente_canal INTEGER,
    frente_usuario TEXT,
    frente_senha TEXT,
    costas_ip TEXT,
    costas_porta INTEGER,
    costas_canal INTEGER,
    costas_usuario TEXT,
    costas_senha TEXT,
    dataCriacao TIMESTAMP NOT NULL
)
