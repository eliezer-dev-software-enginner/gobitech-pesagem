# Contexto do Projeto

## O que é
Reescrita do software de pesagem da Balanças Gobitech (cliente André, via DGB Tecnologia/
Guilherme), usando o framework próprio Megalodonte, no lugar de consertar o código legado
Java Swing + MySQL original. Decisão tomada em 2026-08-17 depois de uma auditoria completa do
código antigo — ver `/home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/` (`CONTEXT.md`,
`DECISIONS.md`, `TODO.md`, `ENTREGAS.md`) pro histórico completo: 13 bugs confirmados, mapeamento
especificação × entregue, e a decisão de negócio (R$35/h, ~30-50h pra Fase 1) que motivou a
reescrita em vez do conserto.

**Por que reescrever em vez de consertar**: o código antigo tinha bugs estruturais (jar que não
roda, licença com SQL quebrado, MySQL cliente-servidor desnecessário pra uma balança de estação
única, câmera nunca conectada ao fluxo de pesagem) — ver auditoria linkada acima. Reescrever com
Megalodonte + reaproveitando a infraestrutura já madura do `plics-sw` (padrão CRUD, SQLite+Persism
+Flyway, exportação em PDF, empacotamento) comprime bastante o esforço em cima desses mesmos
problemas, sem herdar os bugs específicos do código antigo.

## Origem deste projeto
Começou como uma cópia literal do `plics-sw` (ERP de varejo — vendas, compras, estoque,
contas a pagar/receber). Nesta sessão, o schema, a camada de dados (Models/Repositories/
Services) e as telas (Screens/ViewModels) foram trocados pra refletir o domínio de pesagem —
ver `DECISIONS.md` pras decisões específicas de cada troca.

## Stack
- Java 25, JavaFX + Megalodonte (framework de UI próprio)
- Persism (ORM) + SQLite — banco embutido, sem servidor externo pra instalar/manter
- Flyway (migrations, em `src/main/resources/flyway_migrations`)
- Gradle (`./gradlew`)
- Padrão de tela: `Screen` + `ViewModel` (uma pasta por entidade em `my_app/screens/`),
  `ContratoTelaCrudV3`/`ViewModelScreenContract` como contrato genérico de CRUD
- Camada de dados: `Model` (Persism, anotado com `@Table`/`@Column`) + `Repository`
  (`BaseRepository<M>`) + `Service` (`BaseService<M>`) em `my_app/db/`

## Estrutura de entidades (Fase 1)
| Entidade | Tabela | Tela |
|---|---|---|
| Usuário | `usuarios` | `UsuarioScreen` |
| Cliente | `clientes` | `ClienteScreen` |
| Produto | `produtos` | `ProdutoScreen` |
| Pesagem | `pesagens` | `PesagemEntrada/Saida/Avulsa/ManualScreen` (formulários por tipo) + `PesagemHistoricoScreen` (lista) |
| Desconto | `descontos` | (sem tela própria — editado dentro da Pesagem) |
| Empresa | `empresas` | `CadastroEmpresaScreen` (dados/logo pro cabeçalho do ticket) |
| Preferências | `preferencias` | `PreferenciasScreen` (config única do app) |
| Licença | `licensas` | `LicensaScreen` (só visível/acessível pra usuário admin) |
| Conexão da balança | `conexao_balanca` | `ConexaoBalancaScreen` (Serial ou TCP) |

**Fora da Fase 1, adiado pra Fase 2**: câmera Intelbras — **já implementada** (tabela
`conexao_camera` criada via V13 + `ConexaoCameraScreen`/`ConexaoCameraViewModel`), mas o item de
menu "Conexão das câmeras" está **comentado** em `HomeScreen.java:74` **por decisão do usuário**
(pendência M3 da vistoria, "decidido: manter" — a câmera entra no fluxo só na Fase 2/uso real),
deixando a tela inalcançável pela UI. Ver `/home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/ENTREGAS.md`.

## Estado atual (2026-09-08)
- **Vistoria completa do projeto concluída** (2026-09-07, auditoria *read-only*): **40 pendências**
  registradas em `docs/TODO.md` (11 alta, 21 média, 8 baixa). Todas **resolvidas ou decididas**
  nas rodadas de correção — ver o **Histórico** abaixo e as decisões em `docs/DECISIONS.md`.
