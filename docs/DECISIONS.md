# Decisões Arquiteturais

## 2026-08-31: Ticket de pesagem reproduz o layout do André + guarda o operador (usuario_id)

**Contexto:** o usuário trouxe o ticket real do André (app antigo) como referência e pediu pra
reproduzir o layout exato, incluindo as **duas cópias** (o ticket vem impresso 2 vezes seguidas).
O ticket do André é texto monoespaçado com cabeçalho da empresa (Cnpj/Insc.est/End/Bairro/
Cidade/Fone), "Ticket de Pesagem Nº", Placa/Uf, Data/Hora de entrada e saída, Operador/
Motorista/Produto/Fornecedor/Cliente, Peso entrada/saída/líquido, Observação e assinaturas.
O ticket atual exibia Tara/Bruto/Líquido + descontos e não tinha entrada/saída nem operador.

**Decisões:**
- **Layout replicado** (`TicketPdfExporter` reescrito): monoespaçado (fonte Courier), com
  Data Entrada/Hora e Data saida/Hora, Peso entrada/saída/líquido, observação e assinaturas —
  conforme o ticket do André. **Duas vias na MESMA folha** (uma página única): a linha longa
  `--…--` fica **entre** as duas vias como separação/corte (não pertence a nenhuma via), e há
  uma **linha de assinatura acima de cada nome** (operador à esquerda, motorista à direita)
  pra assinatura manual depois de imprimir. Separador usa `-` corrido em vez do caractere de
  box-drawing `─` (U+2500) — fora do WinAnsiEncoding das fontes padrão do PDFBox.
- **Fornecedor**: campo exibido vazio (o modelo de pesagem não tem esse dado e o usuário
  confirmou que não deve ser preenchido). **Cliente** também vira vazio quando não há valor
  (Motorista/Produto seguem com `---`, como no André).
- **Operador**: a pesagem **não guardava quem a criou**. Adicionado `pesagens.usuario_id`
  (migration `V17`), preenchido no salvamento com o usuário logado
  (`PesagemFormViewModel.montarModel` ← `SessaoUsuario.usuarioLogado()`), e relação `usuario`
  anexada por `PesagemService.anexarRelacoes`. Coluna opcional: pesagens antigas ficam `NULL`
  (exibem `---` no ticket). O usuário escolheu guardar o id (produz o operador real de cada
  pesagem) em vez de só usar o usuário logado na hora de imprimir.
- **Dados de entrada no ticket**: como o ticket mostra Data/Hora e Peso de entrada E de saída,
  `PesagemHistoricoViewModel.imprimirTicket` agora carrega a entrada vinculada
  (`PesagemService.buscarEntradaVinculada`) e passa ela pro `gerar(...)`. Para entrada/avulsa/
  manual (sem `entrada_id`) os campos de entrada ficam vazios.

**Testado:** `TicketPdfExporterTest` reescrito (2 vias na mesma folha = 1 página, linha de
separação entre elas, linha de assinatura acima dos nomes, campos do André, sem empresa/sem
entrada/sem relações), `PesagemServiceTest` ganhou 4 casos (persistência/leitura de
`usuario_id`, anexação do operador, `buscarEntradaVinculada` com e sem vínculo). `./gradlew
test` → **194 testes, BUILD SUCCESSFUL**.

> Nota: neste mesmo lote foi completada a remoção já iniciada (staged) da tela de verificação de
> atualização (`InfoUpdateScreen`), removendo as 2 referências mortas restantes em
> `AppRoutes.java` (import + enum `INFO_UPDATE`) que impediam a compilação.

---

## 2026-08-31: Produto opcional na pesagem + `*` em todos os campos obrigatórios

**Contexto:** o usuário pediu que o **produto** deixasse de ser obrigatório ao salvar uma
pesagem (o operador nem sempre sabe/preenche o produto na hora) e que todo campo obrigatório da
aplicação ganhasse um `*` visual no rótulo.

**Decisão (produto opcional):**
- Removida a única regra que tornava o produto obrigatório: "Produto é obrigatório" em
  `PesagemService.validarCampos()`. O formulário de pesagem (`PesagemFormScreen`) já não exigia o
  campo — só o Service barrava.
- Coluna `pesagens.produto_id` ficou **nullable**. SQLite não permite remover `NOT NULL` via
  `ALTER COLUMN`, então a migration `V16` recria a tabela `pesagens` preservando dados, FKs
  (`cliente_id`/`produto_id`/`desconto_id`), `tipo_pesagem` e `entrada_id` — mesmo padrão do `V11`.
- Teste `PesagemServiceTest.deveLancarExcecaoQuandoProdutoNaoInformado` virou `produtoEhOpcional`
  (salva com `produtoId` nulo e relê `null`).

**Decisão (`*` nos obrigatórios):**
- Novo helper `Components.obrigatorio(String)` que devolve `label + " *"`, aplicado nos rótulos
  dos campos obrigatórios em cada tela (Pesagem: Placa/Nome do motorista/Cliente; Login:
  E-mail/Senha; Cliente: Loja/Razão social; Produto: Nome; Usuário: Nome/Login/Senha; Empresa:
  Nome; Conexão da balança: Tipo + campos do tipo selecionado). **Produto** no formulário de
  pesagem **não** recebe `*` (consistente com a decisão acima). Câmeras e licença ficam sem `*`
  (nenhum campo de texto obrigatório nelas).

**Testado:** `./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL** (sem regressão).

---

## 2026-08-31: Relatório do histórico — formato do André + coluna Tara + rodapé de totais

**Contexto:** usuário trouxe o relatório real do André (app antigo) como referência exata e
pediu pra reproduzi-lo, **adicionando a tara**. O modelo atual guarda tara (`peso_veiculo`),
bruto (`peso_total`) e líquido (`peso_final`) na **mesma** pesagem (mantido — ver decisão
abaixo sobre `entrada_id`). O relatório antigo não tinha a tara; ela é a medida que faltava.

**Decisão (formato final do relatório):**
- Uma linha **por par Entrada+Saída** da mesma placa/visita (como o do André), agora com coluna
  `Tara (Kg)` adicionada:
  `Ticket | Tara (Kg) | Entrada | Horário | Saída | Horário | Placa | Produto | Cliente | Peso bruto | Peso líquido`
  (coluna Fornecedor segue **removida**, conforme pedido do usuário).
- **Entrada/Saída** exibem a **data** (`dd/MM/yyyy`); **Horário** exibe a **hora** (`HH:mm:ss`) —
  igual ao do André (antes eu havia interpretado como peso; a referência deixou claro que é data).
- **Tara (Kg)** = peso de veículo da entrada; **Peso bruto/líquido** = do registro consolidado do
  par (a saída, quando há par). Eventos únicos (avulsa/manual/saída órfã) entram como linha
  própria na coluna Entrada com seus próprios tara/bruto/líquido.
- **Rodapé**, igual ao do André: `Observação (todas as linhas): ...` (observações não-vazias dos
  registros, senão `----`), `Quantidade total entradas: N`, `Total peso líquido: X`.
- `RelatorioPesagemPdfExporter` ganhou overload com `List<String> rodape` impresso após a tabela
  (com quebra de página), preservando o "Gerado em ... - N registro(s)" de baixo.

**Testado:** `RelatorioPesagemPdfExporterTest` novo (3 casos: layout+rodapé, sem rodapé/linhas,
quebra de página com 100 linhas). `./gradlew test` → **192 testes, BUILD SUCCESSFUL**.

---

## 2026-08-31: Relatório do histórico agrupa Entrada+Saída — novo campo `pesagens.entrada_id`

**Contexto:** pedido do usuário — o relatório do histórico de pesagens deve ter exatamente as
colunas "Ticket, Entrada, Horário, Saída, Horário, Placa, Produto, Cliente, Peso bruto, Peso
líquido" (coluna "Fornecedor" descartada, ele confirmou). Cada linha deve agrupar **Entrada +
Saída da mesma placa/visita** numa linha só, e o usuário pediu pra revisar o modo de salvamento
pra permitir esse agrupamento correto — hoje não havia como vincular uma saída à sua entrada.

**Decisão (duas partes):**
- **Schema/salvamento:** novo campo opcional `pesagens.entrada_id` (migration `V15`) — a pesagem
  de **saída** referencia a **entrada** que a originou. `PesagemSaidaViewModel` guarda o id da
  entrada puxada (`buscarUltimaEntrada`) e, ao salvar, grava em `entradaId` via novo hook
  `PesagemFormViewModel.aoMontarModel(PesagemModel)` (vazio por padrão, sobrescrito pela Saída).
  Para entradas/avulsas/manuais fica `NULL`.
- **Relatório (`PesagemHistoricoScreen.exportPdf`):** colunas exatas pedidas. Uma linha por par:
  Entrada/Horário = peso bruto (`pesoTotal`) e hora da entrada; Saída/Horário = peso bruto e hora
  da saída; Placa/Produto/Cliente da entrada; Peso bruto/Peso líquido do registro consolidado
  (a saída, quando há par). Saídas sem a entrada no snapshot e pesagens avulsas/manuais entram
  como linha própria — nada some do relatório. São **10** colunas, alinhadas 1:1 aos valores.

**Testado:** novo `PesagemServiceTest.entradaIdDaSaidaEhPersistidoERelido` (grava e relê de volta).
`./gradlew test` → **189 testes, BUILD SUCCESSFUL**.

---

## 2026-08-28: Eventos por entidade — `EntityEvent` virou base abstrata e nasceram eventos concretos

**Contexto:** o `EntityEvent<T>` (um único `record` genérico) tornava o consumo verboso: todo
listener precisava de `if (event instanceof EntityEvent<?> ee && ee.entity() instanceof XxxModel)`.
Pedido: um evento por entidade, estendendo uma base.

**Decisão:**
- `EntityEvent<T>` virou **classe abstrata genérica mantida apenas como marcador** do tipo da
  entidade. Os acessors `entity()`, `type()`, `is()` e `entityId()` eram dead code (nenhum
  listener/teste os lia) e foram removidos; o enum `EventType` também saiu. A intenção
  (criado/editado/excluido) é comunicada pelo nome da fábrica estática de cada evento — quem se
  inscreve só casa no tipo concreto (`instanceof ClienteEvent`) e recarrega a lista.
- Eventos concretos por entidade, cada um com fábricas `criado/editado/excluido`:
  `ClienteEvent`, `ProdutoEvent`, `PesagemEvent`, `UsuarioEvent`.
- Listeners deixaram de fazer pattern-match genérico: agora `if (event instanceof ClienteEvent)`
  etc. (`ClienteViewModel`, `ProdutoScreenViewModel`, `PesagemHistoricoViewModel` —
  `PesagemEvent`). O formulário de pesagem reage a `ClienteEvent`/`ProdutoEvent` (pra recarregar
  os selects) e publica `PesagemEvent`. Cruds publicam o evento da própria entidade.
- Mesmo padrão de ciclo de vida do fix anterior: cada listener continua se desinscrevendo no
  `onDestroy`.
- **Testado:** `EventBusTest` segue cobrindo subscribe/unsubscribe (os eventos continuam
  circulando como `Object`). `./gradlew test` → **188 testes, BUILD SUCCESSFUL**.

---

## 2026-08-28: Bug — error ao salvar pesagem de saída (NPE de session nula no histórico)

**Contexto:** ao salvar uma pesagem de saída, o app estourava
`NullPointerException ... BaseRepository.session() is null` dentro de
`PesagemHistoricoViewModel.fetchListData`. A pesagem em si salvava; o erro vinha da reação ao
evento de "pesagem criada".

**Causa raiz:** vazamento de listener no `EventBus` (singleton global). As ViewModels de
listagem se inscrevem no `EventBus` no construtor (pra recarregar a lista ao salvar/excluir),
mas **nunca se desinscreviam no `onDestroy()`** — que, ali, fecha o Service (e, por baixo, a
`Session` do Persism vira `null`). Sequência: abriu o histórico → saiu (Service fechado,
listener ficou no bus global) → salvou a saída → o evento disparou o `fetchListData()` da
ViewModel **já destruída** → `Session` nula → NPE.

**Decisão:**
- `EventBus` ganhou `unsubscribe(Consumer)` (e o `publish` atual continuou igual) — antes não
  existia como sair do bus.
- As 4 ViewModels que se inscrevem passaram a guardar o listener num campo
  (`Consumer<Object> eventListener = this::onEntityEvent`) e a chamar
  `EventBus.getInstance().unsubscribe(eventListener)` no `onDestroy()`, antes de fechar os
  services: `PesagemHistoricoViewModel`, `PesagemFormViewModel` (e por herança as 4 telas de
  formulário), `ClienteViewModel`, `ProdutoScreenViewModel`.
- **Testado:** novo `EventBusTest` (recebe evento; após `unsubscribe` não recebe mais).
  `./gradlew test` → **188 testes, BUILD SUCCESSFUL**.

---

## 2026-08-28: Formulário de pesagem — `InputRgCpf` (RG/CPF) + pesos com `InputColumnDecimal`

**Contexto:** polimento do formulário de pesagem (base `PesagemFormScreen`). O "Documento do
motorista" era um campo de texto cru; os campos de tara/bruto também eram texto cru, sem máscara
nem formatação decimal.

**Decisão:**
- **`InputRgCpf(label, state)` novo em `Components`** — campo combinado RG-ou-CPF com máscara
  dinâmica: até 9 dígitos assume RG (`##.###.###-#`), a partir do 10º assume CPF
  (`###.###.###-##`), devolvendo no state só os dígitos. Usado no lugar do campo cru
  "Documento do motorista". Formatação em `Utils.formatRgCpf` (testável isoladamente).
