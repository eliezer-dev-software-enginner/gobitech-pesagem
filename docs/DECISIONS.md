# Decisões Arquiteturais

## 2026-08-18: `dev.py` reiniciava sozinho com qualquer toque de metadado (ex.: indexação do IntelliJ)

**Contexto:** usuário relatou "só de fazer ctrl+f no IntelliJ já dispara [o hot reload]". `dev.py`
já tinha lógica pra ignorar eventos de sistema de arquivos sem mudança real de conteúdo
(`ChangeHandler` comparando hash SHA256), mas `known_hashes` começava **vazio** — nunca era
pré-populado com o estado atual dos arquivos antes do `observer.start()`. Resultado: o PRIMEIRO
evento de sistema de arquivos em QUALQUER arquivo depois do `dev.py` subir (um touch de metadado
sem conteúdo mudado — indexação do IntelliJ, Local History, o que for) não tinha nada pra comparar
(`known_hashes.get(path)` retornava `None`), então parecia "mudança" e disparava um restart falso.
Depois desse primeiro restart o hash ficava gravado e comparações seguintes no mesmo arquivo
funcionavam certo — por isso o sintoma era "só na primeira interação depois de abrir o projeto".

**Decisão:** nova função `seed_known_hashes()` — percorre `WATCH_DIRS` e popula
`handler.known_hashes` com o hash de cada arquivo já existente, chamada antes de
`observer.start()`.

**Testado:** rodei `dev.py` de verdade (venv descartável, `JAVAFX_MODULES_HOME` setado),
confirmei: (1) um `touch` puro (sem mudar conteúdo) num arquivo já monitorado **não** dispara mais
restart — mesmo PID continua rodando; (2) uma mudança de conteúdo real **continua** disparando
restart normalmente (PID novo, app reconecta ao banco do zero). Não é possível reproduzir
"Ctrl+F no IntelliJ" especificamente neste ambiente, mas a causa raiz (primeiro touch em qualquer
arquivo sempre parecia mudança) explica o sintoma relatado independente de qual ação específica do
IntelliJ disparou o evento de sistema de arquivos.

## 2026-08-18: `PesagemViewModel` migrado da flag `destruido` manual pra `ctx.scope()` (framework)

**Contexto:** a corrida documentada na entrada abaixo foi corrigida na hora com uma flag
`volatile boolean destruido` local. Ao investigar se esse gap deveria ser resolvido no framework
(igual `viewModelScope` resolve no Android/Compose), foi adicionado `megalodonte.base.async.Scope`
no `megalodonte-libs` — cancelamento vinculado ao ciclo de vida, com `Router` (v4) cancelando
automaticamente o `Scope` de cada tela antes de chamar `onDestroy()` (ver `DECISIONS.md`/`TODO.md`
do `megalodonte-libs`).

**Decisão:** `PesagemViewModel.iniciarLeituraBalanca()` migrado de `Async.Run(...)` +
flag manual pra `ctx2.scope().run(...)` + `ctx2.scope().onCancel(leitor::parar)` — o próprio
`Router` cancela o escopo quando a tela é destruída, sem precisar de código próprio pra isso.
`pararLeituraBalanca()` mantido chamando `leitorBalanca.parar()` explicitamente (defensivo,
cobre o caso comum de conexão já estabelecida) — o cancelamento via `Scope` cobre especificamente
a corrida (conexão que termina de abrir depois da tela já ter sido destruída).

**Pré-requisito:** `megalodonte-base`/`megalodonte-router` republicados em `mavenLocal` com o
`Scope` incluído — sem isso o projeto não compila contra a nova API.

**Testado:** `./gradlew compileJava test` — **155/155 testes, BUILD SUCCESSFUL**, sem regressão.
A corrida em si continua sem teste automatizado determinístico (mesma limitação já registrada);
o `Scope` em si tem cobertura própria (`ScopeTest`, 7 casos) no `megalodonte-libs`.

---

## 2026-08-18: Corrida entre `iniciarLeituraBalanca()` e `onDestroy()` vazava conexão/thread

