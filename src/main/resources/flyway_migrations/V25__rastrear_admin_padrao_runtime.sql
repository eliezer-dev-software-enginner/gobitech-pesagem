CREATE TABLE configuracoes_runtime (
    chave TEXT PRIMARY KEY,
    usuario_id INTEGER,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- V10 precisa permanecer intacta por causa do checksum do Flyway. Esta migration
-- identifica somente o seed legado ainda não personalizado; o Java troca login e
-- senha pelas propriedades injetadas no launcher antes de abrir a interface.
INSERT INTO configuracoes_runtime (chave, usuario_id)
SELECT 'admin_padrao', id
FROM usuarios
WHERE login = 'qs0g1NZE1uw9f6blYfgsLVfw+mHQEXUZWdyYp4OxxW4='
  AND admin = 1
LIMIT 1;