- **Pesos com `InputColumnDecimal`:** tara e peso bruto passaram a formatar em decimal
  (milhar com `.` e vírgula decimal), via novo `Components.InputWithButtonRowDecimal` (mesmo
  padrão de `InputWithButtonRow`, mas com o input decimal) quando há botão "Capturar", e
  `InputColumnDecimal` puro quando não há. O peso líquido continua só-leitura (calculado).
  O state continua guardando valor em ponto decimal (`32000.50`), então `parseDecimal` do
  ViewModel segue funcionando na montagem do model.
- **Testado:** `UtilsTest` ganhou 4 casos de `formatRgCpf` (RG 9 dígitos, CPF 10/11, letras,
  vazio/nulo). `./gradlew test` → **186 testes, BUILD SUCCESSFUL**.

---

## 2026-08-28: Peso líquido calculado dinamicamente — botão "Calcular" removido

**Contexto:** pedido do usuário — o campo "Peso líquido (Kg)" da pesagem dependia de um botão
"Calcular" (`PesagemFormScreen` → `vm::calcularPesoLiquido`); o operador tinha que lembrar de
clicar, e o valor podia ficar desatualizado se mudasse bruto/tara/desconto depois.

**Decisão:** `pesoFinal` passou a ser recalculado automaticamente a cada mudança de peso bruto,
tara ou de qualquer um dos 8 descontos (`PesagemFormViewModel` se inscreve nesses states e chama
`recalcularPesoLiquido()`, que reaproveita `PesagemCalculo.calcularPesoLiquido`). O campo virou
de exibição (só-leitura, `InputColumn(..., disableInput=true)`) e o botão "Calcular" foi removido
do `PesagemFormScreen` — vale automaticamente pras 4 telas de formulário (Entrada, Saída, Avulsa,
Manual), já que a base é compartilhada. Sem loop de notificação: `pesoFinal` não tem subscriber.
`./gradlew test` → **182 testes, BUILD SUCCESSFUL**.

---

## 2026-08-28: Pesagem — telas de formulário separadas por tipo + `tipo_pesagem` no lugar de `operacao`

**Contexto:** a tela única de Pesagem (`PesagemScreen`, baseada em `ContratoTelaCrudV3`) misturava
entrada, saída, pesagem avulsa e pesagem manual num formulário só, e o tipo era decidido por
paridade de placa (`PesagemService.determinarOperacao`: ímpares viram "Entrada", pares "Saída").
Isso forcava a tela de Pesagem num formato genérico de CRUD que não refletia os fluxos reais do
operador. Pedido: separar em telas por tipo, com semântica própria em cada uma, e parar de inferir
tipo por placa.

**Decisões (confirmadas com o usuário):**
- **Formulários separados por tipo**, sem ligação com `ContratoTelaCrudV3` (não estendem
  `ViewModelScreenContract` nem o contrato de CRUD genérico — é um fluxo próprio de formulário),
  criados como telas independentes em `my_app/screens/pesagemScreen/`: `PesagemEntradaScreen`,
  `PesagemSaidaScreen`, `PesagemAvulsaScreen`, `PesagemManualScreen`, + `PesagemHistoricoScreen`
  (esta última **usa** o `ContratoTelaCrudV3` para a lista/modal de detalhes).
- **`tipo_pesagem` substitui `operacao`**: valores `entrada`/`saida`/`avulsa`/`manual`; o tipo
  passa a ser decidido pela tela que o operador abriu, não mais inferido por paridade de placa.
  Migration `V14__troca_operacao_por_tipo_pesagem.sql` (rename+popula+drop com suporte SQLite).
- **Base compartilhada:** `PesagemFormViewModel` (abstrata: services, states do form, lambda da
  balança, descontos, fotos, `montarModel`/`salvar`/`capturarFotos`) e `PesagemFormScreen` (layout
  base com hooks) — cada tipo sobrescreve o que tem de específico.
- **Semântica por tipo:** Entrada = formulário cheio + balança; Saída = digita a placa → puxa a
  última Entrada daquela placa (dados + Tara via `PesagemService.buscarTaraSugerida`, que agora
  é baseada na **última entrada** `tipo=entrada` — não mais "entrada em aberto"), Tara não
  capturável, Bruto capturável; Avulsa = formulário cheio, Tara digitada manualmente, Bruto
  capturável; Manual = sem balança, Tara e Bruto digitados, Líquido calculado.
- **`PesagemService.salvar()` exige `tipoPesagem` não-blank** e não infere mais nada; removido
  `determinarOperacao`. Novo `PesagemRepository.buscarPorPlacaETipo(placa, tipo)` e
  `PesagemService.buscarUltimaEntrada(placa)`.
- **Navegação:** `Secao.PESAGEM_HISTORICO` novo em `HomeScreenViewModel` (switch instancia as 5
  telas novas) + botão "Histórico de pesagens" na Sidebar; `AppRoutes` perdeu a rota/enum
  `PESAGENS` antiga (apontava pra `PesagemScreen`, deletada).
- Registro histórico da "tara sugerida por entrada em aberto" (2026-08-19) deixou de valer: agora
  a tara sugerida vem da última entrada, independente de a placa estar "aberta" ou "fechada".

**Testado:** `PesagemServiceTest`/`PesagemRepositoryTest`/`TicketPdfExporterTest` atualizados
(tipo em vez de operação, `buscarPorPlacaETipo`, `buscarUltimaEntrada`); `./gradlew test` →
**182 testes, BUILD SUCCESSFUL**.

---

## 2026-08-19: Integração com câmera Intelbras — captura automática de foto na pesagem

**Contexto:** item adiado desde a auditoria do app antigo ("Fase 2" em `TODO.md`) — a
especificação original previa foto do caminhão na pesagem, nunca implementada nesta reescrita
(campos `fotoFrente1/2`/`fotoCostas1/2` só aceitavam escolher um arquivo já existente via
`FileChooser`, sem integração real com câmera nenhuma).

**Pesquisa:** câmeras Intelbras da linha VIP são rebrand da Dahua — herdam o mesmo endpoint HTTP
CGI `/cgi-bin/snapshot.cgi?channel=<n>`, autenticado por HTTP **Digest** (não Basic; a câmera
responde 401 com o desafio, cliente recalcula e reenvia). ONVIF Profile S também é suportado
como padrão genérico, mas o CGI direto é mais simples e é o que os posts do fórum oficial da
Intelbras confirmam funcionando pra linha VIP. Existe um SDK proprietário, mas exige assinar
termo de confidencialidade com a Intelbras — não vale a pena quando o endpoint CGI já resolve.
Fontes: fórum oficial Intelbras (temas 66724, 62599, 65034), guia de compatibilidade SmartRTSP.

**Topologia confirmada com o usuário:** 2 câmeras físicas fixas (frente e costas da balança,
apontadas pro caminhão), não 4 câmeras nem uma única com PTZ. Cada pesagem nova dispara as duas:
"Entrada" preenche `fotoFrente1`/`fotoCostas1`, "Saída" preenche `fotoFrente2`/`fotoCostas2` —
mesma regra de duas visitas por placa já usada pra Tara sugerida.

**Decisão:**
- `CameraSnapshotClient` (`my_app.infra.camera`) — implementa o desafio/resposta Digest (RFC
  2617, MD5) na mão em cima de `java.net.http.HttpClient`, que não tem suporte nativo a Digest
  (só Basic via `java.net.Authenticator`). Sem lib nova.
