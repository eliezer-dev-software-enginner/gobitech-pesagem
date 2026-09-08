# TODO

## Vistoria completa do projeto (2026-09-07) — pendências encontradas

Formato: **PR.** `arquivo:linha` — descrição — sugestão. (Vistoria exaustiva de todo
`src/main`, `src/test`, migrations, build, scripts e docs; sem alteração de código. Ver
`CONTEXT.md`.)

### Decidido: manter (2026-09-07)
Os 5 itens abaixo **ficam como estão por decisão do usuário** — app de estação única, usuários
seletos, sem escala (ver `DECISIONS.md` 2026-09-07). Não executar.
- **A1** `security/CryptoManager.java:13-15` — chave AES-256 fixa hardcoded + modo `ECB`
  (sem IV, determinístico). Qualquer um com o código/JAR decripta senhas/logins de todos os
  usuários. — Decidido: manter.
- **A2** `domain/telegram/TelegramNotifierFactory.java:4-5` — token/chatId do bot cifrados com a
  mesma chave hardcoded → decriptáveis. — Decidido: manter.
- **A3** `flyway_migrations/V10__dados_padrao.sql:7-9` — login/senha do admin padrão cifrados,
  mas com chave exposta no código (A1) → credenciais recuperáveis. — Decidido: manter.
- **A4** `flyway_migrations/V13__criar_conexao_camera.sql:7,12` — `frente_senha`/`costas_senha`
  (câmeras) em **texto puro** no banco. — Decidido: manter.
- **M1** `screens/usuarioScreen/AddOrEditUsuarioScreen.java:63` — edição de usuário carrega a
  senha decriptada no campo (`InputColumnAuth` sem máscara). — Decidido: manter (exibição
  restrita a admin; parte da decisão de encriptação).

### Alta
- **[x] A5** `db/repositories/PesagemRepository.java:71-78` + `DashboardViewModel.java:59-64` +
  `V10:2,12` — filtro de data bind Long epoch-millis contra `dataCriacao` gravado pelo Persism
  como texto `yyyy-MM-dd HH:mm:ss(.f)`; INTEGER < TEXT sempre no SQLite → "até" nunca casa (0
  linhas) e "a partir de" nunca filtra. "Pesagens do mês" do dashboard fica 0; filtro de período
  do histórico incorreto. **Sem teste cobrindo o filtro por data.** — **Corrigido 2026-09-07
  (ver DECISIONS.md): a premissa "Persism grava texto" estava errada — confirmado com o driver
  (sqlite-jdbc 3.45.1.0) e numa cópia do banco real que `dataCriacao` é **INTEGER epoch-ms**
  (setTimestamp do Persism). Filtro agora converte `LocalDate` (inclusivos) pra epoch-ms no fuso
  do sistema e compara numericamente (`dataCriacao >=`/`<=`); API continua recebendo `LocalDate`.
  Teste novo `filtrarPorPeriodoFiltraPelaDataEmInclusivos` em `PesagemRepositoryTest`.**
- **[x] A6** `screens/*/AddOrEditClienteScreen.java`, `AddOrEditProdutoScreen.java`,
  `AddOrEditUsuarioScreen.java` — as 3 telas criam `ViewModel` que faz
  `EventBus.getInstance().subscribe(...)` e Service próprios, mas **não têm `onDestroy()`** →
  listener no bus global + sessão/DB abertas a cada abertura de formulário. É exatamente o
  padrão do NPE de session nula já corrigido em 2026-08-28. — **Corrigido: `onDestroy()`
  implementado nas 3 telas (chama `viewModel.onDestroy()` + fecha o service, log warn).**
- **[x] A7** `AddOrEditClienteScreen:32`, `AddOrEditProdutoScreen:33`, `AddOrEditUsuarioScreen:33`,
  `DetailsClienteScreen:44`, `DetailsProdutoScreen:38`, `DetailsUsuarioScreen:38`,
  `DetailsPesagemScreen:52` — `Long.parseLong(ctx.getParams().get("id"))` sem try/catch → rota
  malformada quebra o construtor da tela (NPE/NFE). — **Corrigido: try/catch `parseLong` nas 6
  telas com log + `ShowAlertError` + fechamento do stage (Details).**
- **[x] A8** `db/services/PesagemService.java:132-143` + `DescontoService.java:22-44` — as regras
  "bruto ≥ tara" e "soma dos descontos ≤ 100%" existem **só na ViewModel**
  (`PesagemFormViewModel.salvar()`); o Service **defaulta pesos null pra ZERO** (pesoFinal
  negativo persistível) e `DescontoService` valida nada. Salvar fora da UI persiste inválido. —
  **Corrigido: `PesagemService.salvar/atualizar` validam `tipoPesagem` obrigatório e bruto<tara
  (só quando ambos pesos > 0 — fluxo "só Tara"/C1 permitido); `DescontoService.salvar/atualizar`
  validam soma>100. Testes novos em `PesagemServiceTest`/`DescontoServiceTest`.**
- **[x] A9** Testes — **nenhum teste de ViewModel**; regras críticas só em teste manual
  (`testes-pesagem.md` H4/G4): soma>100, bruto<tara, salvar sem peso, `preencherDaEntrada`,
  `capturarFotos`. — **Corrigido 2026-09-08: regras extraídas pro `my_app/domain/pesagem/
  PesagemRegras.java` (classe pura, padrão `PesagemCalculo` — importável em teste JUnit sem
  thread do JavaFX): `somarDescontos`/`descontosUltrapassam100`, `liquidoNegativo`,
  `nenhumPesoInformado`, `preencherDaEntrada` (record `PreenchimentoEntrada`), `usarSlot2`/
  `nomeArquivoFoto`. `PesagemFormViewModel.salvar/calcLiquido` e `PesagemSaidaViewModel.
  preencherDaEntrada` delegam a ela. `PesagemRegrasTest` novo (20 casos) — `./gradlew test` →
  BUILD SUCCESSFUL.**
- **[x] A10** `scripts/updater_config.py:9`, `scripts/create-msi-with-updater.py` — `UPDATER_MAIN_CLASS
  = "my_app.updater.Main"` aponta pra pacote/classe **inexistentes** (UpdaterService removido) →
  jpackage gera launcher "Updater" morto. — **Corrigido: 3 scripts "with-updater" +
  `updater_config.py` removidos; referências em comentários limpas.**
- **[x] A11** `README.md:1-149` + `scripts/create-flatpak.py` — README descreve o **Plics SW** (ERP:
  compras/estoque/PDV/fornecedores), funções inexistentes ("Buscar atualização", `Main.isFlatpak`,
  `flatpak/README.md` — diretório `flatpak/` **não existe**) e versão errada (1.1.2 vs 1.0.1). —
  **Corrigido: README reescrito pro produto real (Gobitech pesagem), incluindo pré-requisitos
  (M12); branding do `create-flatpak.py` ajustado (M10).**

### Média
- **[x] M2** `screens/homeScreen/HomeScreen.java:87-89` — menu "Logs" visível **para todos** os
  usuários, sem `.itemIf(isAdmin, ...)`; logs podem conter logins/SQL. — **Corrigido: item
  "Ver logs da aplicação" agora é `itemIf(isAdmin, ...)`.**
