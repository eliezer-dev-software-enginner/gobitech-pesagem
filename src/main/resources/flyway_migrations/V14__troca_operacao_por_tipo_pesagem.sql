-- As 4 formas de pesagem agora são tipos marcados no banco (entrada/saida/avulsa/manual),
-- decididos pela tela que criou a pesagem — não mais inferidos pelo histórico de placa
-- (a antiga lógica de `operacao` alternava Entrada/Saída por paridade da placa).
ALTER TABLE pesagens ADD COLUMN tipo_pesagem TEXT;

UPDATE pesagens SET tipo_pesagem = 'entrada' WHERE operacao = 'Entrada';
UPDATE pesagens SET tipo_pesagem = 'saida'  WHERE operacao = 'Saída';
-- se sobrou alguma linha sem tipo (operação desconhecida/outra), assume entrada por padrão
UPDATE pesagens SET tipo_pesagem = 'entrada' WHERE tipo_pesagem IS NULL;

ALTER TABLE pesagens DROP COLUMN operacao;
