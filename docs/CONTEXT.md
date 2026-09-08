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
menu "Conexão das câmeras" está **comentado** em `HomeScreen.java:74`, deixando a tela inalcançável
pela UI (pendência M3 da vistoria). Ver `/home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/ENTREGAS.md`.

## Estado atual (2026-09-08)
- **M9 corrigido** (Vistoria pendente): validações de telefone/CEP/CPF/CNPJ centralizadas em
  `my_app/utils/Validacoes.java` (`validarTelefone`/`validarCep`/`validarCpfCnpj` — nulo/branco
  passa, formato inválido lança `IllegalArgumentException` com mensagem padronizada), usado
  pelos 3 Services (`ClienteService`, `EmpresaService`, `UsuarioService`). Cliente segue **sem**
  validação de formato de CPF/CNPJ (só unicidade); `UsuarioService` padronizou `isBlank()`.
  `ValidacoesTest` novo (11 casos). Ver `DECISIONS.md` 2026-09-08. Testes: `./gradlew test` →
  **BUILD SUCCESSFUL**.

## Estado atual (2026-09-08)
- **M7 corrigido** (Vistoria pendente): telas/ViewModels deixaram de exibir `e.getMessage()` cru
  (SQL/SO) pro usuário. Regra: mensagem amigável fixa no alerta + detalhe técnico só em
  `log.error`; `catch (IllegalArgumentException)` mantém o `getMessage()` (validação de domínio
  ex.: "Placa é obrigatória"). Coberto: Auth, Dashboard, Conexão balança/câmera, Pesagem
  (form + histórico), Logs, Licença, Empresa, CRUDs (Cliente/Produto/Usuário + Add/Edit +
  Details*), exportação de lista (`ContratoTelaCrudV3`) e os `onErro` dos leitores Serial/TCP.
  Ver `DECISIONS.md` 2026-09-08. Testes: `./gradlew test` → **BUILD SUCCESSFUL**.

## Estado atual (2026-09-08)
- **M20 corrigido** (Vistoria pendente): fim do **N+1** em `PesagemService.anexarRelacoes` —
  `BaseRepository.buscarPorIds` (`WHERE id IN`) + `anexarRelacoes(List)` com 4 lotes no lugar de
  N×4 SELECTs. Dashboard passou a usar **`count()`/`contarPorPeriodo`** (`SELECT COUNT(*)`) em
  vez de listar tudo pra `size()`. Detalhe: `COUNT(*)` via Persism vem como `Integer` no scalar.
  Ver `DECISIONS.md` 2026-09-08. Testes: `./gradlew test` → **BUILD SUCCESSFUL** (+7).

## Estado atual (2026-09-07)
- **Vistoria completa** concluída (auditoria *read-only*): **40 pendências registradas em
  `docs/TODO.md`** (11 alta, 21 média, 8 baixa). Decisões e correções desta rodada em
  `docs/DECISIONS.md`.