- `ConexaoCameraModel`/`Repository`/`Service` (migration `V13`) — configuração singleton (uma
  linha, igual `ConexaoBalancaModel`) com os dois conjuntos de campos (IP/porta/canal/usuário/
  senha) pra frente e costas. As duas câmeras são **opcionais e independentes**: só valida que,
  se um IP foi informado, a porta também foi — nenhuma é obrigatória (diferente de
  `ConexaoBalancaModel`, onde a balança em si é sempre obrigatória).
- `ConexaoCameraScreen`/`ViewModel` (menu Gerencial > "Conexão das câmeras") — um formulário só
  com as duas seções, cada uma com botão "Testar câmera" que captura de verdade e mostra a foto
  (`Image` reativo) pra confirmar que a câmera está acessível e enquadrada certo antes de
  colocar em produção.
- `PesagemViewModel.capturarFotosAutomaticamente()` — chamado só ao **criar** uma pesagem nova
  (não ao editar uma existente: reeditar depois não deve disparar a câmera de novo, o caminhão
  pode nem estar mais lá), logo após o `pesagemService.salvar()` ter sucesso — não na hora de
  imprimir o ticket, que era o timing errado do app original (ver `TODO.md`, Fase 2). Câmera não
  configurada é pulada sem erro; falha de rede/autenticação numa câmera não derruba a pesagem
  (já está salva nesse ponto) — só fica sem aquela foto, e um `log.warn` registra o motivo.
- `FotoPesagemStorage` (`my_app.infra.camera`) — salva os bytes JPEG capturados em
  `~/.gobitech/fotos/`, diferente da foto escolhida manualmente (que só guarda a URI do arquivo
  onde já estava — não há "arquivo original" pra uma captura de câmera, só bytes de resposta
  HTTP).

**Testado:** `CameraSnapshotClientTest` sobe um servidor HTTP fake (`com.sun.net.httpserver`, já
no JDK, sem lib nova) que exige o mesmo desafio Digest de uma câmera real e só libera o JPEG se
o `response` calculado bater — pega de verdade um bug no cálculo (ordem de campo errada, aspas
faltando etc.), não só "chegou alguma coisa". `ConexaoCameraServiceTest`/`RepositoryTest`
seguem o padrão de `ConexaoBalanca`. `./gradlew test`: **181 testes, BUILD SUCCESSFUL**.

---

## 2026-08-19: "Abrir pasta de logs" travava o app inteiro (bloqueava a FX Application Thread)

**Contexto:** usuário reportou o app travando ao clicar em "Abrir pasta de logs" — mesmo depois
da pasta já ter sido limpa (ver decisão do log gigante, abaixo), o problema continuava. Não era
mais sobre tamanho de arquivo.

**Causa:** `LogsScreenViewModel.abrirPastaDeLogs()` estava ligado direto no `onClick` do botão
(`LogsScreen`) e chamava `Desktop.getDesktop().open(...)` de forma síncrona — essa chamada
bloqueia esperando o SO terminar de abrir o gerenciador de arquivos, e como o clique do botão
roda na FX Application Thread, a janela inteira parava de responder enquanto isso. O mesmo
padrão pro PDF do ticket (`PesagemViewModel.abrirArquivo()`) já rodava dentro de `Async.Run` —
só a versão da tela de Logs tinha ficado de fora.

**Decisão:** `abrirPastaDeLogs()` movido pra dentro de `Async.Run`, com o
`Components.ShowAlertError` do caminho de erro voltando pra FX thread via `UI.runOnUi` (a
`Alert` do JavaFX só pode ser criada/mostrada nela). Conferido que não sobrou nenhuma outra
chamada a `Desktop.getDesktop().open()` fora de `Async.Run` no projeto.

---

## 2026-08-19: Componentes escritos direto no node JavaFX — corrigido, estendido o Megalodonte

**Contexto:** feedback do usuário na `LogsScreen` (ver decisão logo abaixo): o código castava
`TextAreaInput.getNode()` pra `TextArea` e chamava `setEditable`/`setWrapText`/`setStyle`/
`setMaxHeight`/`VBox.setVgrow` direto no node, em vez de resolver via Props do Megalodonte. "Se
o que você precisou não existe no Megalodonte, crie lá" — o mesmo padrão apareceu em mais dois
lugares desta sessão: `Components.limitTableHeight` (castava `SimpleTable.getTableView()` pra
setar `maxHeight`) e o botão flutuante "Criar novo" em `ContratoTelaCrudV3` (`StackPane.
setAlignment`/`setMargin`/`VBox.setVgrow` direto no node do `Stack`).

**Decisão:** estendido `megalodonte-components` (repo próprio, publicado em `mavenLocal`) em vez
de continuar castando:
- `TextComponentProps.fontFamily(String)` — base compartilhada de todo componente de texto.
- `InputProps.editable(boolean)` e `.fillHeight()` — aplicados tanto no `TextField` de uma linha
  quanto no `TextArea`.
- `SimpleTableProps.maxHeight(double)` — precisou mover o "libera crescimento vertical" padrão
  que estava hardcoded no construtor de `SimpleTable` pra dentro de
  `SimpleTableProps.applyContainerStyling`: `Props.apply()` roda dentro do `super(...)` do
  `Component`, **antes** do resto do construtor de `SimpleTable` — um `setMaxHeight` fixo lá
  depois sempre sobrescrevia de volta qualquer teto customizado vindo dos Props.
- `Stack.childInCorner(Component, Stack.Corner, int margin)` e `Stack.fillHeight()` — fixa um
  filho num canto (ex.: FAB por cima de conteúdo scrollável) sem expor `Pos`/`Insets` do JavaFX
  na API pública; `Corner` é um enum simples, mesmo idioma que `RowProps`/`ColumnProps` já usam
  pros próprios métodos de alinhamento nomeados (`centerHorizontally()`, `bottomVertically()`
  etc.) em vez de aceitar `Pos` cru.

`LogsScreen`, `Components.limitTableHeight` (removido, virou `new SimpleTableProps().maxHeight(
Components.TABLE_MAX_HEIGHT)` direto no construtor de cada tabela) e o botão flutuante do
`ContratoTelaCrudV3` foram reescritos em cima dessa API nova. `megalodonte-components`
republicado em `mavenLocal`; validado ponta a ponta contra o `balanca-gobitech`
(`--refresh-dependencies compileJava test`: sem regressão).

---

## 2026-08-19: Log de produção crescendo pra dezenas de milhares de linhas — causa raiz era teste

**Contexto:** usuário reportou a `LogsScreen` (ver decisão abaixo) extremamente pesada, a ponto
de travar o app ao tentar abrir a pasta de logs pelo gerenciador de arquivos. `gobitech.log`
estava com **37.539 linhas / 4,6MB**, e a pasta inteira (com os arquivos já rolados pelo
`RollingFileAppender`) somava **14MB**.

**Causa raiz:** os testes não tinham `logback.xml` próprio — herdavam
`src/main/resources/logback.xml` (o mesmo do app real) e escreviam direto em
`~/.gobitech/logs/gobitech.log`. Cada teste recria o schema do zero via Flyway
(`BaseRepositoryTest`), e cada migration loga ~17 linhas em `INFO` — uma única rodada de
`./gradlew test` já gerava perto de 17 mil linhas de log só de migration. Rodada várias vezes ao
longo da sessão (desenvolvimento normal + verificação de cada mudança), isso sozinho respondia
por ~33 mil das 37.539 linhas do arquivo — **não era uso real do app**.

**Decisão:** `src/test/resources/logback-test.xml` — Logback prioriza esse arquivo sobre
`logback.xml` quando os dois estão no classpath, exatamente pra isolar log de teste do de
produção. Só `ConsoleAppender`, sem `FileAppender` nenhum (fica estruturalmente impossível
repetir o vazamento), nível `WARN` em vez de `INFO` (silencia o ruído repetitivo do
Flyway/Persism sem esconder erro/aviso real). Verificado rodando a suíte completa duas vezes
seguidas: `gobitech.log` não ganhou nenhuma linha nova nas duas.

Arquivos antigos (rolados + o `gobitech.log`/`error.log` ativos, ~14MB no total) apagados/
esvaziados com autorização do usuário, já que eram majoritariamente ruído de teste.
`LogsScreenViewModel` também limitado a mostrar só as últimas 500 linhas do arquivo (`TextArea`
do JavaFX não é virtualizado como `TableView` — mesmo sem o vazamento de teste, o arquivo
inteiro carregado nele deixa a tela pesada a partir de alguns milhares de linhas).

---

## 2026-08-19: Tela de Logs em Suporte + logging em todos os pontos principais da aplicação

**Contexto:** pedido do usuário — nenhuma forma de ver o log da aplicação de dentro do app, e a
maior parte do código tratava erro com `e.printStackTrace()` (não persiste em lugar nenhum
acessível pro usuário/suporte) ou simplesmente engolia a exceção sem log nenhum.

**Decisão:**
- `LogsScreen`/`LogsScreenViewModel` (novo) — item "Ver logs da aplicação" no menu Suporte
  (`HomeScreen.menuBar()`), lê `~/.gobitech/logs/gobitech.log` (caminho já definido em
  `logback.xml`) e mostra num viewer só-leitura, com botão de atualizar e de abrir a pasta de
  logs no gerenciador de arquivos padrão (`java.awt.Desktop`).
- Toda ocorrência de `e.printStackTrace()` no `src/main/java` (6 arquivos) virou `log.error(...)`
  de verdade.
- `BaseService` (classe-base de todo Service de entidade) ganhou logging de `salvar`/
  `atualizar`/`excluirById` por padrão — sem logar o model inteiro via `toString()` (não há
  garantia, numa classe genérica, de que um Model futuro não ganhe campo sensível exposto via
  `@ToString`). Como a maioria dos Services concretos sobrescreve esses métodos (chamando
  `repository.X` direto, não `super.X`), cada um ganhou log próprio com campos escolhidos a
  dedo — `UsuarioService` em particular **nunca** loga senha, só login (login/senha continuam
  sempre criptografados em repouso, ver decisão de login mais abaixo), e loga tentativas de
  login falhas/bem-sucedidas.
- Leitores de balança (TCP/Serial): log de conectando/conectado/erro/encerrando — antes,
  silenciosos.
