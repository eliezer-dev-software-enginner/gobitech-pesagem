-- O desconto pertence à pesagem, não ao cadastro do produto. A reconstrução remove a coluna
-- antiga e também normaliza a unidade exibida de "Quilos" para "Kg" nos registros existentes.
CREATE TABLE produtos_nova (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL UNIQUE,
    unidade TEXT,
    observacoes TEXT,
    ativo BIT NOT NULL DEFAULT 1,
    dataCriacao TIMESTAMP NOT NULL
);

INSERT INTO produtos_nova (id, nome, unidade, observacoes, ativo, dataCriacao)
SELECT id, nome, CASE WHEN unidade = 'Quilos' THEN 'Kg' ELSE unidade END,
       observacoes, ativo, dataCriacao
FROM produtos;

DROP TABLE produtos;
ALTER TABLE produtos_nova RENAME TO produtos;
