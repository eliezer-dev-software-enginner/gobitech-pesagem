-- V1-V9 já declaram dataCriacao/expira_em como TIMESTAMP nos arquivos de migration, mas
-- qualquer banco criado ANTES dessa correção já tem essas colunas como REAL de verdade no
-- disco (Flyway não re-executa uma migration só porque o .sql mudou depois de aplicada — só
-- `flyway.repair()` atualiza o checksum, sem recriar nada). Isso quebra
-- atualizar()/buscarById()/listar() com "Illegal Argument occurred setting property:
-- dataCriacao ... Type read: class java.lang.Double" em qualquer instalação que já rodou o
-- app com o schema antigo. Recria as 9 tabelas com o tipo certo, preservando os dados —
-- mesmo padrão já usado no plics-sw (V20/V21) pra esse tipo de correção de tipo de coluna.

ALTER TABLE usuarios RENAME TO usuarios_old;
CREATE TABLE usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    login TEXT NOT NULL UNIQUE,
    senha TEXT NOT NULL,
    nome TEXT NOT NULL,
    ativo BIT NOT NULL DEFAULT 1,
    admin BIT NOT NULL DEFAULT 0,
    telefone TEXT,
    dataCriacao TIMESTAMP NOT NULL
);
INSERT INTO usuarios SELECT * FROM usuarios_old;
DROP TABLE usuarios_old;

-- preferencias pode ter colunas antigas (tema/credenciais_habilitadas/login/senha/licensa) de
-- antes delas serem removidas do schema (ver DECISIONS.md) — select explícito em vez de
-- SELECT * pra não depender de quais colunas extras sobraram num banco já existente.
ALTER TABLE preferencias RENAME TO preferencias_old;
CREATE TABLE preferencias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    primeiro_acesso INTEGER NOT NULL DEFAULT 1,
    dataCriacao TIMESTAMP NOT NULL
);
INSERT INTO preferencias (id, primeiro_acesso, dataCriacao)
SELECT id, primeiro_acesso, dataCriacao FROM preferencias_old;
DROP TABLE preferencias_old;

ALTER TABLE licensas RENAME TO licensas_old;
CREATE TABLE licensas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    valor TEXT NOT NULL UNIQUE,
    expira_em TIMESTAMP,
    dataCriacao TIMESTAMP NOT NULL
);
INSERT INTO licensas SELECT * FROM licensas_old;
DROP TABLE licensas_old;

ALTER TABLE empresas RENAME TO empresas_old;
CREATE TABLE empresas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT,
    cpfCnpj TEXT,
    telefone TEXT,
    email TEXT,
    endereco_cep TEXT,
    endereco_cidade TEXT,
    endereco_estado TEXT,
    endereco_bairro TEXT,
    endereco_rua TEXT,
    endereco_numero TEXT,
    logomarca TEXT,
    dataCriacao TIMESTAMP NOT NULL
);
INSERT INTO empresas SELECT * FROM empresas_old;
DROP TABLE empresas_old;

ALTER TABLE clientes RENAME TO clientes_old;
CREATE TABLE clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    loja TEXT NOT NULL UNIQUE,
    razao_social TEXT NOT NULL,
    cpfCnpj TEXT,
    telefone TEXT,
    cep TEXT,
    uf TEXT,
    cidade TEXT,
    bairro TEXT,
    rua TEXT,
    numero TEXT,
    complemento TEXT,
    ativo BIT NOT NULL DEFAULT 1,
    dataCriacao TIMESTAMP NOT NULL
);
INSERT INTO clientes SELECT * FROM clientes_old;
DROP TABLE clientes_old;

ALTER TABLE produtos RENAME TO produtos_old;
CREATE TABLE produtos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL UNIQUE,
    unidade TEXT,
    observacoes TEXT,
    ativo BIT NOT NULL DEFAULT 1,
    dataCriacao TIMESTAMP NOT NULL
);
INSERT INTO produtos SELECT * FROM produtos_old;
DROP TABLE produtos_old;

ALTER TABLE descontos RENAME TO descontos_old;
CREATE TABLE descontos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    avariados REAL NOT NULL DEFAULT 0,
    ardidos REAL NOT NULL DEFAULT 0,
    quebra_ardidos REAL NOT NULL DEFAULT 0,
    impurezas REAL NOT NULL DEFAULT 0,
    quebra_impurezas REAL NOT NULL DEFAULT 0,
    umidade REAL NOT NULL DEFAULT 0,
    quebra_umidade REAL NOT NULL DEFAULT 0,
    outros REAL NOT NULL DEFAULT 0,
    dataCriacao TIMESTAMP NOT NULL
);
INSERT INTO descontos SELECT * FROM descontos_old;
DROP TABLE descontos_old;

ALTER TABLE conexao_balanca RENAME TO conexao_balanca_old;
CREATE TABLE conexao_balanca (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo_conexao TEXT NOT NULL,
    porta_com TEXT,
    baud_rate INTEGER,
    ip_address TEXT,
    ip_port INTEGER,
    dataCriacao TIMESTAMP NOT NULL
);
INSERT INTO conexao_balanca SELECT * FROM conexao_balanca_old;
DROP TABLE conexao_balanca_old;

-- pesagens por último: referencia clientes/produtos/descontos, que já foram recriados acima.
ALTER TABLE pesagens RENAME TO pesagens_old;
CREATE TABLE pesagens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    motorista_nome TEXT NOT NULL,
    motorista_documento TEXT,
    placa TEXT NOT NULL,
    operacao TEXT NOT NULL,
    nota_fiscal TEXT,
    observacoes TEXT,
    peso_veiculo REAL NOT NULL,
    peso_total REAL NOT NULL,
    peso_final REAL NOT NULL,
    foto_frente_1 TEXT,
    foto_frente_2 TEXT,
    foto_costas_1 TEXT,
    foto_costas_2 TEXT,
    cliente_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    desconto_id INTEGER,
    dataCriacao TIMESTAMP NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (desconto_id) REFERENCES descontos(id)
);
INSERT INTO pesagens SELECT * FROM pesagens_old;
DROP TABLE pesagens_old;