- **M3** `screens/homeScreen/HomeScreen.java:74` — item "Conexão das câmeras" **comentado** →
  a tela `CONEXAO_CAMERA` (implementada, teste incluído) fica inalcançável pela UI. —
  **Decidido (2026-09-07): manter desativado** — câmera entra no fluxo só na Fase 2/uso real.
- **[x] M4** `screens/clienteScreen/ClienteScreen.java:86`, `ProdutoScreen.java:82`,
  `UsuarioScreen.java:81` — `exportPdf` ignora o `snapshotFiltrado` do contrato
  (`ContratoTelaCrudV3`) e relê `vm.filteredList` → PDF pode divergir do que o usuário viu. —
  **Corrigido: as 3 telas exportam a lista recebida no parâmetro (mesmo padrão já usado pelo
  `PesagemHistoricoScreen`).**
- **[x] M5** `screens/empresaScreen/EmpresaViewModel.java:71-73` — `fetchData()` lança
  `RuntimeException` na lambda do `Async.Run` sem `UI.runOnUi`/alerta — divergente das demais
  ViewModels. — **Corrigido: `log.error` + `UI.runOnUi(() -> ShowAlertError(...))`.**
- **[x] M6** `AddOrEditClienteScreen.java:92` / `AddOrEditProdutoScreen.java:88` —
  `viewModel.modoEdicaoState().set(false)` síncrono após `handleAddOrUpdate` assíncrono →
  duplo-clique grava 2 registros (o 2º cai no ramo de criação); `AddOrEditUsuarioScreen` não faz
  isso (divergente). — **Corrigido: trava `tryBeginSalvar()/endSalvar()` na classe base
  `ViewModelScreenContract` (AtomicBoolean liberado no `finally` do Async.Run) — o 2º clique é
  ignorado até a gravação terminar; vale pras 3 telas e fecha o divair das 2 com a de usuário.**
- **[x] M7** Muitas telas exibem `e.getMessage()` cru (stack de SQL/SO) ao usuário:
  `AuthScreenViewModel:79`, `DashboardViewModel:74,113`, `ConexaoBalancaViewModel:72,88,113`,
  `ConexaoCameraViewModel:70,101,150`, `PesagemFormViewModel:170,199,392`,
  `PesagemHistoricoViewModel:84,115,140,171,225,249`, `Details*`, `ContratoTelaCrudV3:81`,
  `LogsScreenViewModel:71`. — **Corrigido 2026-09-08: alerta pro usuário sempre com mensagem
  amigável fixa; detalhe técnico só em `log.error`. `catch (IllegalArgumentException)`
  continua exibindo `getMessage()` (validação de domínio). Mesmo padrão aplicado por
  consistência nos CRUD VMs, `LicensaVM`/`EmpresaVM`/`AddOrEdit*` e nos `onErro` dos leitores
  Serial/TCP (ver DECISIONS.md 2026-09-08).** — **Follow-up 2026-09-08: a nota dizia que
  `EmpresaVM` já estava coberto, mas só o `fetchData` tinha sido ajustado — o `handleSave` ainda
  descartava `e.getMessage()`. Corrigido (ver CONTEXT.md Histórico 2026-09-08) e feita varredura
  em todos os ViewModels/Services/telas: nenhum outro caso real remanescente.**
- **[x] M8** `db/services/PesagemService.java:47-49,58-62` — `atualizar()` não valida
  `tipoPesagem` (só `salvar()`); e o Service impõe `tipoPesagem` obrigatório além da regra "só
  placa" do domínio. — **Corrigido junto do A8: `atualizar()` valida `tipoPesagem` e bruto<tara
  (mesma regra de `salvar()`).**
- **[x] M9** `db/services/ClienteService:70`, `EmpresaService:58-67`, `UsuarioService:47-49` —
  validações de telefone/CEP/CPF duplicadas com estilos divergentes (`isValidPhone` importado ×
  `ValidatorPack.`). — **Corrigido 2026-09-08: novo `my_app/utils/Validacoes.java` centraliza
  "campo opcional + formato" (`validarTelefone`/`validarCep`/`validarCpfCnpj` — no-op se
  null/branco, `IllegalArgumentException` com mensagem padronizada), usado pelos 3 Services
  (`EmpresaService` canaliza também o CPF/CNPJ pelo helper; mensagem vira "CPF/CNPJ inválido");
  `UsuarioService` padronizou `isBlank()`. Comportamento de domínio preservado — Cliente segue
  **sem** validação de formato de CPF/CNPJ (só unicidade). `ValidacoesTest` novo (11 casos).**
- **[x] M10** `scripts/updater_config.py` / `Docs pendentes` — branding "Plics" resíduos:
  `Main.java:36` (`plics.appVersion`), `build.gradle.kts:144` (`-Dplics.appVersion`),
  `ProcessKiller.java:12,29` (`plics-killer.log`), `create-flatpak.py:19` (`PlicsSW`). —
  **Corrigido: tudo renomeado pra `gobitech.*` (ver DECISIONS.md 2026-09-07).**
- **[x] M11** `build.gradle.kts:53-54,94` — JUnit em **dois níveis** (BOM 5.10.0 + jupiter 5.13.1);
  `jna:66-67` e `jackson:109-111` **sem nenhum uso** no código. — **Corrigido: BOM único 5.13.1,
  jna/jackson removidos, declarações duplicadas implementation/testImplementation unificadas.**
- **[x] M12** Envs esperadas sem documentação (README): `JAVAFX_MODULES_HOME` (obrigatória pro
  `gradlew run`, `build.gradle.kts:127-131`), `DEV_MODE`, `GITHUB_TOKEN`. — **Corrigido: seção
  "Pré-requisitos" no README novo (A11).**
- **[x] M13** `flyway_migrations/V10:2,12` — `dataCriacao` seed em **INTEGER** (epoch-ms) vs "Persism
  gravando TEXT" → ordenação e comparações inconsistentes (relacionado a A5). — **Corrigido
  2026-09-07: a premissa estava errada — Persism grava INTEGER epoch-ms (confirmado no driver e
  no banco real). Seeds já são INTEGER consistentes; migration `V20` criada nesta rodada foi
  **removida** (criaria tipos mistos). Sem alteração de schema.**
- **[x] M14** `docs/CONTEXT.md` — 17 seções "Estado atual" fora de ordem cronológica, datas
  duplicadas; cita `Utils.isValidDocumento`/"UtilsTest 7 casos" já movidos pro `pack-utilities`;
  contagem de testes desatualizada (**199** nos docs vs **189** `@Test` reais). — **Corrigido
  2026-09-08: `CONTEXT.md` consolidado — uma seção "Estado atual (2026-09-08)" (vistoria
  completa + o que foi corrigido na 5ª rodada + aberto B4 + **235** `@Test` medidos) + seção
  "Histórico" cronológica com as 18 entradas datadas; citação do fluxo J anotada (validação de
  documento agora vive no `ValidatorPack` do `pack-utilities`).**
- **[x] M15** `docs/TODO.md:412-418` — "Pendente" desatualizado: `Main.APP_NAME` já é "Gobitech"
  (só as chaves `plics.*` sobraram — M10); parcialmente resolvido. — **Revisado 2026-09-08: a
  seção "Resolvido — branding/empacotamento" já está correta e marcada `[x]` (M10 cobre as
  chaves `plics.*` restantes); `CONTEXT.md` refletia o `APP_NAME` novo. Nenhuma pendência
  remanescente — fechado.**
