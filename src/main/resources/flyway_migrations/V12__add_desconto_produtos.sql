-- Campo previsto no DER original do projeto (Anexo II da documentação da DGB Tecnologia:
-- products.discount float(10,2)) mas nunca implementado, nem no app antigo nem nesta reescrita.
-- Desconto padrão do produto — carregado no campo "Outros" da Pesagem ao selecionar o produto
-- (ver PesagemViewModel), editável por pesagem como qualquer um dos outros 7 campos.
ALTER TABLE produtos ADD COLUMN desconto REAL NOT NULL DEFAULT 0;