- **Corrigido nesta rodada (ver TODO.md e DECISIONS.md)**:
  - A5 — filtro por data: descoberta empírica de que o Persism/sqlite-jdbc grava `dataCriacao`
    como **INTEGER epoch-ms** (não texto, como a vistoria supunha — confirmado no driver e numa
    cópia do banco real). Filtro agora converte `LocalDate` (inclusivos) pra epoch-ms e compara
    numericamente; "pesagens do mês" do dashboard e o filtro do histórico voltam a funcionar.
    Migration `V20` (converter seeds em texto) criada nesta rodada foi **removida** — a premissa
    estava errada.
  - A6 — `onDestroy()` implementado nas 3 telas Add/Edit (Cliente/Produto/Usuário).
  - A7 — `parseLong` com try/catch nas 6 telas (3 Add/Edit + 3 Details).
  - A8 — `PesagemService.salvar/atualizar` validam `tipoPesagem` e bruto<tara (só com os dois
    pesos preenchidos); `DescontoService` valida soma>100. Regras também na camada de serviço.
  - A10 — scripts "with-updater" + `updater_config.py` removidos (`my_app.updater.Main` não existe).
  - A11/M12 — README reescrito pro produto real (Gobitech pesagem) com pré-requisitos de ambiente.
  - M2 — menu "Logs" restrito a admin. M5 — `EmpresaViewModel.fetchData` com alerta/log. M8 —
    `atualizar()` valida igual a `salvar()`. M10 — branding `plics.*` → `gobitech.*`. M11 —
    JUnit unificado (BOM 5.13.1) e libs órfãs (`jna`, `jackson`) removidas. M21 — `-Dprism.verbose`
    só em DEV_MODE. B6/B7 — PDFs gerados ignorados e removidos do índice; resíduo do gitignore
    removido. B8 — Saída sem botão "Capturar" na tara (tara somente-leitura da Entrada).
  - **Segunda rodada (mesma data) — foco em pendências médias/baixas** (ver TODO.md):
    M4 — exportPdf usa o `snapshotFiltrado`. M6 — trava anti-duplo-clique na classe base das
    ViewModels (`tryBeginSalvar`/`endSalvar`). M16 — `DevicesTest` removido. M17 — `LeitorBalancaTcpTest`
    novo (ServerSocket em loopback). M18 — testes desfragilizados (sem `Thread.sleep`, porta
    efêmera, banco em memória por classe). M19 — `HOTRELOAD.md` reescrito. B1 — `Parcela.java`
    deletado + mains órfãos + `ACESSO_BLOQUEADO` removido. B2 — imports não usados removidos.
    B3 — `build.gradle.kts` limpo (`publishing`/`maven-publish` órfãos removidos; comentários
    órfãos apagados). B5 — trim no `ProdutoService` + catches silenciosos agora logam.
- **Decidido: manter** (decisão do usuário) — A1 (chave AES), A2 (token Telegram), A3 (seed
  admin), A4 (senhas câmera em texto puro), M1 (senha sem máscara na edição) e M3 (menu de
  câmera continua desativado — só na Fase 2).
- **Pendente desta vistoria ainda aberto**: A9 (testes de ViewModel), M7 (mensagens amigáveis),
  M9 (centralizar validações), M14/M15 (consolidar docs), M20 (N+1 + COUNT no dashboard),
  B4 (workflow do PU — outro repo). Detalhes em `docs/TODO.md`.
- Testes: `./gradlew test` → **BUILD SUCCESSFUL** (196 testes; +2 novos nesta rodada:
  `LeitorBalancaTcpTest`).

## Estado atual (2026-09-03)
- **Inputs desativados com borda vermelha**: os pesos somente-captura (Entrada/Saída) e a tara
  somente-leitura da Saída agora usam a mesma borda vermelha (`#e74c3c`) do "Peso líquido" —
  `Components.InputColumnInteger(..., disableInput=true)` aplica borda vermelha quando desativado
  (antes os desativados ficavam com a borda padrão). Ver `DECISIONS.md`.
- Testes: `./gradlew test` → **BUILD SUCCESSFUL**.

## Estado atual (2026-09-03)
- **Pesos da pesagem: captura x digitação por tipo** — `PesagemFormScreen` agora separa
  `permitirCapturar*` (mostra botão "Capturar") de `*Editavel()` (aceita digitação); quando há
  botão de captura o campo fica somente-leitura (borda vermelha), e novos overloads
  `InputColumnInteger`/`InputWithButtonRowInteger` com `disableInput`. Resultado: **Entrada** =
  tara+bruto captura-only; **Saída** = tara somente-leitura (vem da entrada) + bruto captura-only;
  **Avulsa** = tara digitada (sem botão) + bruto captura-only; **Manual** = tara+bruto digitados
  (sem botão). Reverte o Item 7 (botão "Capturar" na tara da avulsa). Ver `DECISIONS.md`.
- Testes: `./gradlew test` → **BUILD SUCCESSFUL**.