- Todo catch que só engolia a exceção (ou só fazia `printStackTrace()`) nos ViewModels de
  Cliente, Produto, Usuário, Pesagem, Licença, Empresa, Conexão da balança, Dashboard e Home
  (logout) virou `log.error`/`log.warn` real.

---

## 2026-08-19: Botão "Salvar e baixar ticket" no formulário de pesagem

**Contexto:** pedido do usuário — o formulário de pesagem só tinha "Salvar" (adiciona/atualiza
direto); pra baixar o ticket (ver decisão do ticket em PDF, abaixo) era preciso salvar, voltar
pra lista e abrir os detalhes da pesagem de novo.

**Decisão:** `PesagemViewModel.handleAddOrUpdate()` refatorado numa `salvar(boolean
tambemBaixarTicket)` privada compartilhada — o botão novo passa `true` e reusa exatamente o
mesmo fluxo de exportar/abrir o PDF (`imprimirTicket`) já usado no modal de detalhes, chamado
logo após o salvamento ter sucesso, antes de `voltarParaLista()` (formulário ainda na tela
quando o diálogo de salvar aparece). O botão "Salvar" original não muda de comportamento.

---

## 2026-08-19: Botão "Criar novo" flutua fixo no canto inferior direito

**Contexto:** pedido do usuário — antes ficava no fluxo normal da página, depois da tabela; em
telas com muitos registros era preciso rolar até o fim pra achá-lo.