**Contexto:** usuário relatou travamento do computador ao deixar o `dev.py` rodando por muito
tempo, suspeitando de memory leak. Investigação empírica (rodar o `dev.py` de verdade, forçar um
restart, checar a árvore de processos com `ps --forest`) **descartou** a hipótese óbvia — o
processo Java antigo (`my_app.Main`, filho do Gradle Daemon, não do `./gradlew run` que o
`dev.py` mata) morre corretamente a cada restart, o Daemon cancela a build anterior ao detectar o
cliente desconectado.

**Bug real encontrado por leitura de código** (`PesagemViewModel`): `onMount()` chama
`iniciarLeituraBalanca()` (assíncrono, `Async.Run`) e `onDestroy()` chama
`pararLeituraBalanca()` (síncrono). Se o usuário navega pra fora da tela de Pesagem antes da
conexão (Serial ou TCP) terminar de abrir, `pararLeituraBalanca()` roda enquanto `leitorBalanca`
ainda é `null` — não tem o que parar. A conexão termina de abrir *depois*, e ninguém nunca chama
`.parar()` nela: a porta serial/socket TCP fica aberta pra sempre, com uma thread virtual presa
no loop de leitura segurando referência viva pra `ViewModel` inteira (via closure do callback
`onPeso`/`onErro`) — impedindo o garbage collector de liberar todo aquele grafo de objetos. Cada
entrada rápida na tela de Pesagem sem esperar a conexão terminar vazava uma dessas.

**Fix:** flag `volatile boolean destruido`, setada em `pararLeituraBalanca()`. `iniciarLeituraBalanca()`
checa a flag logo após criar o leitor (antes de abrir a porta/socket) e de novo logo depois de
chamar `.iniciar()` — se `destruido` já é `true` em qualquer um dos dois pontos, chama
`leitor.parar()` imediatamente em vez de deixar a conexão pendurada sem dono. Ver
`PesagemViewModel.iniciarLeituraBalanca()`/`.pararLeituraBalanca()`.

**Testado:** `./gradlew compileJava` + `./gradlew test` (155 testes, sem regressão) — a corrida em
si não tem teste automatizado (é uma race condition de timing real, não reproduzível de forma
determinística num teste unitário sem instrumentar `Async.Run`); a correção foi validada por
leitura de código, não por execução.

---

## 2026-08-17: Leitura de peso via serial/TCP implementada — `my_app/infra/balanca/`

**Contexto:** última peça sem nenhum precedente reaproveitável do `plics-sw` (evidência já
registrada antes). A auditoria do app antigo (evidências 5, 6 e 7 em
`/home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/DECISIONS.md`) mapeou os erros a não
repetir: truncamento do separador decimal no parsing do peso, porta serial hardcoded no Linux, e
um comando `"ls\n"` de shell Unix mandado pra balança via Telnet (cargo-culted de algum tutorial,
sem função real — confirmado pela pesquisa sobre "Hércules" ser só um terminal genérico de
teste, não um protocolo proprietário).

**Decisão — arquitetura:**
- `LeitorBalanca` (interface): `iniciar(Consumer<BigDecimal> onPeso, Consumer<String> onErro)` +
  `parar()` — um listener contínuo, não um "ler uma vez". `LeitorBalancaFactory` decide qual
  implementação usar a partir do `ConexaoBalancaModel` salvo (Serial ou TCP).
- `LeitorBalancaSerial`: JSSC (`jssc.SerialPort`), evento nativo de biblioteca (`addEventListener`)
  — porta e baud rate vêm da configuração, nunca hardcoded (corrige o bug do Linux).
- `LeitorBalancaTcp`: **socket TCP cru** (`java.net.Socket`), sem handshake Telnet nem comando
  nenhum enviado — a maioria dos indicadores só transmite continuamente. Roda num
  `Async.Run` (thread virtual, mesmo padrão já usado no resto do app) com timeout de leitura de
  5s tratado como "sem dado novo ainda", não como erro fatal.
