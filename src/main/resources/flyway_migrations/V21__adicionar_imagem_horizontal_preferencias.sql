-- SQLite não permite ADD COLUMN NOT NULL sem DEFAULT quando a tabela já tem linhas
-- (a preferencias tem 1 linha do seed V10) — sem o default, a migration quebra no boot.
ALTER TABLE preferencias ADD COLUMN imagem_horizontal TEXT NOT NULL DEFAULT '';
