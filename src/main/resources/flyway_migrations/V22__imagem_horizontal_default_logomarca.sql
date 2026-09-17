-- Define a logomarca horizontal padrão em preferencias.imagem_horizontal:
-- 'assets/balancas-gobitech-logo-1500x500.png' (recurso embutido no classpath).
-- SQLite não troca o DEFAULT de uma coluna existente (sem ALTER COLUMN), então recria a
-- tabela preservando os dados — mesmo padrão do V18 — incluindo tipo_impressao (V20) e
-- imagem_horizontal (V21). Linhas com imagem vazia/nula (nunca escolhida) passam a exibir
-- a logomarca padrão; "Remover logomarca" continua persistindo '' explícito = sem imagem.

ALTER TABLE preferencias RENAME TO preferencias_old;

CREATE TABLE preferencias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    primeiro_acesso INTEGER NOT NULL DEFAULT 1,
    dataCriacao TIMESTAMP NOT NULL,
    tipo_impressao TEXT NOT NULL DEFAULT 'laser'
        CHECK (tipo_impressao IN ('laser', 'termica')),
    imagem_horizontal TEXT NOT NULL DEFAULT 'assets/balancas-gobitech-logo-1500x500.png'
);

INSERT INTO preferencias (id, primeiro_acesso, dataCriacao, tipo_impressao, imagem_horizontal)
SELECT
    id,
    primeiro_acesso,
    dataCriacao,
    tipo_impressao,
    CASE WHEN imagem_horizontal IS NULL OR imagem_horizontal = ''
         THEN 'assets/balancas-gobitech-logo-1500x500.png'
         ELSE imagem_horizontal END
FROM preferencias_old;

DROP TABLE preferencias_old;