- **Corrigido nesta sessão (5ª rodada)**: **M20** — fim do N+1 nas relações de pesagem (lotes
  `buscarPorIds`) + `count()`/`contarPorPeriodo` no dashboard; **M7** — mensagens de erro
  amigáveis nas telas (detalhe técnico só em `log.error`, `IllegalArgumentException` mantém o
  `getMessage()`); **M9** — validações telefone/CEP/CPF/CNPJ centralizadas em `Validacoes`;
  **A9** — regras críticas da pesagem extraídas pra classe pura testável `PesagemRegras` (soma
  descontos ≤ 100%, líquido negativo, nenhum peso informado, `preencherDaEntrada`, fotos). Ver
  `DECISIONS.md` 2026-09-08.
- **Corrigido na vistoria (2026-09-07, rodadas 1-3)**: A5 (filtro por data com epoch-ms — o
  Persism grava INTEGER, não texto), A6 (onDestroy), A7 (parseLong seguro), A8 (validações na
  camada de serviço), A10 (updater), A11/M12 (README), M2/M4/M5/M6/M8/M10/M11/M13/M16/M17/M18/
  M19/M21 e B1/B2/B3/B5/B6/B7/B8 — detalhes em `docs/TODO.md` e `docs/DECISIONS.md`.
- **Ajustes do usuário (2026-09-08)**: **B8 revertida** — a Pesagem de **Saída** voltou a ter o
  botão "Capturar" na **Tara** (`PesagemSaidaScreen`): a tara da entrada continua pré-preenchida
  e o campo continua somente-leitura, mas o operador pode recapturá-la com o caminhão vazio na
  volta. A seção de **Fotos** do formulário de pesagem está **desativada na Fase 1** (chamada
  comentada em `PesagemFormScreen.java` — as regras de foto já cobertas em `PesagemRegras`).
- **Decidido: manter** (decisão do usuário) — A1 (chave AES), A2 (token Telegram), A3 (seed
  admin), A4 (senhas câmera em texto puro), M1 (senha sem máscara na edição) e M3 (menu de
  câmera desativado — só na Fase 2).
- **Aberto**: **B4** — passo MSI do workflow `package.yml` deve chamar `python.exe` (o Python do
  `setup-python` é `python.exe`, não `python3` no Windows) — **fora deste clone**: o workflow
  mora no repositório `megalodonte-world` (PU), não nesta pasta.
- Testes: **235** `@Test` → `./gradlew test` → **BUILD SUCCESSFUL**.

## Histórico

### 2026-09-07 — Vistoria completa: rodadas de correção (1ª altas, 2ª-3ª médias/baixas)
- **A5/M13** — filtro por data: descoberta empírica de que Persism/sqlite-jdbc grava
  `dataCriacao` como **INTEGER epoch-ms** (não texto, como a vistoria supunha — confirmado no
  driver e no banco real). Filtro converte `LocalDate` (inclusivos) pra epoch-ms e compara
  numericamente; "pesagens do mês" do dashboard e o filtro do histórico voltam a funcionar.
  Migration `V20` (converter seeds em texto) criada nesta rodada foi **removida** — a premissa
  estava errada; seeds do `V10` já são INTEGER consistentes com o runtime.
- **A6/A7** — `onDestroy()` implementado nas 3 telas Add/Edit (Cliente/Produto/Usuário);
  `parseLong` com try/catch nas 6 telas (3 Add/Edit + 3 Details).
- **A8** — `PesagemService.salvar/atualizar` validam `tipoPesagem` e bruto<tara (só com os dois
  pesos preenchidos); `DescontoService` valida soma>100. Regras também na camada de serviço, além
  da ViewModel.
- **A10** — scripts "with-updater" + `updater_config.py` removidos (`my_app.updater.Main` não
  existe). **A11/M12** — README reescrito pro produto real (Gobitech pesagem) com pré-requisitos
  de ambiente (`JAVAFX_MODULES_HOME`, `DEV_MODE`, `GITHUB_TOKEN`).
- **M2** — menu "Logs" restrito a admin. **M5** — `EmpresaViewModel.fetchData` com alerta/log.
  **M8** — `atualizar()` valida igual a `salvar()`. **M10** — branding `plics.*` → `gobitech.*`
  (todo o resíduo). **M11** — JUnit unificado (BOM 5.13.1), `jna`/`jackson` órfãos removidos.
  **M21** — `-Dprism.verbose` só em DEV_MODE.