- **[x] M16** `src/test/java/my_app/DevicesTest.java` — é um `main()` manual (JSSC) **não-JUnit**
  dependente de hardware, fazendo parte do source set de teste. — **Corrigido: **removido** — não
  era `public static void main` (nem rodava) e a listagem de portas já existe na UI
  (`ConexaoBalancaViewModel`).**
- **[x] M17** `infra/balanca/` — `LeitorBalancaSerial`/`LeitorBalancaTcp`/`Factory` **sem teste**
  (só `PesoParser`); nunca validado contra hardware real. Existe `scripts/simular_balanca_tcp.py`.
  — **Corrigido: novo `LeitorBalancaTcpTest`** — ServerSocket em loopback (porta efêmera) simula
  o indicador transmitindo o peso continuamente; também cobre o erro de conexão recusada com
  porta efêmera liberada na hora.
- **[x] M18** Testes frágeis: `LicensaServiceTest:61` (`Thread.sleep(2)` pra diferenciar
  dataCriacao); `CameraSnapshotClientTest:114-119` conecta em `127.0.0.1:1`;
  `BaseRepositoryTest`/`BaseServiceTest` compartilham `file:testdb?mode=memory&cache=shared` sem
  isolamento entre classes. — **Corrigido: `LicensaServiceTest` gera até ter timestamp distinto
  (loop, sem sleep); câmera usa porta efêmera recém-liberada; cada classe de teste usa banco
  em memória próprio (`testdb-<Classe>`) via `testUrl()`.**
- **[x] M19** `HOTRELOAD.md:23-28` — descreve compilação `javac` + classe `Reloader` que não
  existe; o `dev.py` real reinicia via `gradlew run`. — **Corrigido: reescrito pro comportamento
  real (hashes SHA-256 + kill/restart; comandos Windows/Linux; ruído ignorado).**
- **[x] M20** `db/services/PesagemService.java:106-119` — `anexarRelacoes` faz **N+1** SELECTs por
  pesagem (listas); `DashboardViewModel` ainda lista tudo só pra `size()`. — **Corrigido:
  `BaseRepository.buscarPorIds(Collection)` novo (`WHERE id IN (...)`) e `PesagemService.
  anexarRelacoes(List)` anexa Cliente/Produto/Desconto/Usuario de uma lista inteira com **4
  SELECTs em lote** (N×4 → 4). Dashboard usa `count()`/`contarPorPeriodo` (`SELECT COUNT(*)`)
  no lugar de trafegar listas; `PesagemRepository.contarPorPeriodo` reaproveita o critério de
  datas de `filtrar`. Testes novos: `count`/`buscarPorIds`/`buscarPorIdsVazio` em
  `ClienteRepositoryTest` e `countRetornaOTotalDePesagens`/`contarPorPeriodo*`/
  `listarComRelacoesAnexaClientesDistintosEmLote` em `PesagemServiceTest`.**
- **[x] M21** `build.gradle.kts:143` — `-Dprism.verbose=true` fixo no `run` (debug do JavaFX). —
  **Corrigido: só entra no `jvmArgs` quando `DEV_MODE` está setado no ambiente de quem chamou o
  gradle.**

### Baixa
- **[x] B1** `domain/Parcela.java` — classe morta (só import não usado em `Components:36`), divisão em
  `double` sem `RoundingMode` (centavos imprecisos); `domain/Data.java:35-37` — `main()` de teste
  órfão; `TelegramNotifier.java:132-135` — `main()` manual sobrando; `AppRoutes:40` —
  `ACESSO_BLOQUEADO` sem rota/tela. — **Corrigido: `Parcela.java` deletado (import removido das
  `Components`); `main()` órfãos removidos; `ACESSO_BLOQUEADO` removido do enum.**
- **[x] B2** Imports não usados: `PesagemService:5`, `CryptoManager:7-8`, `AuthScreen:3`,
  `Sidebar:22`, `UsuarioScreenViewModel:8`, `PesagemHistoricoViewModel:28-29`,
  `PesagemFormScreen:5,11`, `PesagemHistoricoScreen:5,12`, `ConexaoBalancaViewModel:18`,
  `ContratoTelaCrudV3:27,34`, `ViewModelScreenContract:10`, `Components:34,36,38`. — **Corrigido:
  todos removidos (`UsuarioModel`/`Files`/`Path`/`Redirect`/`SessaoUsuario`/`ProdutoEvent`/
  `Comparator`/`HashSet`/`Button`×2/`ButtonProps`×2/`jSerialComm.SerialPort`/`Stage`/`Show`/
  `AntDesignIconsOutlined`/`persism.Column`; `Show`+`Parcela` nas `Components`).**
- **[x] B3** `build.gradle.kts` — comentários obsoletos (`:12-13`, ":41", ":63"); dependências
  repetidas em `implementation`+`testImplementation` (`:80/100, :82/103, :83/97, :93/106`);
  `tasks.jar enabled=false` + `publishing` com `components["java"]` órfão. — **Corrigido:
  comentários órfãos removidos; `publishing`/plugin `maven-publish` removidos (nada usa — os
  scripts empacotam com `shadowJar`); bloco morto do `tasks.jar` enxugado (fica `enabled=false`),
  com comentário explicando por quê. As duplicatas de dependência já haviam sido removidas em
  M11.**
- **B4** `.github/workflows/package.yml:100-103` — passo MSI usa `python3` no runner Windows
  (Python do `setup-python` é `python.exe`); comentário de outro projeto (`:3-5`). — Sugestão:
  normalizar invocação. **⚠ Fora do clone atual** — o workflow mora no repositório
  `megalodonte-world` (PU), não nesta pasta. Registrar no repo do PU quando atualizar por lá.
- **[x] B5** `ProdutoService.java:52-54` — checa duplicidade com `trim()` mas persiste sem `trim` →
  "Arroz" vs "Arroz ". — Sugestão: normalizar no salvar. `LicensaService:47-51` — `expiraEm`
  null = licença eterna; `DB.java:37` loga URL completa; `DB.java:83`/`ProcessKiller:19`/
  `ListaPdfExporter:59` — `catch (Exception ignored)` sem log. — **Corrigido: `ProdutoService`
  normaliza `model.setNome(nome.trim())` em `salvar`/`atualizar`; catches silenciosos agora
  logam (warn no `DB.closeAllSessions` e no logo do `ListaPdfExporter`; System.err no
  `ProcessKiller`, onde logar via slf4j falharia no mesmo arquivo). `expiraEm` null = licença
  eterna **já era** o comportamento correto (testado) — mantido e documentado; o log de URL no
  `DB` é caminho local do arquivo (não sensível) — mantido.**
- **[x] B6** PDFs gerados commitados no repo: `lista.pdf`, `relatorio.pdf`, `ticket_pesagem_20/34.pdf`,
  `relatorios/*.pdf`, `tickets/*.pdf`, `META-INF/MANIFEST.MF`. — **Corrigido: adicionados ao
  `.gitignore` e removidos do índice (`git rm --cached`).**