## Estado atual (2026-09-03)
- **Fix — Details/Busca de usuário exibiam o login hasheado**: `UsuarioService` ganhou
  override de `buscarById(long)` que decripta `login`/`senha` (texto puro na fronteira com as
  telas, igual `buscarPorLogin`/`listarAtivos`). Corrige `DetailsUsuarioScreen` (que mostrada o
  login criptografado) e `AddOrEditUsuarioScreen` (`populateFieldsFromModel`). Login/senha
  continuam sempre criptografados em repouso — ver `DECISIONS.md`.
- Testes: `./gradlew test` → **BUILD SUCCESSFUL** (novo
  `UsuarioServiceTest.deveRetornarLoginESenhaEmTextoPuroAoBuscarPorId`).

## Estado atual (2026-09-02)
- **Peso da balança em tempo real no dashboard** (HOME): o `DashboardViewModel` ganhou a mesma
  leitura contínua da balança do formulário de pesagem (`pesoAoVivo` + `lendoBalanca`, via
  `ConexaoBalancaService`/`LeitorBalancaFactory`). O `DashboardScreen` exibe "Peso da balança
  agora (Kg):" no topo e liga/desliga a leitura no `onMount`/`onDestroy`.
- Testes: `./gradlew test` → **BUILD SUCCESSFUL**.

## Estado atual (2026-09-02)
- **Utilitários movidos pro pacote `pack-utilities`** (dependência nova
  `com.github.eliezer-dev-software-enginner:pack-utilities:v1.0.0` → pacote `pack.utilities.*`):
  `Utils.java` foi enxugada pra **só `timestampParaArquivo()`**. Validação e formatação agora vêm
  de `ValidatorPack`/`FormatterPack`/`CurrencyPack` (ex.: `isValidDocumento`, `isValidCpfOrCnpj`,
  `isValidPhone`, `isValidCep`, `formatCpfCnpj`, `formatRgCpf`, `formatPhone`, `formatCep`,
  `formatCnpj`, `toBRLCurrency`, `deCentavosParaReal`). Cuidado: validadores de CPF/CNPJ do pacote
  são **mais rigorosos** (dígito verificador real). `EmpresaService`, `PesagemService`,
  `UsuarioService`, `ClienteService`, `Components`, `Data`, `TotaisState`, `ListaPdfExporter`,
  `TicketPdfExporter`, `ClienteScreen` atualizados. `UtilsTest` reduzido ao teste de
  `timestampParaArquivo`. Ver `DECISIONS.md`.
- **`DateUtils` local removido**: os 9 métodos tinham equivalente 1:1 no `pack.utilities.DatePack`
  (mesmos formatos `dd/MM/yyyy` e `dd/MM/yyyy HH:mm` e tratamento de null/0).
  `Parcela`, `Components`, `ProdutoScreen`, `LicensaScreen`, `PesagemHistoricoViewModel`,
  `PesagemHistoricoScreen`, `ClienteScreen`, `UsuarioScreen`, `DashboardViewModel` migrados.
  `my_app/utils/DateUtils.java` deletado.