- `PesoParser` (utilitário puro, com teste dedicado): extrai o número do texto bruto **sem
  descartar o separador decimal** — normaliza formato BR (`1.234,5`) e US (`1,234.5`)
  corretamente, distinguindo qual dos dois símbolos é o decimal pela posição (o que aparece por
  último).

**Decisão — UX na tela de Pesagem:** peso não é mais só digitado — `PesagemScreen` mostra o peso
ao vivo (`vm.pesoAoVivo`) e tem botões "Capturar" pra Tara e Peso Bruto (grava o valor ao vivo no
momento em que o operador aperta, igual o app antigo fazia) e um botão "Calcular" pro Peso
Líquido, que agora usa a mesma fórmula do app original (`bruto - tara`, descontado o percentual
somado dos 8 tipos de desconto) em vez de ser só mais um campo digitado à mão sem cálculo nenhum
por trás (gap que existia até agora nesta reescrita).

**Testado**: `PesoParserTest` (10 casos, incluindo os dois formatos de milhar/decimal e o caso
que era bugado no app antigo). A leitura de hardware em si (Serial/TCP de verdade) não tem teste
automatizado — depende de porta/dispositivo físico ou simulação externa, ver `README.md` do
projeto antigo pras formas de simular sem balança física (o raciocínio continua válido aqui).

---

## 2026-08-17: `V11__fix_dataCriacao_timestamp.sql` — corrige em produção o bug do `REAL` em
qualquer banco já criado antes da correção

**Contexto:** a correção de `dataCriacao REAL` → `TIMESTAMP` (ver decisão mais abaixo) só
consertava migrations que **ainda não tinham sido aplicadas**. Quem já tinha rodado o app antes
(inclusive esta máquina, `~/.gobitech/erp.db`) continuava com o schema antigo no disco — Flyway
não re-executa uma migration só porque o `.sql` mudou depois de já aplicada, só
`flyway.repair()` (chamado no `Main.java`) atualiza o checksum sem recriar nada. Resultado:
`Illegal Argument occurred setting property: dataCriacao ... Type read: class java.lang.Double`
ao abrir o app de verdade — o mesmo bug da evidência anterior, só que em produção, não em teste.

**Decisão:** nova migration `V11`, recriando as 9 tabelas com `dataCriacao`/`expira_em` como
`TIMESTAMP`, preservando os dados (`RENAME → CREATE correto → INSERT SELECT → DROP`, mesmo
padrão do `V20`/`V21` do `plics-sw`). `preferencias` precisou de `SELECT` explícito (não
`SELECT *`) porque esse banco específico também tinha sobrado com colunas antigas
(`tema`/`credenciais_habilitadas`/`login`/`senha`/`licensa`, removidas numa decisão anterior) —
as outras 8 tabelas bateram exatamente com o schema esperado.

**Validado antes de aplicar**: copiei o banco real (`~/.gobitech/erp.db`) pra um arquivo
temporário, rodei a `V11` nele via um teste JUnit descartável (removido depois de confirmar) —
schema virou `TIMESTAMP`, dado preservado, e a leitura via Persism (que estava quebrando) passou
a funcionar. Como `Main.java` já roda `flyway.migrate()` toda vez que o app abre, **o próprio
app se corrige sozinho na próxima abertura** — não precisei editar o arquivo real na mão.

---

## 2026-08-17: `preferencias` simplificada — removidos `credenciais_habilitadas`, `login`,
`senha` e `licensa`