**Decisão:** movido pra dentro de um `Stack` sobreposto ao conteúdo scrollável, fixo no canto
inferior direito com ~20px de margem (independente da posição do scroll) — padrão "floating
action button". Está no `listPage()` privado do `ContratoTelaCrudV3`, então vale igual pras 4
telas que usam esse fluxo padrão (Cliente, Produto, Usuário, Pesagem); Licença tem layout
próprio e não é afetada. (A implementação em si — `StackPane.setAlignment` cru — foi corrigida
depois pra usar a API nova do Megalodonte; ver decisão "Componentes escritos direto no node
JavaFX" acima.)

---

## 2026-08-19: Ticket de pesagem em PDF, abrindo automaticamente no visualizador do sistema

**Contexto:** usuário apontou que os requisitos do projeto original (auditoria do app antigo)
previam poder imprimir o "tiquet" da pesagem, e essa parte nunca foi implementada nesta
reescrita. O app antigo (`pesagemFinal`) imprimia direto numa impressora térmica via ESC/POS —
mas a decisão de "Recursos específicos de varejo/marketing removidos" (mais abaixo neste
arquivo) já tinha descartado a seleção de impressora térmica em Preferências, seguindo o mesmo
caminho do `plics-sw`: exportar PDF em vez de falar com impressora/porta/spooler diretamente.

**Decisão:** `TicketPdfExporter` (novo, `my_app.infra`) gera o ticket em PDF com Apache PDFBox
(já era dependência do projeto, herdada do `plics-sw`, mas não usada nesta cópia) — mesmo padrão
de código do `RelatorioPdfExporter` do `plics-sw`. Cabeçalho com dados da empresa
(`EmpresaModel`), veículo/motorista, cliente/produto, tara/bruto/líquido e o detalhamento de
descontos já existente (`DescontoModel`, só os campos não-zerados) — deliberadamente mais simples
que o ticket "CLASSIFICAÇÃO" do app antigo (umidade/avariados/impureza em percentuais soltos,
sem amostra/peso convertido/peso NF — campos que nunca tiveram implementação real por trás nesta
reescrita).

Botão "Imprimir ticket" só no modal de detalhes da tela de Pesagem
(`PesagemScreen.showItemDetailsComAcoes`, sobrescrevendo o padrão do `ContratoTelaCrudV3`) — as
outras telas de CRUD que reusam o mesmo contrato não ganham essa ação, não faz sentido pra
Cliente/Produto/Usuário/Licença.

Depois de salvo, o PDF abre sozinho no visualizador padrão do sistema (`java.awt.Desktop`) — o
operador já consegue imprimir de lá (Ctrl+P) sem o app precisar falar com impressora nenhuma;
falha em silêncio se o ambiente não suportar (arquivo já está salvo, caminho aparece no popup de
qualquer forma).

---

## 2026-08-19: Teto de altura nas tabelas de listagem

**Contexto:** pedido do usuário — `SimpleTable` (megalodonte-components) crescia livre até
preencher todo o espaço vertical disponível na página, mesmo com poucos itens.

**Decisão:** teto de 350px (~6-8 linhas visíveis) nas 5 telas de lista (Cliente, Produto,
Usuário, Licença, Pesagem) — acima disso a tabela rola por dentro sozinha (comportamento nativo
do `TableView`), nenhum item fica escondido. Aplicado por app (não editando o componente
compartilhado direto) porque outros apps (`plics-sw`) também usam `SimpleTable` e não pediram
essa mudança. (A forma de aplicar o teto mudou depois — ver decisão "Componentes escritos direto
no node JavaFX" acima: `Components.limitTableHeight` castando `getTableView()` virou
`SimpleTableProps.maxHeight`, um recurso de verdade do componente.)

---

## 2026-08-19: Versão do app empacotado sempre mostrava "dev"

**Contexto:** usuário notou que o pacote gerado (via o workflow de release) mostra versão "dev"
em vez da versão real. `scripts/config.py::run_jpackage()` já passa
`--java-options -Dplics.appVersion=${APP_VERSION}` corretamente pro launcher nativo — o problema
estava do lado de quem lê.

**Causa:** `Main.APP_VERSION = System.getProperty("-Dplics.appVersion", "dev")` — o `-D` faz
parte só da sintaxe de linha de comando pra *definir* uma system property
(`java -Dchave=valor`), não da chave em si; a JVM guarda só `plics.appVersion`. Procurar por
`"-Dplics.appVersion"` (com o `-D` incluído na string) nunca batia com nada, então sempre caía
no default `"dev"` — em qualquer build, empacotado ou não.

**Decisão:** removido o `-D` da chave — `System.getProperty("plics.appVersion", "dev")`.
Verificado com um `java -Dplics.appVersion=1.0.0` isolado, exatamente a flag que o jpackage
gera: antes do fix lia "dev" mesmo com a flag presente, depois lê "1.0.0" corretamente; sem a
flag (`./gradlew run` local) continua caindo no default "dev", como esperado pra build de
desenvolvimento.

## 2026-08-19: Revertido o teto de tamanho da Stage — quebrava maximizar de propósito

**Contexto:** usuário reportou que não conseguia mais maximizar a janela. Regressão direta de um
fix meu de ontem (`ScreenContext.applyStageProps`, megalodonte-router): pra impedir a janela de
crescer sozinha além do declarado na rota (sintoma investigado então: conteúdo alto empurrando
Logout/"Criar novo" pra fora da tela), adicionei `stage.setMaxWidth(width)` /
`setMaxHeight(height)`. Isso trava a janela num teto **absoluto** — vale pra qualquer resize,
inclusive o usuário clicando maximizar de propósito numa rota `screenIsExpandable=true`, que é
exatamente o oposto do que essa flag deveria permitir.

**Decisão:** revertido — removidas as duas linhas. O problema original que motivou o teto já
tinha correção no nível certo, feita antes disso no mesmo dia (`Show.fillHeight()` em
`ContratoTelaCrudV3.mainView()` + o `ScrollPane` na Sidebar, ambos no app consumidor): o
conteúdo em si passou a respeitar o espaço disponível e rolar por dentro em vez de crescer,
então travar a janela era redundante além de errado — a documentação de quando adicionei o teto
já registrava essa incerteza ("não consegui confirmar visualmente o resultado final").

**Verificado numericamente** (diagnóstico jogável fora): depois de navegar pra uma rota
`resizable=true`, `stage.getMaxWidth()`/`getMaxHeight()` voltam a `Double.MAX_VALUE` (sem teto),
em vez de travados no tamanho declarado da rota.

## 2026-08-19: Workflow de release não publicava as bibliotecas megalodonte-libs primeiro

**Contexto:** primeira execução de verdade do workflow (usuário publicou o repo e rodou) falhou:
`Could not find megalodonte:megalodonte-base:1.0.0-beta` — óbvio em retrospecto. Essas
bibliotecas nunca foram publicadas em nenhum repositório remoto (Maven Central etc.), só existem
via `mavenLocal()` — e só estavam disponíveis nesta máquina porque eu venho publicando cada uma
manualmente (`./gradlew publishToMavenLocal`) a cada mudança nelas ao longo da sessão. Um runner
de CI não tem nada disso.

**Decisão:** o workflow agora clona `megalodonte-libs` (workspace com os 5 submódulos, cada um
seu próprio repositório público) e roda `install-all.sh`/`install-all.ps1` — os mesmos scripts
que já existiam pra instalação local — antes de empacotar o app, publicando as 5 bibliotecas em
mavenLocal no próprio runner.

**Isso escancarou outro bug real, achado tentando reproduzir localmente antes de mexer no
workflow**: cloná-la do zero com `--recurse-submodules` falhava — o ponteiro de submódulo de
`megalodonte-theme` dentro do superprojeto apontava pro commit `6a625dc` ("fix: remove broken
javadoc link..."), mas esse commit nunca tinha sido enviado pro remoto de `megalodonte-theme`,
só existia local nesta máquina. `git rev-list --left-right --count origin/main...HEAD` confirmou
1 commit só local antes do push. Sem acesso de push configurado neste ambiente (sem `gh` logado,
sem credential helper), pedi confirmação e o usuário enviou o commit manualmente — depois disso,
clone fresco + `install-all.sh` completo (as 5 bibliotecas) rodou limpo, verificado de verdade,
não só lido.

## 2026-08-19: Workflow de release no GitHub — e dois bugs reais achados no caminho

**Contexto:** pedido pra criar um workflow de GitHub Actions que gera releases automaticamente,
igual ao já usado nos projetos de `scene2d-suite` (`hud-creator-gdx`/`atlas-animator-gdx`,
`.github/workflows/package.yml`). Diferente daqueles projetos (libGDX/LWJGL3, empacotam com
`./gradlew installDist` + `jpackage` direto no workflow), este app já tinha seu próprio
empacotamento pronto (`scripts/create-deb.py`, `create-msi.py`, `config.py` — shadowJar + jlink +
jpackage, JavaFX embutido via jlink) — então o workflow novo só precisa **invocar** esses
scripts existentes dentro do CI, não reimplementar a lógica de empacotamento.

**Decisão:** `.github/workflows/package.yml`, mesmo padrão de gatilho do scene2d-suite
(`workflow_dispatch` manual + push de tag `v*`), matriz `ubuntu-latest`/`windows-latest`, um job
`package` que roda `scripts/create-deb.py`/`create-msi.py` e sobe os instaladores como artifact,
e um job `release` que baixa os artifacts e publica via `softprops/action-gh-release@v2`,
lendo a versão de `gradle.properties` (mesma composição `appVersion[.appPatch]` de
`config.py::APP_VERSION`).

Duas coisas que os scripts existentes precisam e não vêm de nenhuma dependência do Gradle:
- **JavaFX modules pro jlink** (`JAVAFX_MODULES_HOME`): baixado no workflow do mesmo zip que já
  é usado localmente (`java_fx_modules.zip`, publicado como release asset em
  `megalodonte-base` — link fornecido pelo usuário), já vem com `linux-25.0.1/` e
  `windows-25.0.1/` nos nomes certos, sem precisar renomear nada.
- **WiX Toolset** (Windows, `jpackage --type msi` shella pra ele) e **Xvfb** (Linux, o
  `smoke_test()` de `config.py` abre uma janela JavaFX de verdade por alguns segundos pra
  confirmar que o runtime empacotado sobe sem exceção — sem display nenhum no runner, precisa de
  um virtual).

**Verificado de verdade, não só lido**: rodei o pipeline localmente com o JDK 25 e o zip de
JavaFX modules baixado da mesma URL do workflow (só pulando o `smoke_test()`, que exige um
display de verdade e não consegui instalar Xvfb aqui sem sudo) — `shadowJar` → `jlink` →
`jpackage --type deb` produziram um `.deb` real, válido, ~260MB, com metadata correta (versão,
vendor, dependências das libs de mídia do JavaFX). Isso validou a parte mais arriscada do
workflow (a nova, nunca testada: baixar/estruturar o JavaFX modules em CI) antes de considerar
pronto.

**Dois bugs reais achados tentando rodar isso, não hipotéticos:**
1. `gradle.properties` estava salvo com encoding misto — a maior parte do arquivo em UTF-8 de
   verdade, mas a linha `appDescription=Sistema de balança para caminhão.` especificamente em
   Latin-1/CP1252 (bytes `0xe7`/`0xe3` pra "ç"/"ã"). `scripts/config.py` lê esse arquivo sem
   especificar encoding (padrão da plataforma, UTF-8 no Linux) — `UnicodeDecodeError` na hora,
   quebrando **qualquer** empacotamento local, não só em CI. Corrigido reescrevendo só aquela
   linha em UTF-8 de verdade (tentei uma primeira vez decodificando o arquivo inteiro como
   Latin-1 e resalvando — piorou as linhas que já estavam certas, duplo-corrompendo acentos que
   já eram UTF-8 válido; revertido e corrigido preservando o resto do arquivo).
2. `.gitignore` tinha uma regra genérica `*.properties` (pensada pra excluir `app.properties`,
   configs com segredo) que **também pegava `gradle.properties`** — um arquivo sem segredo
   nenhum (nome/versão/vendor do app), lido tanto por `build.gradle.kts` quanto pelos scripts de
   empacotamento. A exceção existente (`!.gradle.properties`, com ponto extra no começo) não
   batia com o nome de verdade do arquivo. Resultado: `gradle.properties` nunca esteve
   versionado neste repositório — um checkout novo do zero (exatamente o que o workflow de CI
   faz a cada execução) não teria o arquivo, e tanto o build quanto o release quebrariam de
   cara. Corrigido: `!gradle.properties` (nome certo) no `.gitignore`, arquivo adicionado ao
   controle de versão.

Também corrigido `config.py::open_dist_folder()` — chamado no fim dos dois scripts, tentava
abrir o gerenciador de arquivos (`xdg-open`/`os.startfile`) sem checar se havia sessão de
desktop; num runner de CI isso quebraria o workflow bem no fim, depois de já ter gerado o
pacote. Agora vira no-op quando `CI=true` (setado automaticamente pelo GitHub Actions em todo
runner) — comportamento local/interativo continua igual.

**Pendente, fora do meu escopo sem autorização explícita:** este repositório ainda não tem
remote nenhum configurado (`git remote -v` vazio) — o workflow só passa a rodar de verdade
depois que o repo for criado no GitHub e o código, enviado (`git push`). Não fiz isso — criar
repositório remoto e publicar código são ações que pedem confirmação explícita.

## 2026-08-19: Tara sugerida a partir da entrada em aberto da mesma placa

**Contexto:** ao explicar como Tara/Peso bruto/Peso líquido funcionam na prática (usuário
perguntou "como se eu fosse o operador"), ficou claro que "Entrada"/"Saída"
(`PesagemService.determinarOperacao`) era só um rótulo alternado por placa — não alimentava nada
entre uma pesagem e a próxima. Na prática real, o caminhão não muda de peso vazio entre a
entrada e a saída da mesma visita, então repetir a pesagem vazia na Saída é redundante — o
operador deveria poder reaproveitar a Tara já capturada na Entrada.

Achado de bônus: existia um método `carregarOperacaoPelaPlaca()` na ViewModel, já escrito, mas
**sem nenhum chamador em lugar nenhum do app** — calculava a operação e descartava o resultado.
Substituído por completo pela feature de verdade, em vez de deixado como código morto ao lado.

**Decisão:** `PesagemService.buscarTaraSugerida(placa)` — mesma regra de
`determinarOperacao` (conta de pesagens pra essa placa, ímpar = tem entrada em aberto), mas
devolve a `Tara` daquela entrada em vez de só o rótulo; `null` se não há entrada em aberto (ou
a entrada não tinha Tara). `PesagemViewModel` assina mudanças em `placa` e, quando o valor bate
com uma entrada em aberto, preenche o campo Tara sozinho + um toast avisando de onde veio (o
operador ainda pode sobrescrever digitando por cima, é só um ponto de partida).

**Cuidado com a ordem, por ser assíncrono:** diferente do auto-preenchimento de desconto do
produto (que só lê um objeto já em memória, roda síncrono), esse busca no banco — não dá pra
contar que o resultado chega antes de `populateFieldsFromModel()` (editar/clonar) já ter setado
a Tara de verdade. `modoEdicao.get()` só é checado dentro do callback assíncrono (quando o
resultado chega), não no disparo do `placa.subscribe(...)` — `populateFieldsFromModel()` seta a
placa *antes* de `modoEdicaoState()` virar `true` (ver `ContratoTelaCrudV3.
handleClickMenuEdit`), então checar no disparo veria `false` incorretamente e aplicaria a
sugestão em cima da Tara real de uma pesagem sendo editada. Também checa se a placa não mudou de
novo enquanto a busca corria (evita sugestão de uma placa antiga chegando atrasada e
sobrescrevendo o que já é relevante pra placa atual).

**Testado:** `PesagemServiceTest` — placa nunca pesada retorna `null`; placa com uma Entrada
(Tara explícita) retorna essa Tara; placa com Entrada+Saída (fechada, sem entrada em aberto)
retorna `null`.

## 2026-08-18: DatePicker de validade da licença só aparece se o usuário optar por definir expiração

**Contexto:** antes, o `DatePicker` de "Data de validade" em `LicensaScreen` ficava sempre
visível, com um texto de apoio dizendo "deixe em branco pra sem expiração" — pedido: o
DatePicker só deve aparecer se o usuário explicitamente escolher definir uma data.

**Decisão:** `LicensaViewModel` ganhou `definirExpiracaoSelected` (Sim/Não, mesmo padrão
`Data.simNaoList` já usado em outras telas — ex. "Administrador?" em Usuário), default "Não"
(a maioria das licenças, ex. pro próprio admin, não expira). `LicensaScreen` troca o texto de
apoio pelo seletor Sim/Não e envolve o `DatePickerColumn` num `Show.when(vm.definirExpiracao,
...)`.

`gerar()` passou a consultar o toggle (`definirExpiracao.get()`), não só se `dataExpiracao` já
tinha valor — sem isso, escolher uma data e depois mudar de ideia pra "Não" deixaria a data
antiga (ainda no state, só não mais visível na tela) sendo usada mesmo assim. Também adicionada
uma validação: "Sim" selecionado sem nenhuma data escolhida agora alerta "Selecione a data de
expiração" em vez de gerar silenciosamente uma licença sem expiração (o oposto do que "Sim"
significa).

## 2026-08-18: NullPointerException ao clicar em Logout — regressão do fix do Dashboard

**Contexto:** usuário reportou `NullPointerException: ... State.get() is null` ao clicar em
Logout na sidebar.

**Causa:** regressão introduzida por mim mesmo no fix do Dashboard (ver decisão "Tela de boas-
vindas virou um dashboard real" mais abaixo): `HomeScreenViewModel.logout()` ainda fazia
`telaAtiva.set(null)` antes de navegar pra AUTH — um passo que fazia sentido no design ANTIGO,
onde `telaAtiva == null` era um estado válido (`HomeScreen.renderConteudo()` tratava esse caso
caindo pra `welcomeContent()`). Quando o Dashboard passou a ser sempre uma tela de verdade,
`renderConteudo()` foi simplificado pra `viewModel.telaAtiva.get().render()` sem checar null —
mas o `telaAtiva.set(null)` do `logout()` ficou esquecido, e como `HomeScreen` está inscrito em
`telaAtiva` (`subscribe(tela -> renderConteudo())`), esse `set(null)` disparava
`renderConteudo()` na hora, batendo de frente com o `.get().render()` sem proteção.

**Decisão:** removida a linha `telaAtiva.set(null)` de `logout()` — não tem mais necessidade
nenhuma dela: `destruirTelaAtual()` já libera os recursos da tela atual (onDestroy + cancela o
Scope), e `navigateAndCloseOthers` troca a Scene inteira por AUTH logo em seguida, então não
sobra nenhum `render()` rodando em cima de um `telaAtiva` nulo.

## 2026-08-18: Campo CPF/CNPJ sempre formatava como CNPJ, mesmo digitando um CPF

**Contexto:** achado durante teste manual (`testes-manuais.md`, caso #25, seção Cliente):
funcionalmente OK (detecção de duplicado funcionava), mas a máscara do campo "CPF/CNPJ" sempre
aplicava o padrão de CNPJ (`AA.AAA.AAA/AAAA-DD`), mesmo digitando um CPF de 11 dígitos.

**Causa:** o campo "CPF/CNPJ" (em `ClienteScreen` e também em `CadastroEmpresaScreen` — mesmo
bug, mesmo componente) estava ligado a `Components.InputColumnCnpjAlfanumerico`, que chama
`Utils.formatCnpj(...)` incondicionalmente em `onInitialize`/`onChange`, sem nenhum branch por
tamanho. Existia um `InputColumnCpf` com a máscara certa pra CPF, mas nenhuma das duas telas com
campo combinado usava ele — não existia um componente que decidisse a máscara certa conforme o
tamanho digitado.

**Decisão:** `Utils.formatCpfCnpj(String)` novo — enquanto o digitado tem 11 caracteres ou menos,
assume CPF (só numérico, `formatCpf` com letras descartadas); a partir do 12º, assume CNPJ
(aceita letras — formato alfanumérico mais recente). O reagrupamento dos separadores ao cruzar
esse limiar (ex.: dígitos de CNPJ digitados rápido, os primeiros 11 aparecem com máscara de CPF
até o 12º caractere entrar) é esperado — sem perguntar de antemão qual documento é, não dá pra
saber os grupos certos antes de ver o tamanho final; é o mesmo comportamento usado por a maioria
dos sistemas brasileiros com campo CPF/CNPJ combinado. `Components.InputColumnCpfCnpj` novo
substitui `InputColumnCnpjAlfanumerico` nos dois call sites (`ClienteScreen`,
`CadastroEmpresaScreen`) — o componente antigo, sem mais chamadores, foi removido.

## 2026-08-18: Toggle da sidebar movido pro topo + ícone único que gira até a posição final

Dois ajustes rápidos em cima do fix anterior: (1) posição subiu de "perto do fim" (`Pos.
BOTTOM_LEFT` + `translateY(-30)`) pra "perto do topo" (`Pos.TOP_LEFT` + `translateY(30)`) —
estava perto demais do Logout lá embaixo. (2) trocado o par `CHEVRON_LEFT`/`CHEVRON_RIGHT`
reativo (dois ícones diferentes, trocados na marra) por um único `CHEVRON_LEFT` que **gira** até
a posição final via `Animations.rotate(...)` — 0° mostra "<", girado 180° o mesmo desenho vira
">" (chevron é simétrico por rotação de 180°), então a animação já entrega o ícone certo pro
novo estado em vez de só ficar mais bonita.

## 2026-08-18: Toggle circular só aparecia "pela metade" — precisava estar num Stack mais externo, e a posição virou perto do fim (não mais centralizada)

**Contexto:** depois do fix anterior (toggle circular flutuando sobre a borda da sidebar, dentro
do `Stack` interno de `Sidebar.render()`), o usuário mandou um screenshot: só metade do círculo
aparecia — o resto sumia. Pedido adicional: em vez de centralizado verticalmente, o botão devia
ficar perto do fim da sidebar (`altura da sidebar - 30px`).

**Causa do "meio círculo":** o `Stack` que envolvia o toggle era interno a `Sidebar.render()` —
só embrulhava o corpo da própria sidebar (o `ScrollPane`), não o `contentArea` vizinho. Como o
botão usa `translateX` pra "vazar" metade pra fora da largura da sidebar, essa metade que vaza
cai visualmente em cima do `contentArea` — mas `contentArea` é um **irmão** do `Stack` da
sidebar dentro do `Row` de `HomeScreen`, não um filho do mesmo `Stack`. Numa `Row` (`HBox`),
irmãos pintam na ordem em que foram adicionados como filhos — não por profundidade/z na tela —
e como `contentArea` é adicionado DEPOIS da sidebar, ele pinta por cima da metade do círculo que
invade seu espaço, cobrindo-a.

**Decisão:** o `Stack` que embrulha o toggle subiu de nível — agora é `HomeScreen.render()` quem
monta `new Stack().children(Row(Sidebar+contentArea), Sidebar.toggleButton(viewModel))`, ou seja,
o toggle é o último filho de um `Stack` que envolve a `Row` INTEIRA (sidebar E contentArea
juntos). Assim ele pinta por cima dos dois, não só de um. `Sidebar.render()` voltou a devolver só
o corpo da sidebar (sem o toggle embutido); `Sidebar.toggleButton(viewModel)` virou público,
carregando a mesma lógica de ícone reativo (`<`/`>`) e tamanho fixo circular — só a alocação do
`Stack` que o hospeda mudou de dono.

**Posição:** trocado `Pos.CENTER_RIGHT` por `Pos.BOTTOM_LEFT` + `translateY(-30)` (ancora no fim
do Stack, sobe 30px) — o X continua reativo à largura atual da sidebar (64/160), recalculado a
cada mudança de `sidebarMinimizada`, então o botão sempre fica em cima da borda certa em qualquer
dos dois estados.

**Verificado numericamente** (diagnóstico jogável fora contra o `HomeScreen` de verdade): o
`StackPane` mais externo tem exatamente 2 filhos e o toggle é o último (`QTD_FILHOS_STACK=2`,
`TOGGLE_EH_ULTIMO_FILHO=true`) — condição que garante a ordem de pintura corrigida. Base do
Stack em y=620 (altura da janela), base do toggle em y=591 — 29px do fim (a diferença de 1px pro
30 pedido é o mesmo arredondamento de borda já visto no fix anterior, cosmético). X continua
entre 146 e 174, estradando a borda em x=160 como antes.

## 2026-08-18: Botão de expandir/encolher virou um círculo flutuando sobre a borda da sidebar

**Contexto:** pedido pra reposicionar o botão de minimizar/maximizar — em vez de ocupar uma
linha dentro da coluna de navegação, ele deveria "flutuar" sobre a borda direita da sidebar
(offset, sobreposto — a metáfora usada foi "z-index"), formato circular, e o ícone deveria
refletir o estado (`<` quando expandida, `>` quando encolhida) em vez de um ícone de hambúrguer
que gira 90°.

**Decisão:** `Sidebar.render()` agora envolve o corpo da sidebar (o `ScrollPane` de antes) e o
botão de toggle num `Stack` (`megalodonte.components.layout_components.Stack`, um `StackPane`
puro) — o corpo primeiro, o botão depois (`Stack` empilha por ordem de inserção, o último filho
fica por cima — é o "z-index" que foi pedido). O botão:
- Tamanho fixo 28×28 + `borderRadius` = metade do diâmetro → círculo.
- `StackPane.setAlignment(node, Pos.CENTER_RIGHT)` + `translateX(+14)` — alinha à borda direita
  do Stack e desloca meio diâmetro pra fora, ficando literalmente montado em cima da borda,
  metade dentro/metade fora.
- Ícone reativo (`ComputedState<IconInterface>`, mesmo padrão já usado nos itens de navegação):
  `Entypo.CHEVRON_LEFT` quando expandida, `CHEVRON_RIGHT` quando minimizada — sem animação de
  rotação (o ícone já muda de forma sozinho, não precisa girar).

**Verificado numericamente** (diagnóstico jogável fora, reproduzindo Sidebar dentro de um Row
igual a `HomeScreen` de verdade — a primeira tentativa, com a Sidebar como raiz direto da Scene,
deu um resultado enganoso porque o Stack esticava pra largura toda da janela; só fez sentido
depois de reproduzir o Row real): botão fica entre x=146 e x=174 quando a borda da sidebar está
em x=160 — estradando a borda como esperado; alternar `sidebarMinimizada` não lança exceção.

## 2026-08-18: Janela crescia sozinha além do tamanho declarado da rota, empurrando conteúdo pra fora da tela

**Contexto:** o fix anterior (ScrollPane na Sidebar) não resolveu de verdade — o usuário
observou, com precisão, que era o **conteúdo em foco** que empurrava o Logout, não o monitor em
si: telas com tabela cheia + botão (ex.: lista de Pesagem) jogavam tanto o "Criar novo" quanto o
Logout da Sidebar (uma tela vizinha, sem relação nenhuma com o conteúdo) pra fora da área visível.

**Investigação:** montei diagnósticos jogáveis fora sucessivos pra isolar a causa de verdade (em
vez de aplicar mais um fix especulativo em cima do anterior):
1. Reproduzi o esqueleto real de `HomeScreen` (Row com Sidebar + content) com uma lista sintética
   de 30 linhas — sozinho, isso não vazava (Row ficava exatamente do tamanho da Scene).
2. Reproduzi fielmente `ContratoTelaCrudV3.mainView()` (`Container.fillHeight()` -> `Show.when()`
   -> `ScrollPaneDefault`) — percebi que `Show.when()` nunca recebia `.fillHeight()`, então a
   página de lista/formulário trava a própria altura em `USE_PREF_SIZE` (não estica nem encolhe),
   quebrando a cadeia que faria o `ScrollPane` de dentro rolar de verdade.
3. O que realmente expôs o mecanismo: reproduzir a sequência real de dois passos — `Stage.show()`
   inicial com conteúdo curto (dashboard), depois trocar pra conteúdo alto **sem tocar no
   tamanho da janela** (exatamente o que `HomeScreenViewModel.navegarPara()` faz — só troca os
   filhos de `contentArea`). Resultado: a `Scene`/`Stage`, mesmo com `setWidth`/`setHeight`
   explícitos no show() inicial, **cresce sozinha** numa passada de layout posterior quando o
   conteúdo pede mais altura — porque a rota é `resizable=true` e nada nunca travava um teto.

**Causa raiz:** duas lacunas se somam:
- `ContratoTelaCrudV3.mainView()` não chamava `.fillHeight()` no `Show.when(...)` — sem isso, a
  página de lista/form nunca é forçada a caber no espaço real, então o `ScrollPaneDefault` que
  a envolve nunca é forçado a rolar (ele só cresce livremente).
- `ScreenContext.applyStageProps` (megalodonte-router) só chamava `stage.setWidth/setHeight` —
  nunca `setMaxWidth/setMaxHeight`. Numa rota `resizable=true` (o caso de `HOME`, que usa
  `MAX_WIDTH`/`MAX_HEIGHT` — nomes que já sugeriam um teto, nunca imposto de verdade), o JavaFX
  deixa a janela crescer sozinha além do tamanho declarado quando o conteúdo pede mais espaço.
  Numa tela com bastante altura sobrando isso passa despercebido; num monitor com menos altura
  disponível, a janela cresce além do que cabe fisicamente, empurrando qualquer coisa ancorada
  embaixo — Logout na Sidebar, "Criar novo" no content — pra fora da área visível.

**Decisão — corrigido nas duas pontas:**
1. `ContratoTelaCrudV3.mainView()`: `.fillHeight()` adicionado ao `Show.when(...)`.
2. `ScreenContext.applyStageProps` (megalodonte-router): agora também chama
   `stage.setMaxWidth(width)` / `stage.setMaxHeight(height)` com os mesmos valores do
   `setWidth`/`setHeight` — a janela continua podendo ser encolhida pelo usuário (`resizable`
   continua valendo pra isso), só não cresce mais sozinha além do que a rota declarou.

**Limitação desta investigação:** os diagnósticos confirmaram o mecanismo (janela crescendo
sozinha numa Stage resizable sem teto) mas, mesmo com `setMaxHeight` setado explicitamente no
teste sintético, a `Scene` sintética ainda reportava uma altura maior numa leitura imediata após
o layout — não fechei se isso é uma particularidade do ambiente de teste (sem gerenciador de
janela "de verdade" fora do X11 puro deste sandbox) ou se falta mais alguma reconciliação. Os
dois fixes acima são estruturalmente corretos e sem efeito colateral esperado (`fillHeight` faz o
scroll interno funcionar como já funciona em outras telas; `setMaxWidth/Height` só impede
crescimento além do declarado, nunca impede encolher) — mas não consegui confirmar visualmente o
resultado final (ver `AI_RULES.md`: sem teste visual). Se o sintoma persistir mesmo com os dois
fixes, o próximo passo é medir a altura real da `Stage` (não da `Scene`) logo após `navegarPara`
trocar pro conteúdo alto, no app rodando de verdade.

## 2026-08-18: Sidebar — item ativo com texto/ícone pretos, ícones alinhados, colunas ID mais estreitas

**Contexto:** três ajustes de polish pedidos juntos: (1) o item selecionado da sidebar (fundo
amarelo) tinha texto/ícone brancos — baixo contraste; (2) os ícones dos itens da sidebar não
ficavam alinhados verticalmente entre si; (3) a coluna "ID" das tabelas (Produto, Usuário,
Cliente, Pesagem) ocupava mais espaço do que um número de poucos dígitos precisa.

**Decisão — cores do item ativo:** `ButtonProps` (megalodonte-components) não tinha uma versão
reativa de `textColor` (só `bgColor(ReadableState<String>)` já existia, usado desde o fix
anterior de destacar a seção ativa). Adicionado `textColor(ReadableState<String>)` no mesmo
padrão do `bgColor` reativo — campo `textColorState`, aplicado em `bindStates()`, e o
`applyTheme()` estático só roda quando não há state reativo controlando. `Sidebar.botaoNav()`
agora computa `corComputada`/`iconeComputado` junto com o `bgComputado` que já existia, todos
dependendo do mesmo `selecionado` — preto quando ativo, branco quando não.

**Decisão — alinhamento dos ícones:** cada botão da sidebar tem texto de comprimento diferente
("Início" vs. "Usuários"), e o `Button` do JavaFX centraliza por padrão o grupo ícone+texto
dentro da largura do botão — como o grupo todo muda de largura conforme o texto, o ícone (que
fica à esquerda do texto) acaba num x diferente em cada botão. Corrigido setando `Pos.CENTER_LEFT`
direto no node do JavaFX (`Sidebar.alinharEsquerda()`) — ícone sempre começa no mesmo x,
independente de quanto texto vem depois.

**Decisão — coluna ID mais estreita:** `SimpleTable` já tinha um overload de `column(title,
extractor, maxWidth)` pronto (usado por `imageColumn`, nunca pela coluna ID). Com
`CONSTRAINED_RESIZE_POLICY` (já configurado em `SimpleTable`), colunas sem `maxWidth` dividem o
espaço restante proporcionalmente — bastou capar a coluna "ID" em 60px nas 4 telas (Produto,
Usuário, Cliente, Pesagem) pra ela parar de competir por espaço com colunas que precisam de mais
(Nome, Placa, etc.).

## 2026-08-18: Logout da sidebar ficava fora da área visível em monitores com DPI diferente

**Contexto:** usuário reportou que o botão de Logout "sumia" quando a seção Pesagem estava
selecionada — mas só num monitor específico; no notebook continuava visível no mesmo lugar. Isso
descartou de cara qualquer teoria ligada ao conteúdo da tela de Pesagem em si (cheguei a montar
um diagnóstico jogável fora reproduzindo o layout real da Sidebar+conteúdo com dados de verdade
pra medir a posição do botão — o conteúdo mais alto do Pesagem, sozinho, não empurrava nada pra
fora, então não era isso).

**Causa raiz:** `ScaleProvider` (megalodonte-base) detecta o fator de escala/DPI **uma única
vez**, olhando `Screen.getPrimary()`, e guarda num campo estático — nunca reavalia depois, mesmo
que a janela seja aberta ou movida pra um monitor diferente do que foi consultado no boot. Como
a altura da janela (`ScreenContext.applyStageProps`) e praticamente todo espaçamento/tamanho da
UI passam por `ScaleProvider.scale(...)`, um fator "errado" pro monitor onde a janela está de
fato sendo exibida deixa menos altura real disponível do que o esperado — e como o app é uma
janela única, fixa, sem esse recálculo por monitor, o conteúdo da sidebar (que cresce conforme
mais itens de navegação existem, ex.: quando "Usuários" aparece pra admin) pode ultrapassar a
altura de verdade disponível, empurrando o Logout (fixado embaixo via `SpacerVertical().fill()`)
pra fora da área visível — sem nenhum jeito de rolar até ele.

**Decisão:** corrigir a causa raiz (fazer `ScaleProvider` reavaliar por monitor/janela) é uma
mudança de framework mais ampla, que eu não consigo validar de verdade sem um setup
multi-monitor real. Em vez disso, apliquei uma correção defensiva e de baixo risco que resolve o
sintoma relatado (e qualquer variante futura da mesma classe de bug — janela pequena demais,
mais itens de sidebar adicionados depois, etc.): `Sidebar.render()` agora envolve a Column de
navegação num `Components.ScrollPaneDefault(...)`, com barra horizontal desligada — se o
conteúdo não couber na altura oferecida, rola em vez de estourar sem jeito de alcançar. Também
corrigido `Components.ScrollPaneDefault`: faltava `scroll.setMaxHeight(Double.MAX_VALUE)` — sem
isso, o ScrollPane só esticava até a altura do pai quando o pai era uma VBox (o `VBox.setVgrow`
já existente só vale nesse caso); como a Sidebar agora vive direto dentro de um `Row` (HBox), sem
esse `setMaxHeight` o ScrollPane ficaria travado na própria altura preferida em vez de ocupar o
espaço real que o Row oferece.

**Verificado numericamente** (não visualmente — ver `AI_RULES.md`): diagnóstico jogável fora que
força a Sidebar renderizada de verdade dentro de uma viewport de 220px (bem menor que os 334px
que o conteúdo precisa), rola até o fim (`vvalue=1.0`) e mede a posição do botão de Logout na
cena — resultado: `LOGOUT_ALCANCAVEL=true` (min/max Y do botão inteiramente dentro dos 220px da
cena). Sem a correção, esse mesmo teste teria conteúdo cortado sem scroll nenhum disponível.

## 2026-08-18: `Menu.textColor()` não pegava — `setFill()` direto perde pro estilo inline já aplicado

**Contexto:** ao escurecer a MenuBar (ver decisão de MenuBar/Sidebar mais abaixo), adicionei
`Menu.textColor(String)` fazendo `triggerLabel.setFill(Color.web(color))` — compilou, rodou sem
exceção, mas o usuário reportou que os títulos ("Gerencial", "Suporte") continuavam pretos.

**Causa:** `TextProps` (usado por `Text`, inclusive o `Text` interno do `Menu`) aplica cor via
`StyleUtils.applyStyleProperty(node, color, FX_FILL)` — ou seja, `node.setStyle("-fx-fill: ...")`,
um estilo inline. No CSS do JavaFX, um estilo inline (`setStyle`) tem prioridade mais alta que um
valor setado via `setFill()` direto; quando o motor de CSS reprocessa o node (no próximo pulse),
ele reaplica o `-fx-fill` do estilo inline por cima do `setFill()` que rodou antes — visualmente,
o `setFill()` nunca "gruda".

**Decisão:** `Menu.textColor()` corrigido pra usar o mesmo mecanismo (`StyleUtils.
updateTextColor(node, color)`, que também escreve em `-fx-fill` via estilo inline) em vez de
`setFill()` direto — agora sobrescreve de verdade o valor já presente no mesmo estilo inline, em
vez de competir com ele por fora. **Lição geral pra esse framework:** qualquer cor/estilo que já
é setado por um `Props.applyTheme()` (fill, background, border, etc.) só pode ser sobrescrito
depois via os helpers de `StyleUtils`, nunca via API JavaFX direta (`setFill`, `setStyle` cru
fora do merge de `setStyleProperty`) — misturar os dois mecanismos faz o inline sempre vencer.

## 2026-08-18: Tela de boas-vindas virou um dashboard real, navegável pela sidebar

**Contexto:** a tela que abria em `Secao.HOME` só mostrava "Balanças Gobitech" / "Sistema de
pesagem" — texto estático, sem link de volta na sidebar (não tinha como voltar pra ela depois de
sair). Pedido: virar um dashboard com totais (produtos, clientes, pesagens no total e pesagens
no mês) e um item "Início" na sidebar pra poder retornar.

**Decisão:** `Secao.HOME` deixou de ser um caso especial em `HomeScreenViewModel.navegarPara()`
(antes: `telaAtiva.set(null)`, sem `ScreenComponent` de verdade) e passou a instanciar
`DashboardScreen` igual qualquer outra seção — inclusive já na construção do
`HomeScreenViewModel` (antes o dashboard só existia via fallback em `HomeScreen.
renderConteudo()`; agora `telaAtiva` nunca é null). Isso também corrigiu de graça um descuido:
antes, sair da Home não chamava `onDestroy()`/cancelava scope de nada (não existia tela de
verdade); agora que a Home carrega dados via `Async`, ela precisa do mesmo ciclo de vida
onMount/onDestroy das demais, e já ganha isso automaticamente por reusar o mesmo
`destruirTelaAtual()`.

Números exibidos: `ProdutoService.listar().size()`, `ClienteService.listar().size()` e
`PesagemService.listar().size()` (contagem simples em memória — sem `COUNT(*)` dedicado, volume
de dados desse app não justifica) — mostram exatamente a mesma contagem que cada tela de listagem
já usa, pra não haver "o dashboard diz um número, a lista mostra outro". Pesagens do mês reusa
`PesagemService.filtrar(...)` (o mesmo método do filtro de pesagens) passando o primeiro dia do
mês atual como início — apesar de anexar relações desnecessariamente pra um mero count, o volume
mensal é baixo o bastante pra não valer a pena criar um método novo só pra isso.

## 2026-08-18: Usuários não-admin não têm acesso à tela de Usuários

**Contexto:** usuário pediu que quem não é admin não possa manipular outros usuários.

**Decisão:** mesmo padrão já usado pra "Gerar licença" (`SessaoUsuario.isAdmin()`): o item
"Usuários" nem aparece na sidebar pra quem não é admin (`Sidebar.botaoNav`, condicional em
`Sidebar.render()`). Como `HomeScreenViewModel.navegarPara(Secao.USUARIOS)` só é chamado a
partir desse botão (nenhum outro caminho de navegação leva lá), esconder o item já é suficiente
— não há uma segunda porta de entrada pra guardar. Opção mais restritiva descartada
(permitir editar a si mesmo, bloquear só os outros) — usuário preferiu a tela inteira oculta,
mais simples e consistente com o precedente já existente.

## 2026-08-18: Sidebar — logo, item ativo destacado e ícone de minimizar rotaciona

**Contexto:** pedido de polish visual: logo no topo (versão quadrada quando comprimida), só a
seção ativa destacada com fundo amarelo, e o ícone de minimizar/maximizar girando 90° para
indicar o estado (deitado = expandida, em pé = comprimida).

**Decisão:** `Sidebar.render()` agora recebe o `HomeScreenViewModel` inteiro (em vez de uma
lista de `Runnable`s soltos) — precisa ler `secaoAtiva` pra saber qual botão destacar.
- Logo: `Show.when(minimizada, ...)` alternando entre `/assets/app_banner_square.png` e
  `/assets/app_banner.png` — ambos já existiam como assets, não foram criados agora.
- Item ativo: `ButtonProps.bgColor(ReadableState<String>)` (já existia, não usada antes na
  sidebar) com um `ComputedState` que compara `secaoAtiva` com a seção do botão — amarelo
  (`ThemeManager.theme().colors().primary()`) quando bate, `transparent` quando não. Ícone e
  texto continuam brancos nos dois estados (só o fundo muda) — é o que foi pedido, não friso
  visualmente com a cor do ícone em cima do amarelo.
- Rotação: `Animations.rotate(Node, from, to, Duration)` (já existia em `megalodonte-base`,
  não foi criada agora) aplicada diretamente no nó do ícone do botão de toggle, disparada no
  `subscribe` de `sidebarMinimizada` — gira sempre do ângulo atual pro alvo (0° ou 90°), não
  reseta a animação a cada clique.

## 2026-08-18: NullPointerException ao abrir Pesagem — seed de `conexao_balanca` incompleta

**Contexto:** usuário relatou `NullPointerException` em `ConexaoBalancaModel.getBaudRate()` ao
abrir a tela de Pesagem. Causa: `V10__dados_padrao.sql` inseria uma linha em `conexao_balanca`
com `tipo_conexao='Serial'` mas sem `porta_com`/`baud_rate` — algo que `ConexaoBalancaService.
validarCampos()` nunca deixaria salvar pela tela (exige os dois campos pra Serial), mas a seed
via SQL bruto passava direto por essa validação. `LeitorBalancaFactory.criar()` desembrulhava
`Integer baudRate` pra `int` sem checar null primeiro.

**Decisão:** (1) `LeitorBalancaFactory.criar()` agora trata config incompleta (porta/baud ou
IP/porta faltando) igual a "não configurada" — mensagem amigável em vez de NPE, defesa em
profundidade independente de como uma config incompleta chegue ao banco. (2) `V10` editada pra
não inserir mais essa linha — `ConexaoBalancaService.salvarOuAtualizar()` já cria a linha na
primeira vez que o usuário salva pela tela. (3) Banco já existente: removida cirurgicamente só a
linha incompleta (`DELETE` com `WHERE` explícito nos campos nulos) — dados reais de
clientes/produtos/usuários já cadastrados ficaram intactos.

**Testado:** reproduzi a config exata quebrada (`Serial` sem porta/baud) num teste descartável —
confirma mensagem amigável em vez de NPE. 155/155 testes, app sobe sem exceção.

---

## 2026-08-18: Edição de Cliente/Produto/Usuário/Pesagem não refletia na lista (bug no framework)

**Contexto:** usuário relatou "a atualização de cliente não está refletindo na UI quando retorno
pra ela". Achado a causa: `ListState.set()` (em `megalodonte-reactivity`) usava
`Objects.equals(listaAntiga, listaNova)` — comparação por **conteúdo** — pra decidir se notifica
os listeners. `updateIf()` (usado por todo `handleAddOrUpdate()` de edição) muta o objeto já
presente na lista e devolve a mesma referência — a "lista nova" tem os mesmos objetos da antiga,
então a comparação por conteúdo achava "igual" e cancelava a notificação, mesmo os campos tendo
mudado de verdade. A tabela só mostrava o dado certo depois de sair e voltar pra seção (refetch
completo do banco cria objetos novos, aí sim diferentes por identidade).

**Decisão:** corrigido na raiz, no `megalodonte-libs` (`megalodonte-reactivity`) — guarda trocada
de comparação por conteúdo pra identidade da lista (`==`). Não é um bug deste app especificamente,
afeta qualquer tela que edite um item já carregado na lista. Ver `DECISIONS.md`/`TODO.md` do
`megalodonte-libs` pro relato completo e o teste que prova o cenário
(`ListStateUpdateIfBugTest`).

**Testado:** `megalodonte-reactivity` republicado em `mavenLocal`;
`--refresh-dependencies compileJava test` neste projeto: **155/155, sem regressão**.

---

## 2026-08-18: Campo `desconto` do produto (previsto no DER original, nunca implementado)

**Contexto:** conferindo o Anexo II (Diagrama Entidade Relacionamento) da documentação original
da DGB Tecnologia, a entidade `products` sempre teve um campo `discount float(10,2)` — nunca
implementado nem no app antigo entregue nem nesta reescrita (o app antigo tinha desconto só por
pesagem, não por produto).

**Decisão:** `ProdutoModel.desconto` (`BigDecimal`, migration `V12__add_desconto_produtos.sql`,
`ALTER TABLE` — não editei uma migration antiga porque já existem dados reais no banco). Ao
selecionar um produto na tela de Pesagem, o desconto padrão dele carrega automaticamente no campo
**"Outros"** (um dos 8 campos de desconto já existentes — não criamos um 9º campo nem um cálculo
paralelo). O operador pode editar livremente depois — é só um ponto de partida. Editar/clonar uma
pesagem já existente não é afetado: `populateFieldsFromModel()` seta `produtoSelected` antes de
carregar o desconto realmente salvo, então o valor real sempre sobrescreve o auto-preenchimento
(ordem documentada em comentário no código — não inverter).

**Testado:** `./gradlew test`: 155/155. Migration validada rodando contra o banco real (dado
existente do produto "Soja" preservado, `desconto` veio com o default `0`).

---

**Contexto:** usuário pediu que o admin padrão fosse criado com login/senha já cifrados
(forneceu os valores prontos) e que login/senha fiquem **sempre criptografados** ao salvar,
não só o admin. Investigando a fronteira certa pra isso, achei um bug real e já existente:
`UsuarioService.autenticar()` tentava **decriptar** os parâmetros de entrada (como se já
chegassem cifrados), mas `AuthScreenViewModel.entrar()` sempre mandou texto puro — e a
comparação de senha comparava a variável errada. Rodando os testes antes de mexer: **4 dos 12
testes de `UsuarioServiceTest` já estavam falhando** (`RuntimeException: Illegal base64
character`/`Last unit does not have enough valid bits`) — login nunca funcionou de verdade
depois da última reescrita do `AuthScreenViewModel`.

**Decisão — onde a criptografia mora:** só em `UsuarioService` (usa o `CryptoManager` já
existente, AES/ECB, mesma chave fixa usada pro token do Telegram). `UsuarioRepository` e a
`UsuarioModel` continuam sem saber de criptografia nenhuma — só leem/gravam o que já está no
banco. Como AES/ECB é determinístico, dá pra **buscar e comparar por igualdade de texto cifrado**
sem nunca precisar decriptar a senha armazenada:
- `salvar()`/`atualizar()`: criptografam login/senha antes de persistir, restauram o texto puro
  no objeto do chamador logo depois (em `finally`) — quem chama (ViewModel/tela) nunca vê nem
  precisa saber do texto cifrado.
- `autenticar(login, senha)`: recebe texto puro (como sempre foi chamado), criptografa
  internamente pra buscar/comparar. Corrige o bug acima de quebra.
- `listarAtivos()`/`buscarPorLogin()`: decriptam antes de devolver, pra telas de listagem/edição
  (`UsuarioScreen`) continuarem mostrando login legível.
- Verificação de "login já em uso" agora compara o texto **cifrado** (o que de fato está
  gravado), não mais o texto puro contra uma coluna cifrada (o que nunca teria batido).

**Admin padrão** (`V10__dados_padrao.sql`): login/senha trocados pros valores cifrados fornecidos
pelo usuário (confirmado rodando o `CryptoManager` de verdade antes de gravar — as credenciais
reais em si não ficam documentadas aqui, mesma chave AES/ECB fixa já usada em outro lugar do app
torna o texto cifrado equivalente a texto puro pra quem tem acesso ao repositório). `nome`
trocado de "Gestor" pra "André".

**Banco local apagado** (`~/.gobitech/erp.db`) — autorizado explicitamente pelo usuário, app
ainda não está em produção. Sem isso o Flyway não teria como re-popular o admin (já tinha uma
linha antiga com login `'gestor'` em texto puro aplicada). Migration `V10` editada **diretamente**
em vez de nova migration — seguro porque o banco foi apagado por completo (sem checksum
conflitante em `flyway_schema_history`).

**Testado:** `./gradlew test` — 155/155, incluindo os 4 que antes falhavam. Além disso, um teste
descartável (criado e removido depois, seguindo o mesmo padrão usado pra validar a V11) rodou o
Flyway do zero contra o caminho real do banco e confirmou de ponta a ponta: (1) a coluna
`login`/`senha` no banco fica com o texto **exatamente cifrado** fornecido, não texto puro;
(2) `autenticar(...)` com as credenciais reais do admin autentica; (3) senha errada e login
inexistente retornam `null`; (4) o modelo retornado por `autenticar()` traz login/senha em texto
puro pro resto do app usar normalmente.

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