- Testes: `./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL**.

## Estado atual (2026-08-17)
- Migrations, Models, Repositories, Services e Screens/ViewModels das 9 entidades acima:
  **feitos e compilando** (`./gradlew compileJava` → BUILD SUCCESS, 0 erros).
- Roteamento (`AppRoutes`) e fluxo de login/primeiro acesso adaptados pro novo modelo
  (login por usuário real, não mais um login único compartilhado).
- **Leitura de peso via serial/TCP implementada** (`my_app/infra/balanca/`) — `PesagemScreen`
  mostra o peso ao vivo e tem botões "Capturar" (Tara/Bruto) e "Calcular" (Líquido). Não
  testado contra hardware real ainda (só a lógica de parsing tem teste automatizado).
- **Testes automatizados**: 149 testes (Repository + Service de cada uma das 9 entidades +
  `PesoParser`), `./gradlew test` → **BUILD SUCCESSFUL**. Rodar os testes revelou e corrigiu um
  bug real nas migrations (`dataCriacao REAL` quebrava qualquer releitura do banco — ver
  `DECISIONS.md`), inclusive num banco real já em uso (corrigido via `V11`, com auto-correção
  no próximo boot do app).

## Estado atual (2026-09-02)
- **Polimento de UX/pesagem** (9 itens do TODO):
  - Botão **"Copiar placa"** no modal de detalhes do histórico.
  - Correção **"Registrar registrar ..."** no botão de salvar (4 telas).
  - **Busca de placa insensível a maiúsc/minúsc** no banco (`UPPER(placa)=UPPER(?)`) e campo
    Placa agora é **uppercase** (`InputColumnUppercase`).
  - **Borda vermelha** em inputs não-editáveis (ex.: Peso líquido).
  - **Popup some sozinho** após ~3s.
  - Botão **"Capturar" Tara na Pesagem avulsa** (caminhão vazio na balança).
  - **Inscrição estadual** da empresa: novo campo (migration `V19`) + cadastro + exibida no
    cabeçalho de relatório/tickets "Insc.est:".
  - Download de **relatório** (`relatório - <data>.pdf`) e **ticket** (`ticket - <data>.pdf`)
    com data/hora no nome (`Utils.timestampParaArquivo`).
- Testes: `./gradlew test` → **BUILD SUCCESSFUL**.

## Estado atual (2026-09-02)
- **Fluxo J (campos textuais opcionais) — fixes**:
  - **J2**: documento do motorista agora é **validado se preenchido** — novo
    `Utils.isValidDocumento()` (RG 8-9 dígitos ou CPF 11) e `PesagemService.validarCampos()`
    lança `IllegalArgumentException` quando inválido. Vazio continua permitido.
  - **J4**: `PesagemService.validarCampos()` **lança `IllegalArgumentException`** quando
    "Nome do motorista" excede 100 caracteres.
  - As validações J2/J4 ficam na **Service** (segundo o padrão já existente), a ViewModel só
    exibe o `getMessage()` da `IllegalArgumentException`.
- Testes: `./gradlew test` → **BUILD SUCCESSFUL**.

## Estado atual (2026-09-01)
- **Fluxo H (descontos)**: **bloqueio** de salvamento quando a soma dos descontos ultrapassa
  100% — alerta "A soma dos descontos não pode ultrapassar 100."; soma extraída pro método
  reutilizável `somaDescontos()`.
- **Fluxo G (pesagem manual) — fixes**:
  - **G4**: **bloqueio** de salvamento quando Peso bruto < Tara (líquido negativo) — alerta
    "Peso bruto não pode ser menor que a Tara".
  - **F2/G6**: aviso de confirmação antes de salvar pesagem **sem nenhum peso**; Sim salva, Não
    cancela. Vale pras 4 telas (classe base `PesagemFormViewModel`).
  - **G3**: comportamento de colagem com ponto decimal (`8500.5` → `85.005`) **mantido por
    decisão do usuário** — aplicação de balança, valor é digitado/capturado (vírgula), não colado.
- Cálculo do líquido extraído pro método reutilizável `calcLiquido()` (sem duplicação entre a
  exibição dinâmica e a validação de salvamento).
- Testes: `./gradlew test` → **BUILD SUCCESSFUL**.

## Estado atual (2026-09-01)
- **Fix de dois bugs** encontrados em testes manuais (`testes-pesagem.md`, cenários C1 e D2):
  - **C1**: Peso líquido ficava negativo ao registrar Entrada só com Tara (sem Peso bruto) —
    `recalcularPesoLiquido()` agora retorna vazio quando bruto não informado.
  - **D2**: Saída não trazia o Peso bruto da Entrada ao buscar pela placa — `preencherDaEntrada()`
    agora copia `pesoTotal` além de `pesoVeiculo`.
- **Fix de bug real em `UsuarioService`**: `salvar()`/`atualizar()` faziam
  `model.getTelefone().isEmpty()` sem checar null — NPE ao salvar usuário sem telefone (campo
  opcional). Corrigido com null-check antes.
- Testes: `./gradlew test` → **199 testes, BUILD SUCCESSFUL** (0 falhas — os 9 NPEs pré-existentes
  em `UsuarioServiceTest`/`PesagemServiceTest` foram corrigidos).

## Estado atual (2026-08-31)
- **Só a Placa é obrigatória** na pesagem (pedido do André). Removida a exigência de Motorista e
  Cliente do `PesagemService.validarCampos`; formulários deixaram de marcar `*` em "Nome do
  motorista" e "Cliente" (Placa segue com `*`). Migration `V18` torna `motorista_nome` e
  `cliente_id` nullable (SQLite recria a tabela, preservando dados/FKs e as colunas
  `entrada_id`/`usuario_id` — mesmo padrão do `V16`). Produto e demais já eram opcionais. Ver
  `DECISIONS.md` e `TODO.md`.
- Testes: `./gradlew test` → **196 testes, BUILD SUCCESSFUL**.

## Estado atual (2026-08-31)
- **Relatório resumido de entradas e saídas** agora imita o visual do relatório do André:
  texto **monoespaçado (Courier)** com cabeçalho da empresa (nome/Cpf/Insc.e/End/Bairro/
  Cidade/Fone), linha de `_`, título centralizado, separadores de `=`, colunas, uma linha
  **"Observação:"** embaixo de cada ticket (a observação da própria pesagem, `---` se vazia) e
  linha de totais. Mantida a coluna **Tara (Kg)** (decisão do usuário). **Em negrito**
  (Courier-Bold, que tem a mesma largura de glifo do regular — não desalinha o texto): título,
  nomes das colunas, rótulos "Observação", "Quantidade total entradas" e "Total peso liquido".
  A largura da fonte é calculada pra linha mais larga caber na página. Ver `DECISIONS.md` e
  `TODO.md`.
- Testes: `./gradlew test` → **196 testes, BUILD SUCCESSFUL** (inclui `RelatorioPesagemPdfExporterTest`).

## Estado atual (2026-08-31)
- **Ticket de pesagem** agora reproduz o layout exato do ticket do André (texto monoespaçado,
  uma linha por campo, com cabeçalho da empresa, Ticket Nº, Placa/Uf, Data/Hora de entrada e
  saída, Operador/Motorista/Produto/Fornecedor/Cliente, Peso entrada/saída/líquido, Observação
  e assinaturas), com **2 vias na MESMA folha** (linha de separação entre elas + linha de
  assinatura acima de cada nome). Novo campo `pesagens.usuario_id` (migration `V17`)
  guarda quem criou a pesagem (Operador no ticket). `PesagemHistoricoViewModel.imprimirTicket`
  usa a entrada vinculada pra Data/Hora/Peso de entrada. Ver `DECISIONS.md` e `TODO.md`.
- Testes: `./gradlew test` → **194 testes, BUILD SUCCESSFUL**.
- Completada a remoção já iniciada da `InfoUpdateScreen` (2 refs mortas em `AppRoutes.java`).

## Estado atual (2026-08-31)
- **Produto opcional nas pesagens**: removida a validação "Produto é obrigatório" de
  `PesagemService.validarCampos()` e a coluna `pesagens.produto_id` ficou nullable (migration
  `V16` recria a tabela preservando dados/FKs). **`*` em todos os campos obrigatórios** das
  telas via novo helper `Components.obrigatorio(label)` (`label *`): Pesagem (Placa/Nome do
  motorista/Cliente), Login (E-mail/Senha), Cliente (Loja/Razão social), Produto (Nome), Usuário
  (Nome/Login/Senha), Empresa (Nome), Conexão da balança (Tipo + campos do tipo selecionado).
  Produto no formulário fica sem `*` (opcional). Ver `DECISIONS.md` e `TODO.md`.
- Testes: `./gradlew test` → **BUILD SUCCESSFUL** (sem regressão).

## Estado atual (2026-08-31)
- **Relatório do histórico de pesagens** reproduz o do André (1 linha por par Entrada+Saída da
  mesma placa) com a **tara adicionada** e rodapé de totais. Colunas: Ticket | Tara (Kg) |
  Entrada | Horário | Saída | Horário | Placa | Produto | Cliente | Peso bruto | Peso líquido.
  Entrada/Saída = data, Horário = hora (HH:mm:ss); bruto/líquido do registro consolidado. Rodapé:
  observação, total de entradas, total peso líquido. Para o agrupamento correto, novo campo
  `pesagens.entrada_id` (migration `V15`). Ver `DECISIONS.md` e `TODO.md`.
- Testes: `./gradlew test` → **192 testes, BUILD SUCCESSFUL** (inclui `RelatorioPesagemPdfExporterTest`).

## Estado atual (2026-08-31)
- **Ticket térmico 80mm (ESC/POS)** — além do PDF, o ticket da pesagem agora pode ser impresso
  direto numa **impressora térmica**. Novo `TicketThermalExporter` (`my_app/infra`) reusa o motor
  ESC/POS (`escpos-coffee`, dependência já existente) e envia pra **impressora padrão do
  sistema** (`PrinterOutputStream.getDefaultPrintService()`), sem configurar porta. Layout dos
  campos igual ao do André (Cnpj/Insc.est/End/Bairro/Cidade/Fone, Ticket, Placa, DT/H Entrada/
  Saída, Operador/Motorista/Produto/Fornecedor/Cliente, Peso de Entrada/Saída/Líquido,
  Peso Líquido Final, Observação + assinaturas ADMINISTRADOR/MOTORISTA), adaptado pra bobina.
  Novo botão **"Imprimir térmica"** no modal de detalhes do histórico, ao lado de "Imprimir
  ticket" (PDF). `PesagemHistoricoViewModel.imprimirTicketTermica`. Sem tela/porta: usa a
  impressora padrão do Windows. Ver `DECISIONS.md` e `TODO.md`.
- Testes: `./gradlew test` → **196 testes, BUILD SUCCESSFUL** (inclui `TicketThermalExporterTest`).

## Estado atual (2026-08-28)
- A **pesagem** deixou de ser uma tela única de CRUD (`PesagemScreen` removida). Viraram telas
  separadas por tipo — `PesagemEntrada`, `PesagemSaida`, `PesagemAvulsa`, `PesagemManual`
  (formulários independentes, **sem** `ContratoTelaCrudV3`) + `PesagemHistorico` (lista com
  `ContratoTelaCrudV3`). O campo `operacao` virou `tipo_pesagem` (`entrada`/`saida`/`avulsa`/
  `manual`), decidido pela tela aberta, não mais por paridade de placa. Migration `V14`. Ver
  `DECISIONS.md`.
- Testes: **182 testes**, `./gradlew test` → **BUILD SUCCESSFUL**.

## Modelo de licenciamento — confirmado com o Guilherme (2026-08-17)
O André vai poder gerar quantas licenças precisar, ele mesmo — sem API nem backend. Ele tem
login de admin próprio, entra em qualquer computador com o app instalado, e gera uma licença ali
(com data de validade), local. **Isso já está implementado**: `LicensaScreen` (menu "Gerencial →
Gerar licença", só aparece pra quem está logado como admin — `SessaoUsuario.isAdmin()`) chama
`LicensaService.gerarNova(expiraEm)`. Detalhe importante: admin sempre consegue logar, mesmo com
a licença atual expirada/ausente — senão ele ficaria trancado pra fora sem conseguir gerar uma
nova. Usuários não-admin são bloqueados se a licença mais recente estiver expirada.