- **M4** — `exportPdf` usa o `snapshotFiltrado`. **M6** — trava anti duplo-clique na classe base
  das ViewModels (`tryBeginSalvar`/`endSalvar`). **M16** — `DevicesTest` removido. **M17** —
  `LeitorBalancaTcpTest` novo (ServerSocket em loopback). **M18** — testes desfragilizados:
  banco em memória **por classe** (`testdb-<Classe>`), sem `Thread.sleep`, portas efêmeras.
  **M19** — `HOTRELOAD.md` reescrito pro comportamento real do `dev.py`.
- **B1** — `Parcela.java` deletado + mains órfãos + `ACESSO_BLOQUEADO` removido. **B2** — imports
  não usados removidos (12 arquivos). **B3** — `build.gradle.kts` limpo (`publishing`/
  `maven-publish` removidos; comentários órfãos apagados). **B5** — `ProdutoService` normaliza
  `nome` com `trim()`; catches silenciosos agora logam. **B6/B7** — PDFs gerados ignorados e
  removidos do índice; resíduo do `.gitignore` removido. **B8** — Saída sem botão "Capturar" na
  tara (tara somente-leitura, vem da Entrada).
- Decidido: manter — A1 (chave AES), A2 (token Telegram), A3 (seed admin), A4 (senhas câmera em
  texto puro), M1 (senha sem máscara na edição) e M3 (menu de câmera desativado).

### 2026-09-03 — Fix: Details/Busca de usuário exibiam login hasheado
- `UsuarioService` ganhou override de `buscarById(long)` que decripta `login`/`senha` antes de
  devolver o model (texto puro na fronteira com as telas, mesmo padrão de `buscarPorLogin`/
  `listarAtivos`). Login/senha continuam sempre criptografados em repouso.

### 2026-09-03 — Inputs não-editáveis com borda vermelha
- Os pesos somente-captura (Entrada/Saída) e a tara somente-leitura da Saída usam a borda
  vermelha (`#e74c3c`) do "Peso líquido" — `Components.InputColumnInteger(..., disableInput=true)`
  aplica a borda quando desativado (novo overload, junto com `InputWithButtonRowInteger`).

### 2026-09-03 — Pesagem: captura × digitação por tipo de tela
- `PesagemFormScreen` separa `permitirCapturar*` (botão "Capturar") de `*Editavel()` (aceita
  digitação); com botão o campo fica somente-leitura. Resultado: **Entrada** = tara+bruto
  captura-only; **Saída** = tara somente-leitura (vem da entrada) + bruto captura-only;
  **Avulsa** = tara digitada (sem botão — **reverte** o Item 7) + bruto captura-only;
  **Manual** = tara+bruto digitados (inalterado).

### 2026-09-02 — Peso da balança em tempo real no dashboard
- `DashboardViewModel` ganhou a mesma leitura contínua da balança do formulário de pesagem
  (`pesoAoVivo` + `lendoBalanca`, via `ConexaoBalancaService`/`LeitorBalancaFactory`);
  `DashboardScreen` exibe "Peso da balança agora (Kg):" no topo, ligando a leitura no
  `onMount`/`onDestroy`.

### 2026-09-02 — Utilitários movidos pro pacote `pack-utilities`
- Dependência nova `com.github.eliezer-dev-software-enginner:pack-utilities:v1.0.0` (pacote
  `pack.utilities.*`). `Utils.java` enxugada pra **só `timestampParaArquivo()`**; validação e
  formatação agora vêm de `ValidatorPack`/`FormatterPack`/`CurrencyPack` (`isValidDocumento`,
  `isValidCpfOrCnpj`, `isValidPhone`, `isValidCep`, `formatCpfCnpj`, `formatRgCpf`, `formatPhone`,
  `formatCep`, `formatCnpj`, `toBRLCurrency`, `deCentavosParaReal`). Cuidado: validadores de
  CPF/CNPJ do pacote são **mais rigorosos** (dígito verificador real).
- **`DateUtils` local removido** — os 9 métodos tinham equivalente 1:1 no `pack.utilities`
  `DatePack` (mesmos formatos `dd/MM/yyyy` e `dd/MM/yyyy HH:mm` e tratamento de null/0).