**Contexto:** com o login por usuário real implementado (ver decisão de "Login agora é por
usuário real" mais abaixo), esses 4 campos de `preferencias` ficaram órfãos — `login`/`senha`
eram do modelo antigo de login único compartilhado, `credenciais_habilitadas` era o toggle que
decidia se esse login único era exigido, e `licensa` (campo solto) foi superado pela tabela
`licensas` de verdade. Conferido: nenhum dos quatro tinha mais leitura/escrita fora da própria
tela de Preferências antes da limpeza.

**Decisão:** removidos da migration (`V2__criar_preferencias.sql`), do `PreferenciasModel`, do
`PreferenciasService` (a validação inteira dependia só de `credenciais_habilitadas`, então
sumiu junto) e da UI (`PreferenciasScreen`/`ViewModel` — sobrou só o seletor de tema).
`preferencias` agora só tem `tema` e `primeiro_acesso` (que continua controlando a tela de
boas-vindas). `signOut()` também mudou: antes resetava campos de `preferencias` pra "deslogar";
agora só limpa `SessaoUsuario` e navega pra AUTH — nem precisa mais tocar no banco pra isso.

---

## 2026-08-17: Reescrever com Megalodonte em vez de consertar o Java Swing/MySQL original

**Contexto:** auditoria completa do código original (ver
`/home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/DECISIONS.md`) encontrou 13 bugs
confirmados — jar que não inicia, sistema de licença com SQL quebrado, nome de motorista
hardcoded em todo ticket impresso, fotos da pesagem que nunca recarregam, filtro com bug de
precedência AND/OR, MySQL cliente-servidor sem necessidade real (uma balança = um PC), câmera
nunca conectada ao fluxo de pesagem.

**Decisão:** reescrever do zero usando o framework Megalodonte, partindo de uma cópia do
`plics-sw` (ERP já maduro, mesmo framework) e trocando a camada de dados e as telas pro domínio
de pesagem. Ver `ENTREGAS.md` no projeto antigo pro raciocínio de custo/benefício completo
(estimativa de horas, comparação com o custo de só consertar o código antigo).

**Consequência:** resolve de graça vários dos bugs originais (SQLite embutido em vez de MySQL,
padrão de Model/Repository/Service testável, Flyway versionando o schema em vez de ALTER TABLE
manual) — mas exige reconstruir tudo, inclusive integração com hardware (balança) que não tem
nenhum precedente no `plics-sw`.

---

## 2026-08-17: SQLite + Persism + Flyway no lugar de MySQL

**Contexto:** o app original conectava em `localhost:3305` — MySQL rodando na mesma máquina do
software, uma dependência de instalação/manutenção extra sem necessidade real (não há evidência
de múltiplas estações de pesagem compartilhando um banco central).

**Decisão:** usar o mesmo stack já validado no `plics-sw` — SQLite embutido (arquivo local, sem
processo de servidor) + Persism como ORM + Flyway pra versionar o schema. Convenção de tipos:
`id INTEGER PRIMARY KEY AUTOINCREMENT`, `BIT` pra booleano, `dataCriacao TIMESTAMP` pra campos
mapeados como `LocalDateTime` na Model (ver correção abaixo — errei essa parte na primeira
versão desta migration e só descobri rodando os testes automatizados).

**Correção (mesmo dia, ao escrever os testes automatizados):** a primeira versão das migrations
usava `dataCriacao REAL` (copiando o padrão de `V20`/`V21` do `plics-sw`, que existe pra
consertar um bug de overflow — mas aquele bug era especificamente sobre campos mapeados como
`Long`/`Integer` primitivo, não `LocalDateTime`). Rodar os 141 testes revelou o problema de
verdade: **qualquer método que relê do banco** (`atualizar`, `buscarById`, `listar`,
`buscarUnico`, `buscarPorX`) quebrava com `PersismException`/`IllegalArgumentException:
argument type mismatch` — o Persism lê uma coluna `REAL` como `Double` e tenta chamar
`setDataCriacao(Double)` via reflexão numa Model que espera `LocalDateTime`, e não tem conversão
automática registrada pra esse par de tipos. `salvar()` (insert puro, sem reler) passava
disfarçando o problema. Todas as Models antigas do `plics-sw` que funcionavam de verdade
(`ClienteModel`, `EmpresaModel`, `ProdutoModel` etc.) sempre usaram `dataCriacao TIMESTAMP`, não
`REAL` — isso já estava certo na origem, eu que copiei o padrão errado ao escrever as migrations
novas. Corrigido em todas as 9 tabelas (`dataCriacao` e `licensas.expira_em`, o único outro
campo `LocalDateTime` do schema). Campos `BigDecimal` (pesos, percentuais de desconto)
continuam `REAL` normalmente — o problema era só com `LocalDateTime`.

---

## 2026-08-17: Schema simplificado — produtos e clientes bem mais enxutos que o design original

**Contexto:** o DER original (anexo do `.docx` de especificação) e o schema final do app antigo
tinham campos de varejo genérico (preço de compra/venda, estoque, fornecedor em `produtos`) que
nunca fizeram sentido pro domínio de pesagem — nem o próprio app antigo usava a maioria deles.

**Decisão:** `produtos` ficou com só `nome`, `unidade`, `observacoes` — o suficiente pro que a
tela de pesagem precisa (selecionar o que está sendo pesado). `clientes` ganhou os campos reais
do protótipo (loja, razão social, CPF/CNPJ, endereço completo) só que reaproveitando o
componente de endereço já pronto do framework (`EnderecoState`/`Components.enderecoComponent`)
— por isso os campos de endereço usam os nomes que esse componente espera (`cep`/`uf`/`cidade`/
`bairro`/`rua`/`numero`), não os nomes do app antigo (`municipio`/`estado`/`endereco`).

---

## 2026-08-17: Fotos da pesagem renomeadas (`foto_frente_1/2`, `foto_costas_1/2`)

**Contexto:** o app antigo guardava as 4 fotos em colunas genéricas `image_1..4`, e um bug de
copiar-e-colar em `DTO/Weighing.java` fazia as fotos 3 e 4 nunca carregarem do banco — a
genericidade do nome das colunas não ajudou ninguém a perceber o bug mais cedo (ver evidência 4
na auditoria do projeto antigo).

**Decisão:** usar os nomes que o próprio DER original já sugeria (`front_image_1/2`,
`back_image_1/2`, traduzidos): `foto_frente_1`, `foto_frente_2`, `foto_costas_1`, `foto_costas_2`
em `pesagens`. Nomes explícitos deixam claro o que cada campo é, e o `PesagemModel` novo não tem
nenhum construtor duplicado herdado do app antigo pra repetir aquele tipo de bug.

---

## 2026-08-17: Login agora é por usuário real, sempre obrigatório

**Contexto:** o `plics-sw` original (de onde este projeto partiu) tem um modelo de login único
compartilhado — `preferencias.login`/`senha`, com um toggle (`credenciais_habilitadas`) pra até
desligar a exigência de login inteiramente. Isso nunca fez sentido pro domínio de pesagem: a
especificação original (item 2) pede "Auth de usuários — login e definição de permissões para
diferentes tipos de usuários", ou seja, contas individuais, não um login só.

**Decisão:** `usuarios` (tabela nova, com `login`/`senha`/`admin`) é a fonte de autenticação real
— `AuthScreenViewModel.entrar()` chama `UsuarioService.autenticar(login, senha)`. O toggle
`credenciais_habilitadas` de `preferencias` não é mais usado pra decidir se pede login — login é
sempre obrigatório agora (`InitialRouteResolver.resolve()` simplificado: só decide entre WELCOME,
na primeira vez, e AUTH, sempre depois). `preferencias.primeiro_acesso` continua controlando só a
tela de boas-vindas (mecanismo genérico, não específico de varejo — não precisou mudar).

**Seed**: usuário `gestor`/senha `1234` (migration `V10__dados_padrao.sql`) — mesma senha que já
era usada como "senha mestra" no app antigo, mantida como ponto de partida conhecido.

---

## 2026-08-17: Licença — modelo confirmado com o Guilherme, implementado

**Contexto:** a auditoria do app antigo (evidência 10) achou que o modelo de licenciamento nunca
foi implementado como a especificação pedia (chave gerada pela DGB) — no app antigo, qualquer
admin gerava a própria chave sem controle nenhum. Perguntei ao Guilherme se o André pretende
revender/licenciar isso pra outras balanças no futuro.

**Resposta do Guilherme:** sim, o André vai gerar quantas licenças precisar, ele mesmo — sem API
nem backend. Ele tem seu próprio login de admin, entra em qualquer computador com o app
instalado, e gera a licença ali (com data de validade), local.

**Decisão:** implementado exatamente assim — sem servidor de validação, sem chamada de rede.
`LicensaScreen` (nova) só fica acessível/visível a quem está logado com `usuarios.admin = true`
(`SessaoUsuario.isAdmin()` — tanto o item de menu quanto a tela em si checam isso, ver
`LicensaViewModel.acessoPermitido()`/`bloquearAcesso()`). Chama `LicensaService.gerarNova(
expiraEm)`, que já existia. Isso É o modelo pedido — não é um placeholder até algo melhor.

**Detalhe importante corrigido em `AuthScreenViewModel`**: a checagem de licença expirada só
bloqueia usuários **não-admin**. Se bloqueasse todo mundo, um admin com a licença expirada ficaria
trancado pra fora do próprio sistema, sem conseguir logar pra gerar uma nova — problema do tipo
"ovo e galinha" que o modelo do Guilherme depende de evitar (é o próprio André, logado, quem
resolve a licença).

**O que foi removido, permanece removido**: o `VerificacaoAcessoService.acessoLiberado()`
original do `plics-sw` (tirado do `Main.java` antes desta decisão) continua fora — ele checava um
valor publicado no site oficial *do Plics SW*, o que nunca fez sentido pra esse produto, e o
modelo confirmado (self-service local, sem backend) também não precisa dele.

---

## 2026-08-17: Recursos específicos de varejo/marketing do `plics-sw` removidos, não adaptados

**Contexto:** por ter partido de uma cópia do `plics-sw`, várias telas/serviços vieram junto sem
fazer sentido pro domínio de pesagem.

**Decisão — removidos** (não são bugs, são features de outro produto):
- Dashboard financeiro da Home (receitas/despesas de vendas/compras/contas a pagar-receber) —
  não existe mais "venda"/"compra" nesse domínio.
- Modal de promoção do Instagram na Home — marketing específico do Plics SW.
- Seleção de impressora térmica em Preferências — o app antigo já exportava ticket em PDF, não
  ESC/POS; sem impressora térmica, sem necessidade de configurar porta/spooler.
- Telas inteiras de Fornecedor, Compras, Contas a Pagar/Receber, Técnico, Ordem de Serviço, PDV,
  Pedidos, Relatórios, Categoria, Cor — junto com Models/Repositories/Services/DTOs associados.
- "Buscar atualização" na Home — dependia de `my_app.infra.UpdaterService`, que **não existe**
  nesta cópia do projeto (parece ter sido adicionado numa versão mais recente do `plics-sw` do
  que a copiada aqui). Não foi recriado — é infraestrutura de packaging, fora do escopo de
  telas/dados desta sessão.

**Mantidos, sem mudança**: link de suporte via WhatsApp e "Site Oficial" na tela de login —
não tenho evidência de que sejam contato do Plics SW especificamente vs. contato do
desenvolvedor oferecendo suporte direto pro cliente; não removidos sem confirmação.

---

## 2026-08-17: Peso da pesagem é digitado manualmente por enquanto (sem integração real com a
balança)

**Contexto:** ler o peso de verdade exige abrir uma porta serial (JSSC, já usado no `plics-sw`
pra impressora térmica, mas nunca pra ler um dispositivo) ou uma conexão TCP com o indicador de
peso — nenhum dos dois tem qualquer precedente reaproveitável do `plics-sw`. É a única parte do
projeto sem nenhuma infraestrutura pronta pra copiar.

**Decisão:** os campos de peso em `PesagemScreen` são inputs numéricos comuns por enquanto —
suficiente pra testar e validar todo o resto do fluxo (CRUD, desconto, ticket, filtro) sem
depender de hardware. A leitura real fica como TODO — ver `TODO.md` e o `README.md` do projeto
antigo (`/home/eliezer/Desktop/dev/outros/balanca-gobitech/README.md`) pra formas de simular uma
balança sem hardware físico, que continuam válidas conceitualmente pra testar a integração real
quando ela for construída.
