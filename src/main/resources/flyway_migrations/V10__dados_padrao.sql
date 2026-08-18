INSERT INTO preferencias (primeiro_acesso, dataCriacao)
SELECT 1, strftime('%s', 'now') * 1000
WHERE NOT EXISTS (SELECT 1 FROM preferencias WHERE id = 1);

-- login/senha já vêm criptografados (AES/ECB, mesma chave de my_app.security.CryptoManager) —
-- login/senha em texto puro nunca ficam gravados no banco, nem no admin padrão.
INSERT INTO usuarios (login, senha, nome, ativo, admin, dataCriacao)
SELECT 'qs0g1NZE1uw9f6blYfgsLVfw+mHQEXUZWdyYp4OxxW4=', 'F9/1j/YRj56RRZaCZbFsOw==', 'André', 1, 1, strftime('%s', 'now') * 1000
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE login = 'qs0g1NZE1uw9f6blYfgsLVfw+mHQEXUZWdyYp4OxxW4=');

INSERT INTO empresas (nome, dataCriacao)
SELECT 'Balanças Gobitech', strftime('%s', 'now') * 1000
WHERE NOT EXISTS (SELECT 1 FROM empresas WHERE id = 1);

-- Sem seed de conexao_balanca: uma linha com tipo_conexao='Serial' mas porta_com/baud_rate
-- nulos passava direto pelo LeitorBalancaFactory e estourava NullPointerException ao abrir
-- a tela de Pesagem (ver DECISIONS.md). ConexaoBalancaService.salvarOuAtualizar() já cria a
-- linha na primeira vez que o usuário salva configuração válida pela tela — não precisa de
-- linha pré-existente.