- `UtilsTest` reduzido ao teste de `timestampParaArquivo` (os casos de validação passaram a
  valer sobre o `pack-utilities`, testado lá).

### 2026-09-02 — Polimento de UX/pesagem (9 itens do TODO)
- Botão "Copiar placa" no modal de detalhes do histórico; correção "Registrar registrar ...";
  busca de placa insensível a maiúsc/minúsc (`UPPER(placa)=UPPER(?)`); campo Placa uppercase
  (`InputColumnUppercase`); borda vermelha em inputs não-editáveis; popup auto-dismiss (~3s);
  botão "Capturar" Tara na Pesagem avulsa (caminhão vazio na balança); **Inscrição estadual**
  da empresa (migration `V19`, campo novo + exibida nos cabeçalhos "Insc.est:"); downloads de
  relatório/ticket com data/hora no nome (`relatório - <data>.pdf`, `ticket - <data>.pdf`).

### 2026-09-02 — Fluxo J: documento do motorista + nome limitado
- Documento do motorista **validado se preenchido** (RG 8-9 dígitos ou CPF 11) e "Nome do
  motorista" limitado a 100 caracteres — na época via `Utils.isValidDocumento`; hoje a validação
  vive no `ValidatorPack.isValidDocumento` do `pack-utilities` (migração de 2026-09-02 acima).
  `PesagemService.validarCampos()` lança `IllegalArgumentException`; vazio continua permitido.

### 2026-09-01 — Fluxos G/H: líquido negativo, descontos > 100% e salvar sem peso
- **H4** — bloqueio de salvamento quando a soma dos descontos ultrapassa 100%; soma extraída pro
  método reutilizável `somaDescontos()`. **G4** — bloqueio quando Peso bruto < Tara (líquido
  negativo, alerta). **F2/G6** — aviso de confirmação antes de salvar sem nenhum peso (Sim salva,
  Não cancela), vale pras 4 telas. **G3** — colagem com ponto decimal (`8500.5` → `85.005`)
  **mantido por decisão do usuário**. Cálculo do líquido extraído pro reutilizável `calcLiquido()`.

### 2026-09-01 — Fix: líquido negativo na Entrada só-Tara + Saída sem Peso bruto + NPE de telefone
- **C1** — `recalcularPesoLiquido()` devolve vazio quando não há peso bruto (antes `0 − tara` =
  negativo). **D2** — `PesagemSaidaViewModel.preencherDaEntrada()` copia também `pesoTotal` da
  Entrada (antes só `pesoVeiculo`).
- **Bug real** — `UsuarioService.salvar/atualizar` faziam `getTelefone().isEmpty()` sem
  null-check (campo opcional) → NPE ao salvar usuário sem telefone. Corrigido com null-check;
  os 9 testes que falhavam eram esse mesmo bug.

### 2026-08-31 — Só a Placa é obrigatória na pesagem (pedido do André)
- Removida a exigência de Motorista e Cliente do `PesagemService.validarCampos`; formulários
  sem `*` nesses campos. Migration `V18` torna `motorista_nome` e `cliente_id` nullable (SQLite
  recria a tabela preservando dados/FKs e as colunas `entrada_id`/`usuario_id`). Testes:
  `motoristaEhOpcional`/`clienteEhOpcional` no lugar dos antigos "obrigatório".

### 2026-08-31 — Relatório resumido no layout monoespaçado do André + negritos
- `RelatorioPesagemPdfExporter` reescrito pra texto monoespaçado (Courier) como o do André:
  cabeçalho da empresa, linha de `_`, título centralizado, separadores de `=`, colunas,
  "Observação:" **por linha**, linha de totais. Negrito via Courier-Bold (mesma largura de
  glifo — não desalinha). Coluna **Tara (Kg) mantida** (decisão do usuário) e fonte
  auto-dimensionada pra caber na A4.

### 2026-08-31 — Ticket de pesagem no layout do André + operador (`pesagens.usuario_id`)
- `TicketPdfExporter` reproduz o ticket do André (texto monoespaçado, campos por linha) com
  **2 vias na MESMA folha** e linha de assinatura acima de cada nome. Novo campo
  `pesagens.usuario_id` (migration `V17`) guarda quem criou a pesagem (Operador no ticket),
  preenchido via `SessaoUsuario`; `PesagemHistoricoViewModel.imprimirTicket` usa a entrada
  vinculada pra Data/Hora/Peso de entrada. Fornecedor exibido vazio (modelo não tem o dado).