- **[x] B7** `.gitignore:5` — resíduo `plics-sw-new-version-for-test/`. — **Corrigido: removido.**
- **[x] B8** `screens/pesagemScreen/PesagemSaidaScreen.java:23-26` — `permitirCapturarTara()=true`
  é o default e **contradiz** o javadoc da própria classe ("a Tara vem da entrada — não é
  capturada de novo"); sinaliza botão "Capturar" na tara da Saída. — **Corrigido: retorna `false`
  e `taraEditavel()=false` (tara somente-leitura, vem da Entrada) — alinhado ao comportamento
  documentado (Item 2026-09-03 e CONTEXT).**
- [x] `PesagemFormScreen`: novos hooks `taraEditavel()`/`brutoEditavel()` + lógica no
      `secaoPesos` — campo com botão "Capturar" fica somente-leitura (borda vermelha); campo sem
      botão e não-editável também fica somente-leitura
- [x] `Components`: overloads `InputColumnInteger(..., disableInput)` e
      `InputWithButtonRowInteger(..., disableInput)`
- [x] **Entrada**: tara + bruto só capturam (não digitam)
- [x] **Saída**: tara somente-leitura sem botão (vem da entrada, `taraEditavel()=false`); bruto
      só captura
- [x] **Avulsa**: tara digitada (sem botão — reverte o Item 7); bruto só captura
- [x] **Manual**: tara + bruto digitados, sem botão (inalterado)
- [x] `./gradlew test`: **BUILD SUCCESSFUL**

## Concluído (fix — Details usuário exibindo login hasheado — 2026-09-03)
- [x] `UsuarioService.buscarById(long)` agora decripta `login`/`senha` antes de devolver o
      model (override sobre o `BaseService`), mesmo padrão de `buscarPorLogin`/`listarAtivos` —
      corrige `DetailsUsuarioScreen` (mostrava o login criptografado) e `AddOrEditUsuarioScreen`
      (`populateFieldsFromModel`)
- [x] `UsuarioServiceTest.deveRetornarLoginESenhaEmTextoPuroAoBuscarPorId` novo — `./gradlew
      test`: **BUILD SUCCESSFUL**

## Concluído (lote de melhorias de UX/polimento — 2026-09-02)
- [x] **Item 1 — botão copiar placa**: novo botão "Copiar placa" no modal de detalhes do
      histórico (`PesagemHistoricoScreen.itemDetails`) que copia a placa pra área de
      transferência (útil pra colar na busca da pesagem de saída)
- [x] **Item 2 — "Registrar registrar ..."**: `PesagemFormViewModel.textoBotaoSalvar()` fazia
      `"Registrar " + tituloFormulario()` — como os títulos já começam com "Registrar",
      virava "Registrar registrar pesagem de saída". Corrigido pra retornar só o título.
- [x] **Item 3 — busca de placa ignora maiúsc/minúsc**: `PesagemRepository.buscarPorPlaca`,
      `buscarPorPlacaETipo` e `filtrar` passaram a usar `UPPER(placa) = UPPER(?)` — a busca da
      Saída encontra a entrada mesmo com a caixa diferente (SQLite compara placa em binário
      por padrão)
- [x] **Item 4 — borda vermelha em input não-editável**: `InputColumn` com
      `disableInput=true` (ex.: Peso líquido) agora usa borda vermelha pra sinalizar somente
      leitura
- [x] **Item 5 — input uppercase pra Placa**: novo `Components.InputColumnUppercase`
      (força MAIÚSCULAS no display e no state), usado no campo Placa das 4 telas de pesagem
- [x] **Item 6 — popup some sozinho**: `Components.ShowPopup` agora esconde após ~3s
      (`PauseTransition`), além de sumir ao clicar fora
- [x] **Item 7 — Capturar Tara na avulsa**: decisão do usuário: mostrar o botão "Capturar"
      na tara da Pesagem avulsa (caminhão vazio está na balança). Removido o override
      `permitirCapturarTara()=false` do `PesagemAvulsaScreen`.
- [x] **Item 8 — Inscrição estadual da empresa**: novo campo `empresas.inscricao_estadual`
      (migration `V19`, opcional) + campo no `CadastroEmpresaScreen`/`EmpresaViewModel` +
      modelo; exibido no cabeçalho de relatório e tickets ("Insc.est: ...") no
      `RelatorioPesagemPdfExporter`, `TicketPdfExporter` e `TicketThermalExporter`
- [x] **Item 9 — nome de arquivo de baixar com data/hora + prefixo**: novo
      `Utils.timestampParaArquivo()` (`yyyy-MM-dd_HHmm`, seguro pra nome de arquivo). Relatório
      (`ContratoTelaCrudV3.handleClickBaixarLista`) → `relatório - <data>.pdf`; ticket
      (`PesagemHistoricoViewModel.imprimirTicket`) → `ticket - <data>.pdf`
- [x] Testes: `UtilsTest` (timestamp), `EmpresaServiceTest` (inscricao persistida),
      `PesagemRepositoryTest` (busca/filtro de placa ignorando caixa), exporters (Insc.est no
      ticket PDF/térmico e relatório) — `./gradlew test`: **BUILD SUCCESSFUL**

## Concluído (fluxo J — validar documento e limitar nome do motorista — 2026-09-02)
- [x] `Utils.isValidDocumento(String)`: aceita vazio/nulo, RG (8-9 dígitos) ou CPF (11) — J2
- [x] `PesagemService.validarCampos()` **lança `IllegalArgumentException`** quando o documento
      preenchido não é RG/CPF válido e quando o Nome do motorista excede 100 caracteres — J2/J4
      (validação de negócio na Service, seguindo o padrão da "Placa é obrigatória")
- [x] `UtilsTest`: 7 novos casos de `isValidDocumento`
- [x] `PesagemServiceTest`: 5 novos casos (documento inválido/vazio/RG/CPF; nome 101 e 100 chars)
- [x] `./gradlew test`: **BUILD SUCCESSFUL** — `testes-pesagem.md` atualizado (J2 e J4 = ok)

## Concluído (fluxo H — bloquear soma de descontos > 100% — 2026-09-01)
- [x] Bloqueio de salvamento quando a soma dos 8 descontos ultrapassa 100%, com alerta —
      decisão do usuário (H4)
- [x] Soma dos descontos extraída pro método reutilizável `somaDescontos()` (usado em
      `calcLiquido` e na validação de `salvar`) — sem duplicação
- [x] `testes-pesagem.md` atualizado (H4) — `./gradlew test`: **BUILD SUCCESSFUL**

## Concluído (fluxo G — bruto<tara, salvar sem peso — 2026-09-01)
- [x] Bloqueio de salvamento quando Peso bruto < Tara (líquido negativo), com alerta —
      decisão do usuário (G4/B5)
- [x] Aviso de confirmação antes de salvar pesagem sem nenhum peso (F2/G6) — vale pras 4 telas
- [x] Cálculo do líquido extraído pro método reutilizável `calcLiquido()` (sem duplicação)
- [x] G3 (colar `8500.5` → `85.005`) **mantido por decisão do usuário** — aplicação de balança,
      valor é digitado/capturado com vírgula, não colado
- [x] `testes-pesagem.md` atualizado (G3/G4/G5) — `./gradlew test`: **BUILD SUCCESSFUL**

## Concluído (fix — líquido negativo + Saída sem Peso bruto — 2026-09-01)
- [x] `PesagemFormViewModel.recalcularPesoLiquido()`: retorna vazio quando `pesoTotal` está
      vazio (antes dava `0 − tara` = negativo) — cenário C1
- [x] `PesagemSaidaViewModel.preencherDaEntrada()`: agora copia `pesoTotal` da Entrada além de
      `pesoVeiculo` — cenário D2
- [x] `testes-pesagem.md` atualizado com os resultados (C1 e D2 marcados como ok)

## Concluído (fix — NPE em UsuarioService ao salvar sem telefone — 2026-09-01)
- [x] `UsuarioService.salvar()`/`atualizar()`: null-check em `getTelefone()` antes de chamar
      `isEmpty()` — causava NPE ao salvar/editar usuário sem telefone (campo opcional)
- [x] Corrigiu 9 testes que falhavam com NPE (`UsuarioServiceTest` + `PesagemServiceTest`) —
      todos eram o mesmo bug real, não problema de infraestrutura de teste
- [x] `./gradlew test` → **199 testes, BUILD SUCCESSFUL** (0 falhas)

## Concluído (só a Placa é obrigatória na pesagem — 2026-08-31)
- [x] `PesagemService.validarCampos` passou a exigir somente `placa` (Motorista e Cliente
      deixaram de ser obrigatórios)
- [x] Formulário (`PesagemFormScreen`): removido o `*` de "Nome do motorista" e "Cliente";
      Placa segue obrigatória (`*`)
- [x] Migration `V18` torna `motorista_nome` e `cliente_id` nullable (recria a tabela como no
      `V16`, preservando dados/FKs e `entrada_id`/`usuario_id`)
- [x] Testes: `deveLancarExcecaoQuandoMotoristaVazio`/`...ClienteNaoInformado` viraram
      `motoristaEhOpcional`/`clienteEhOpcional` — `./gradlew test`: **196 testes,
      BUILD SUCCESSFUL**

## Concluído (relatório resumido no layout monoespaçado do André + negritos — 2026-08-31)
- [x] `RelatorioPesagemPdfExporter` reescrito: relatório vira texto **monoespaçado (Courier)**
      igual ao do André — cabeçalho da empresa (nome/Cpf/Insc.e/End/Bairro/Cidade/Fone), linha de
      `_`, título centralizado, separadores de `=`, colunas, "Observação:" **por linha** (a
      observação da pesagem, `---` se vazia) e linha de totais
- [x] **Em negrito** (Courier-Bold, mesma largura de glifo do regular — não desalinha): o título
      "Relatório resumo de entradas e saídas", os nomes das colunas, e os rótulos "Observação",
      "Quantidade total entradas" e "Total peso liquido"
- [x] Coluna **Tara (Kg) mantida** (decisão do usuário; o relatório real do André não a tem),
      colunas alinhadas por largura calculada em caracteres; fonte auto-dimensionada pra caber na
      A4
- [x] `PesagemHistoricoScreen.exportPdf`: agora passa observação por linha e os totais
      (quantidade + soma do peso líquido); `RelatorioPesagemPdfExporterTest` reescrito (3 casos)
      — `./gradlew test`: **196 testes, BUILD SUCCESSFUL**

## Concluído (ticket de pesagem em impressora térmica 80mm — 2026-08-31)
- [x] Novo `TicketThermalExporter` (`my_app/infra`) imprime o ticket numa térmica 80mm via
      ESC/POS (`escpos-coffee`, já no build), no mesmo layout de campo do ticket do André:
      cabeçalho da empresa (Cnpj/Insc.est/End/Bairro/Cidade/Fone), "TICKET DE PESAGEM",
      Ticket nº, Placa do Veículo, DT/H Entrada/Saída (`dd/MM/yyyy HH:mm:ss`),
      Operador/Motorista/Produto/Fornecedor/Cliente, Peso de Entrada/Saída/Líquido,
      Peso Líquido Final, Observação e assinaturas `ADMINISTRADOR`/`MOTORISTA` — bobina 80mm
- [x] Envia pra **impressora padrão do sistema** (`PrinterOutputStream.getDefaultPrintService()`)
      — sem tela de configuração/porta/IP (confirmado com o usuário)
- [x] Novo botão **"Imprimir térmica"** no modal de detalhes do histórico
      (`PesagemHistoricoScreen`) ao lado de "Imprimir ticket" (PDF, que segue intacto);
      `PesagemHistoricoViewModel.imprimirTicketTermica` reusa `buscarComRelacoes` +
      `buscarEntradaVinculada`
- [x] `TicketThermalExporterTest` novo (2 casos: layout completo + sem empresa/entrada/relações) —
      `./gradlew test`: **196 testes, BUILD SUCCESSFUL**

## Concluído (ticket de pesagem no layout do André + operador — 2026-08-31)
- [x] `TicketPdfExporter` reescrito reproduzindo o ticket do André (texto monoespaçado):
      cabeçalho da empresa (Cnpj/Insc.est/End/Bairro/Cidade/Fone), "Ticket de Pesagem Nº",
      Placa/Uf, Data/Hora de entrada e saída, Operador/Motorista/Produto/Fornecedor/Cliente,
      Peso entrada/saída/líquido, Observação e assinaturas; **2 vias na MESMA folha** (1
      página), com linha de separação entre elas e **linha de assinatura acima de cada nome**
- [x] Novo campo `pesagens.usuario_id` (migration `V17`) — guarda quem criou a pesagem,
      preenchido no salvamento via `SessaoUsuario` (`PesagemFormViewModel.montarModel`);
      relação `usuario` anexada por `PesagemService.anexarRelacoes`; ticket exibe o Operador
- [x] `PesagemHistoricoViewModel.imprimirTicket` carrega a entrada vinculada
      (`PesagemService.buscarEntradaVinculada`) pra preencher Data/Hora/Peso de entrada
- [x] Fornecedor exibido vazio (modelo não tem esse dado); Cliente vazio quando sem valor,
      Motorista/Produto seguem com `---` (como no André)
- [x] `TicketPdfExporterTest` reescrito (2 páginas/vias, campos, sem empresa/entrada/relações) +
      `PesagemServiceTest` com 4 casos novos — `./gradlew test`: **194 testes, BUILD SUCCESSFUL**
- [x] Completada a remoção (já staged) da `InfoUpdateScreen`: removidas as 2 referências mortas
      em `AppRoutes.java` (import + enum `INFO_UPDATE`) que quebravam a compilação

## Concluído (produto opcional nas pesagens + `*` nos campos obrigatórios — 2026-08-31)
- [x] Produto deixou de ser obrigatório nas pesagens: removida a validação "Produto é
      obrigatório" de `PesagemService.validarCampos()`; coluna `produto_id` ficou nullable
      (migration `V16`, recria a tabela preservando dados e FKs) — teste ajustado
      (`produtoEhOpcional` em vez de `deveLancarExcecaoQuandoProdutoNaoInformado`)
- [x] `*` aplicado aos campos obrigatórios em todas as telas — novo helper
      `Components.obrigatorio(label)` (`label *`):
      - Pesagem: Placa*, Nome do motorista*, Cliente* (Produto segue sem `*` — agora opcional)
      - Login: E-mail*, Senha*
      - Cliente: Loja*, Razão social*
      - Produto: Nome do produto*
      - Usuário: Nome*, Login*, Senha*
      - Empresa: Nome*
      - Conexão da balança: Tipo de conexão* + (Porta COM*, Baud rate* / Endereço IP*, Porta*
        conforme o tipo selecionado)
- [x] `./gradlew test --rerun-tasks`: **BUILD SUCCESSFUL** (sem regressão)

## Concluído (relatório do histórico — formato do André + Tara + rodapé — 2026-08-31)
- [x] Relatório reproduz o do André (1 linha por par Entrada+Saída da mesma placa) em colunas:
      Ticket | Tara (Kg) | Entrada | Horário | Saída | Horário | Placa | Produto | Cliente |
      Peso bruto | Peso líquido (Fornecedor removida conforme pedido)
- [x] Entrada/Saída = data (dd/MM/yyyy); Horário = hora (HH:mm:ss); Tara = peso de veículo da
      entrada; bruto/líquido do registro consolidado do par; eventos únicos entram como linha
      própria na coluna Entrada
- [x] Rodapé igual ao do André: observação, "Quantidade total entradas: N", "Total peso líquido: X"
- [x] `RelatorioPesagemPdfExporter`: overload com `List<String> rodape` + quebra de página
- [x] `RelatorioPesagemPdfExporterTest` novo (3 casos) — `./gradlew test`: **192 testes,
      BUILD SUCCESSFUL**

## Concluído (relatório do histórico agrupa Entrada+Saída — 2026-08-31)
- [x] Colunas do relatório = exatamente "Ticket, Entrada, Horário, Saída, Horário, Placa,
      Produto, Cliente, Peso bruto, Peso líquido" (Fornecedor descartado conforme pedido)
- [x] Agrupamento Entrada+Saída da mesma placa/visita numa linha só — `PesagemHistoricoScreen.
      exportPdf`; avulsas/manuais e saídas sem a entrada no snapshot entram como linha própria
- [x] Novo campo `pesagens.entrada_id` (migration `V15`) preenchido na pesagem de saída com o id
      da entrada que a originou — hook `PesagemFormViewModel.aoMontarModel` + `PesagemSaidaViewModel`
- [x] `PesagemServiceTest.entradaIdDaSaidaEhPersistidoERelido` novo — `./gradlew test`:
      **189 testes, BUILD SUCCESSFUL**

## Concluído (eventos por entidade — 2026-08-28)
- [x] `EntityEvent<T>` virou classe abstrata genérica; criados os eventos concretos
      `ClienteEvent`, `ProdutoEvent`, `PesagemEvent`, `UsuarioEvent` (fábricas criado/editado/
      excluido, sem `EventType`) — listeners casam no tipo concreto em vez de
      `instanceof EntityEvent<?> && entity() instanceof X`; os acessors `entity()`, `type()`,
      `is()` e `entityId()` eram dead code e foram removidos (classe virou marcador puro)
- [x] ViewModels atualizadas: `ClienteViewModel`/`ProdutoScreenViewModel`/`PesagemHistoricoViewModel`
      (listener do próprio evento), `PesagemFormViewModel` (escuta `ClienteEvent`/`ProdutoEvent`,
      publica `PesagemEvent`), `UsuarioScreenViewModel` (publica `UsuarioEvent`)
- [x] `./gradlew test`: **188 testes, BUILD SUCCESSFUL**

## Concluído (bug — NPE session nula ao salvar pesagem de saída — 2026-08-28)
- [x] `EventBus.unsubscribe` novo + as 4 ViewModels que se inscreviam (histórico, formulário de
      pesagem, Cliente, Produto) agora se desinscrevem no `onDestroy`, antes de fechar o Service
      — antes, uma ViewModel já destruída continuava reagindo ao evento com a `Session` nula e
      estourava NPE (ver `DECISIONS.md`)
- [x] `EventBusTest` novo — `./gradlew test`: **188 testes, BUILD SUCCESSFUL**

## Concluído (pesagem — formatação de campos do formulário — 2026-08-28)
- [x] `Components.InputRgCpf` novo (RG/CPF combinado com máscara dinâmica) — usado no
      "Documento do motorista"; formatação em `Utils.formatRgCpf`
- [x] Pesos (tara/bruto) formatados em decimal via `InputColumnDecimal` /
      `InputWithButtonRowDecimal` novo (mantém o botão "Capturar"); peso líquido continua
      só-leitura
- [x] `UtilsTest`: 4 casos novos de `formatRgCpf` — `./gradlew test`: **186 testes,
      BUILD SUCCESSFUL**

## Concluído (pesagem — peso líquido dinâmico — 2026-08-28)
- [x] "Peso líquido" recalculado dinamicamente a cada mudança de bruto/tara/desconto —
      `PesagemFormViewModel` se inscreve nos states e reaproveita `PesagemCalculo`; botão
      "Calcular" removido e campo virou só-leitura (vale pras 4 telas de formulário)
- [x] `./gradlew test`: **182 testes, BUILD SUCCESSFUL**

## Concluído (pesagem — formulários separados por tipo + `tipo_pesagem` — 2026-08-28)
- [x] `tipo_pesagem` (valores `entrada`/`saida`/`avulsa`/`manual`) no lugar de `operacao` —
      migration `V14`; o tipo passou a ser decidido pela tela aberta, não mais por paridade de
      placa
- [x] `PesagemService.salvar()` exige `tipoPesagem` (não infere); removido `determinarOperacao`;
      `buscarTaraSugerida` agora vem da **última entrada** da placa (`buscarUltimaEntrada`) —
      novo `PesagemRepository.buscarPorPlacaETipo`
- [x] Telas de formulário por tipo, sem `ContratoTelaCrudV3` (fluxo próprio):
      `PesagemEntrada/Saida/Avulsa/Manual{Screen,ViewModel}` sobre a base `PesagemForm{Screen,
      ViewModel}` — semântica por tipo (ver `DECISIONS.md`)
- [x] `PesagemHistorico{Screen,ViewModel}` — tela única de histórico usando `ContratoTelaCrudV3`
      (lista, filtro, excluir, baixar lista, imprimir ticket no modal de detalhes)
- [x] Navegação: `Secao.PESAGEM_HISTORICO` + botão "Histórico de pesagens" na sidebar; rota/enum
      `PESAGENS` removidos de `AppRoutes`; `PesagemScreen`/`PesagemViewModel` antigos deletados
- [x] `TicketPdfExporter` atualizado: "Tipo: ..." em vez de "Operação: ..."
- [x] Testes atualizados (`PesagemServiceTest`/`PesagemRepositoryTest`/`TicketPdfExporterTest`) —
      `./gradlew test`: **182 testes, BUILD SUCCESSFUL**

## Concluído (integração com câmera Intelbras — 2026-08-19)
- [x] Item de "Fase 2 (adiado)" resolvido — captura de foto automática na pesagem, integrando
      de verdade com as duas câmeras Intelbras (frente/costas) via HTTP CGI
      (`/cgi-bin/snapshot.cgi`, autenticação Digest — mesmo endpoint da Dahua, fabricante
      original das câmeras VIP)
- [x] `CameraSnapshotClient` — desafio/resposta Digest (RFC 2617) implementado na mão em cima de
      `java.net.http.HttpClient`, sem lib nova
- [x] `ConexaoCameraModel`/`Repository`/`Service` (migration `V13`, singleton) + tela "Conexão
      das câmeras" (menu Gerencial) com botão "Testar câmera" e prévia da foto capturada
- [x] Timing corrigido: foto capturada na hora da pesagem (`PesagemViewModel.
      capturarFotosAutomaticamente`, só ao criar, não ao editar), não na hora de imprimir o
      ticket — esse era o bug de timing do app original
- [x] Falha numa câmera (rede/autenticação/não configurada) não derruba o salvamento da pesagem
- [x] `CameraSnapshotClientTest` — servidor HTTP fake (`com.sun.net.httpserver`, já no JDK)
      exigindo o mesmo desafio Digest de uma câmera real; `ConexaoCameraServiceTest`/
      `RepositoryTest` seguem o padrão de `ConexaoBalanca`
- [x] `./gradlew test`: **181 testes, BUILD SUCCESSFUL**

## Concluído (ticket em PDF, tela de Logs, logging em toda a aplicação — 2026-08-19)
- [x] `TicketPdfExporter` (PDFBox) — requisito do projeto original que faltava nesta reescrita;
      botão "Imprimir ticket" no modal de detalhes da pesagem + "Salvar e baixar ticket" no
      formulário; PDF abre sozinho no visualizador padrão do sistema depois de salvo
- [x] Botão "Criar novo" flutuante (canto inferior direito) e teto de altura nas tabelas de
      listagem (350px) — pedidos de UX do usuário
- [x] `LogsScreen` (Suporte > "Ver logs da aplicação") — lê `~/.gobitech/logs/gobitech.log`
- [x] Logging adicionado em todos os pontos principais (app lifecycle, toda entidade
      salvar/atualizar/excluir, login/logout, leitores de balança) — praticamente tudo usava
      `e.printStackTrace()` ou engolia a exceção sem log nenhum antes disso
- [x] **Bug real encontrado e corrigido**: log de produção (`~/.gobitech/logs/gobitech.log`)
      tinha crescido pra 37,5 mil linhas / 4,6MB (14MB somando os arquivos rolados) — causa raiz
      era os testes não terem `logback.xml` próprio e escreverem no mesmo arquivo do app real
      (cada rodada de `./gradlew test` gerava ~17 mil linhas só de migration do Flyway). Corrigido
      com `src/test/resources/logback-test.xml`; log antigo limpo; `LogsScreenViewModel` também
      limitado a mostrar só as últimas 500 linhas (TextArea do JavaFX não é virtualizado)
- [x] `megalodonte-components` estendido (`fontFamily`/`editable`/`fillHeight` em `InputProps`,
      `maxHeight` em `SimpleTableProps`, `childInCorner`/`fillHeight` em `Stack`) — telas
      passaram a usar só a API do framework em vez de castar pro node JavaFX cru; republicado em
      `mavenLocal`
- [x] `./gradlew test`: **167 testes, BUILD SUCCESSFUL**

## Concluído (desconto do produto + fix de edição não refletindo — 2026-08-18)
- [x] `ProdutoModel.desconto` (novo campo, previsto no DER original) — migration `V12`,
      formulário de Produto, tabela, modal de detalhes
- [x] `PesagemViewModel`: seleção de produto carrega o desconto padrão dele no campo "Outros"
- [x] Bug real corrigido no framework (`megalodonte-reactivity`): `ListState.set()` comparava
      listas por conteúdo em vez de identidade, cancelando notificação quando `updateIf()` mutava
      e devolvia a mesma referência — edição de Cliente/Produto/Usuário/Pesagem não refletia na
      lista até trocar de seção. Ver `DECISIONS.md`.
- [x] `./gradlew test`: **155/155, BUILD SUCCESSFUL**

## Concluído (login/senha sempre criptografados — 2026-08-18)
- [x] Bug real corrigido: `UsuarioService.autenticar()` tentava decriptar texto puro (nunca
      funcionava — 4/12 testes de `UsuarioServiceTest` já falhavam antes desse fix)
- [x] `salvar()`/`atualizar()`/`autenticar()`/`listarAtivos()`/`buscarPorLogin()`: login/senha
      sempre criptografados em repouso (AES/ECB via `CryptoManager`), texto puro só na fronteira
      com as telas
- [x] Admin padrão (`V10__dados_padrao.sql`) trocado pros valores fornecidos pelo usuário
      (login/senha já cifrados) — credenciais reais não documentadas aqui, só o admin sabe
- [x] Banco local (`~/.gobitech/erp.db`) apagado e recriado do zero — autorizado pelo usuário
- [x] `./gradlew test`: **155/155, BUILD SUCCESSFUL** (inclusive os 4 que antes falhavam) + teste
      descartável de ponta a ponta confirmando o admin autentica e o banco guarda só o cifrado.
      Ver `DECISIONS.md`.

## Concluído (fix do restart falso no dev.py — 2026-08-18)
- [x] `dev.py`: `known_hashes` nunca era pré-populado — primeiro touch de metadado em qualquer
      arquivo (sem mudança de conteúdo) disparava restart falso. Nova `seed_known_hashes()`
      chamada antes do `observer.start()`. Testado rodando o script de verdade: touch puro não
      restarta mais, mudança de conteúdo real continua restartando normalmente. Ver `DECISIONS.md`.

## Concluído (migração pra `Scope` do framework — 2026-08-18)
- [x] `megalodonte-base`/`megalodonte-router` republicados em `mavenLocal` com
      `megalodonte.base.async.Scope` (cancelamento vinculado ao ciclo de vida) +
      `Router` (v4) cancelando automaticamente o `Scope` de cada tela em `onDestroy()`
- [x] `PesagemViewModel.iniciarLeituraBalanca()` migrado da flag `destruido` manual pra
      `ctx2.scope()` — ver `DECISIONS.md`
- [x] `./gradlew test`: **155 testes, BUILD SUCCESSFUL** (sem regressão)

## Concluído (vazamento de conexão da balança — 2026-08-18)
- [x] Investigado relato de travamento do computador com `dev.py` rodando por muito tempo —
      descartada a hipótese de processo Java órfão no restart do `dev.py` (testado empiricamente,
      o Gradle Daemon mata o processo antigo corretamente)
- [x] Encontrado e corrigido bug real por leitura de código: corrida entre
      `iniciarLeituraBalanca()` (async) e `onDestroy()`/`pararLeituraBalanca()` (sync) podia
      deixar a conexão Serial/TCP aberta pra sempre + uma thread presa segurando a `ViewModel`
      inteira viva, se o usuário saísse da tela de Pesagem antes da conexão terminar de abrir —
      ver `DECISIONS.md`
- [x] `./gradlew test`: **155 testes, BUILD SUCCESSFUL** (sem regressão)

## Concluído (leitura de peso via serial/TCP — 2026-08-17)
- [x] `my_app/infra/balanca/`: `LeitorBalanca` (interface) + `LeitorBalancaSerial` (JSSC) +
      `LeitorBalancaTcp` (socket cru, sem Telnet) + `LeitorBalancaFactory` (decide qual usar a
      partir da `ConexaoBalancaModel` salva)
- [x] `PesoParser` — parsing de peso que preserva separador decimal (corrige o bug do app
      antigo) e trata formato BR/US de milhar
- [x] `PesagemScreen`: peso ao vivo + botões "Capturar" (Tara/Peso Bruto) e "Calcular" (Peso
      Líquido, com a fórmula real — bruto menos tara, descontado o somatório dos 8 tipos de
      desconto — que não existia antes nesta reescrita)
- [x] `PesoParserTest` (10 casos) — `./gradlew test`: **149 testes, BUILD SUCCESSFUL**
- [ ] **Não testado contra hardware real** — só a lógica de parsing tem teste automatizado; a
      conexão serial/TCP em si precisa ser validada contra o equipamento físico ou simulada (ver
      `README.md` do projeto antigo)

## Concluído (correção em produção — 2026-08-17)
- [x] `V11__fix_dataCriacao_timestamp.sql` — bancos já criados antes da correção `REAL`→
      `TIMESTAMP` (evidência no `DECISIONS.md`) davam `Illegal Argument occurred setting
      property: dataCriacao` ao abrir o app de verdade. Nova migration corrige qualquer banco
      existente, dado preservado. Validado contra uma cópia do banco real antes de confiar que
      funciona.

## Concluído (Fase 1 — migrations, dados, telas — 2026-08-17)
- [x] Migrations do domínio de pesagem criadas (`V1` a `V10`): usuários, preferências, licenças,
      empresa, clientes, produtos, descontos, pesagens, conexão da balança + dados padrão
- [x] Migrations antigas do `plics-sw` (varejo) removidas
- [x] Models/Repositories/Services adaptados: `Cliente`, `Produto`, `Empresa`, `Preferencias`
- [x] Models/Repositories/Services novos: `Usuario`, `Licensa`, `Desconto`, `Pesagem`,
      `ConexaoBalanca`
- [x] Models/Repositories/Services de varejo removidos (Categoria, Compra, ContaAReceber,
      ContasPagar, Cor, Fornecedor, OrdemServico, Pedido/PedidoItem, Tecnico, Venda)
- [x] `PesagemRepository.filtrar()`: filtro por placa/motorista/cliente/produto/período com
      AND corretamente agrupado (o app antigo tinha bug de precedência AND/OR aqui)
- [x] Screens/ViewModels adaptadas: `ClienteScreen`, `ProdutoScreen`, `CadastroEmpresaScreen`,
      `PreferenciasScreen`, `AuthScreen` (login por usuário real + checagem de licença via
      `LicensaService`), `HomeScreen` (menu simplificado, sem dashboard financeiro)
- [x] Screens/ViewModels novas: `UsuarioScreen`, `ConexaoBalancaScreen`, `PesagemScreen` (com
      filtro, descontos, 4 fotos via seleção manual de arquivo)
- [x] Telas de varejo removidas (Categoria, Compras, ContasAPagar/AReceber, Fornecedor,
      OrdemServico, PDV, Pedidos, Relatorios, Tecnico, Venda) + serviços de nível superior que só
      existiam pra sustentar elas (`RelatorioService`, `PDVService`, `EscPosPrinter`, etc.)
- [x] `AppRoutes`/`InitialRouteResolver`/`Main.java` atualizados pro novo conjunto de telas e pro
      login obrigatório por usuário
- [x] `./gradlew compileJava`: **BUILD SUCCESS, 0 erros**
- [x] Migrations validadas rodando de ponta a ponta num SQLite limpo

## Concluído (licenciamento — 2026-08-17)
- [x] Modelo de licença confirmado com o Guilherme: self-service local pelo André (login de
      admin, sem API/backend) — ver `DECISIONS.md`
- [x] `LicensaScreen`/`LicensaViewModel` — gerar licença com validade opcional, listar licenças
      já geradas
- [x] `SessaoUsuario` (novo) — guarda o usuário logado na sessão, `isAdmin()` usado pra
      esconder/bloquear a tela de licença
- [x] Item de menu "Gerar licença" só aparece pra quem está logado como admin
- [x] `AuthScreenViewModel`: admin sempre consegue logar mesmo com licença expirada/ausente
      (senão ficaria trancado pra fora); não-admin é bloqueado se a licença mais recente expirou
- [x] `preferencias` simplificada: removidos `credenciais_habilitadas`/`login`/`senha`/`licensa`
      (órfãos desde que login virou por usuário real) — ver `DECISIONS.md`

## Concluído (testes automatizados + limpeza de preferências — 2026-08-17)
- [x] Removido controle de tema — nunca esteve ligado a nada real (`ThemeManager.setTheme` é
      fixo em `Main.java`). `preferencias` ficou só com `primeiro_acesso`. `PreferenciasScreen`
      virou só "Encerrar sessão".
- [x] Testes obsoletos removidos (Categoria/Compras/ContasPagar-AReceber/Fornecedor/
      OrdemServico/Pedido/Tecnico/Venda, PDV, Relatórios, `UpdaterServiceTest` — serviço não
      existe nesta cópia, `CleanDbRunner` — testava feature que não existe mais)
- [x] `BaseServiceTest.limparDadosPadrao()` corrigido pro schema novo
- [x] Testes atualizados: `Cliente`, `Produto`, `Preferencias` (campos novos/removidos);
      `Empresa` não precisou mudar
- [x] Testes novos: `Usuario`, `Licensa`, `Desconto`, `Pesagem` (com Cliente/Produto de apoio,
      cobrindo o filtro AND corrigido), `ConexaoBalanca` — Repository + Service de cada um
- [x] **`./gradlew test`: 141 testes, BUILD SUCCESSFUL**
- [x] **Bug real encontrado rodando os testes** (não só um problema de teste): migrations usavam
      `dataCriacao REAL`, que quebrava `atualizar()`/`buscarById()`/`listar()` em produção
      também, não só em teste — corrigido pra `TIMESTAMP` em todas as 9 tabelas. Ver
      `DECISIONS.md` pro relato completo (inclusive a correção de uma explicação errada que eu
      mesmo tinha documentado antes de rodar os testes de verdade).

## Resolvido — branding/empacotamento (herdado do plics-sw)
- [x] `Main.APP_NAME`/`BASE_TITLE` já dizem "Gobitech" (feito em rodada anterior); o que sobrava
      eram as chaves `plics.*` — corrigido em 2026-09-07 (M10): `Main.java` lê
      `gobitech.appVersion`, `build.gradle.kts`/`scripts/config.py` passam `-Dgobitech.appVersion`,
      `ProcessKiller` usa `gobitech-killer.log`, `create-flatpak.py` usa `APP_ID ...gobitech`.
- [x] `gradle.properties` usa `appName`/`appDisplayName` = gobitech; scripts MSI/DEB/Flatpak
      leem tudo de `config.py`/`gradle.properties`. Scripts "with-updater" (que apontavam pra
      `my_app.updater.Main` inexistente) removidos em 2026-09-07 (A10).
- [x] "Buscar atualização" removido da Home porque `my_app.infra.UpdaterService` não existe —
      se for reintroduzir, é trabalho de infraestrutura de release, não de tela.

