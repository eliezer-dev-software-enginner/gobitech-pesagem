INSERT INTO preferencias (primeiro_acesso, dataCriacao)
SELECT 1, strftime('%s', 'now') * 1000
WHERE NOT EXISTS (SELECT 1 FROM preferencias WHERE id = 1);

INSERT INTO usuarios (login, senha, nome, ativo, admin, dataCriacao)
SELECT 'gestor', '1234', 'Gestor', 1, 1, strftime('%s', 'now') * 1000
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE login = 'gestor');

INSERT INTO empresas (nome, dataCriacao)
SELECT 'Balanças Gobitech', strftime('%s', 'now') * 1000
WHERE NOT EXISTS (SELECT 1 FROM empresas WHERE id = 1);

INSERT INTO conexao_balanca (tipo_conexao, dataCriacao)
SELECT 'Serial', strftime('%s', 'now') * 1000
WHERE NOT EXISTS (SELECT 1 FROM conexao_balanca WHERE id = 1);