### 2026-08-31 — Produto opcional na pesagem + `*` nos campos obrigatórios
- Produto deixou de ser obrigatório (`pesagens.produto_id` nullable, migration `V16` recria a
  tabela) — teste vira `produtoEhOpcional`. Novo helper `Components.obrigatorio(label)` aplica
  `*` em todos os campos obrigatórios das telas (Pesagem/Login/Cliente/Produto/Usuário/Empresa/
  Conexão da balança).

### 2026-08-31 — Relatório do histórico: formato do André + coluna Tara + rodapé de totais
- 1 linha por par Entrada+Saída da mesma placa/visita; colunas `Ticket | Tara (Kg) | Entrada |
  Horário | Saída | Horário | Placa | Produto | Cliente | Peso bruto | Peso líquido`.
  Rodapé: observações, "Quantidade total entradas: N", "Total peso líquido: X". Para o
  agrupamento, novo campo `pesagens.entrada_id` (migration `V15`) — a Saída grava o id da
  Entrada que a originou.

### 2026-08-31 — Ticket térmico 80mm (ESC/POS)
- Novo `TicketThermalExporter` (`my_app/infra`) imprime o ticket numa térmica 80mm via
  `escpos-coffee` (ESC/POS) na **impressora padrão do sistema** (sem configurar porta/spooler).
  Novos botões "Imprimir térmica" no modal de detalhes do histórico ao lado de "Imprimir ticket"
  (PDF). `PesagemHistoricoViewModel.imprimirTicketTermica` reusa `buscarComRelacoes` +
  `buscarEntradaVinculada`.

### 2026-08-28 — Pesagem: telas de formulário separadas por tipo + `tipo_pesagem`
- A tela única de Pesagem (`PesagemScreen`) virou **4 formulários por tipo** — Entrada, Saída,
  Avulsa, Manual (`PesagemForm{Screen,ViewModel}` base, sem `ContratoTelaCrudV3`) + **histórico**
  (`PesagemHistorico{Screen,ViewModel}`, com `ContratoTelaCrudV3`: lista, filtro, excluir, baixar
  lista, imprimir ticket no modal). O campo `operacao` virou `tipo_pesagem` (`entrada`/`saida`/
  `avulsa`/`manual`), decidido pela tela aberta — migration `V14`. `PesagemService.salvar()`
  exige `tipoPesagem`; `buscarTaraSugerida` vem da **última entrada** da placa. Navegação nova
  (`Secao.PESAGEM_HISTORICO`); rota `PESAGENS` antiga removida.

### 2026-08-17 — Fase 1: migrations, dados, telas + testes
- Migrations `V1`–`V10` do domínio de pesagem (usuários, preferências, licenças, empresa,
  clientes, produtos, descontos, pesagens, conexão da balança + dados padrão); modelos/
  repositórios/services e telas adaptados/novos; varejo removido. `PesagemRepository.filtrar()`
  com AND corretamente agrupado (o app antigo tinha bug de precedência AND/OR aqui).
- **Bug real encontrado/testado**: migrations usavam `dataCriacao REAL`, que quebrava
  `atualizar()`/`buscarById()`/`listar()` em produção — corrigido pra `TIMESTAMP` nas 9 tabelas
  (e via `V11` pra bancos já criados, validado contra cópia do banco real).
- Testes: 149 (Repository + Service de cada entidade + `PesoParser`). Tema removido;
  `preferencias` ficou só com `primeiro_acesso`.

## Modelo de licenciamento — confirmado com o Guilherme (2026-08-17)
O André vai poder gerar quantas licenças precisar, ele mesmo — sem API nem backend. Ele tem
login de admin próprio, entra em qualquer computador com o app instalado, e gera uma licença ali
(com data de validade), local. **Isso já está implementado**: `LicensaScreen` (menu "Gerencial →
Gerar licença", só aparece pra quem está logado como admin — `SessaoUsuario.isAdmin()`) chama
`LicensaService.gerarNova(expiraEm)`. Detalhe importante: admin sempre consegue logar, mesmo com
a licença atual expirada/ausente — senão ele ficaria trancado pra fora sem conseguir gerar uma
nova. Usuários não-admin são bloqueados se a licença mais recente estiver expirada.