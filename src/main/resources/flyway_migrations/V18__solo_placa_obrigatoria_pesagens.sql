-- Só a placa passou a ser obrigatória nas pesagens (decisão 2026-08-31): o André pediu que
-- motorista e cliente deixassem de ser exigidos. SQLite não permite remover NOT NULL via
-- ALTER COLUMN, então recria a tabela (mesmo padrão do V16), preservando os dados e as FKs,
-- incluindo colunas adicionadas depois (entrada_id e usuario_id).

ALTER TABLE pesagens RENAME TO pesagens_old;

CREATE TABLE pesagens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    motorista_nome TEXT,
    motorista_documento TEXT,
    placa TEXT NOT NULL,
    nota_fiscal TEXT,
    observacoes TEXT,
    peso_veiculo REAL NOT NULL,
    peso_total REAL NOT NULL,
    peso_final REAL NOT NULL,
    foto_frente_1 TEXT,
    foto_frente_2 TEXT,
    foto_costas_1 TEXT,
    foto_costas_2 TEXT,
    cliente_id INTEGER,
    produto_id INTEGER,
    desconto_id INTEGER,
    dataCriacao TIMESTAMP NOT NULL,
    tipo_pesagem TEXT,
    entrada_id INTEGER,
    usuario_id INTEGER,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (desconto_id) REFERENCES descontos(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

INSERT INTO pesagens (
    id, motorista_nome, motorista_documento, placa, nota_fiscal, observacoes,
    peso_veiculo, peso_total, peso_final,
    foto_frente_1, foto_frente_2, foto_costas_1, foto_costas_2,
    cliente_id, produto_id, desconto_id, dataCriacao, tipo_pesagem, entrada_id, usuario_id
)
SELECT
    id, motorista_nome, motorista_documento, placa, nota_fiscal, observacoes,
    peso_veiculo, peso_total, peso_final,
    foto_frente_1, foto_frente_2, foto_costas_1, foto_costas_2,
    cliente_id, produto_id, desconto_id, dataCriacao, tipo_pesagem, entrada_id, usuario_id
FROM pesagens_old;

DROP TABLE pesagens_old;

CREATE INDEX IF NOT EXISTS idx_pesagens_usuario_id ON pesagens(usuario_id);
