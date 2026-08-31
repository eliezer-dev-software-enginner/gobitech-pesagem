-- O ticket de pesagem (padrão do André) mostra o "Operador" (quem registrou a pesagem).
-- A pesagem não guardava quem a criou, então adiciona usuario_id, preenchido na criação
-- com o usuário logado (SessaoUsuario). Coluna opcional: pesagens antigas ficam com NULL.

ALTER TABLE pesagens ADD COLUMN usuario_id INTEGER;

-- índice pra leitura no ticket (operador da pesagem)
CREATE INDEX IF NOT EXISTS idx_pesagens_usuario_id ON pesagens(usuario_id);
