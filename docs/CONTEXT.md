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
| Preferências | `preferencias` | `ConfiguracoesScreen` (Gerencial → Configurações; tipo de impressão) |
| Licença | `licensas` | `LicensaScreen` (só visível/acessível pra usuário admin) |
| Conexão da balança | `conexao_balanca` | `ConexaoBalancaScreen` (Serial ou TCP) |

**Fora da Fase 1, adiado pra Fase 2**: câmera Intelbras — **já implementada** (tabela
`conexao_camera` criada via V13 + `ConexaoCameraScreen`/`ConexaoCameraViewModel`), mas o item de
menu "Conexão das câmeras" está **comentado** em `HomeScreen.java:74` **por decisão do usuário**
(pendência M3 da vistoria, "decidido: manter" — a câmera entra no fluxo só na Fase 2/uso real),
deixando a tela inalcançável pela UI. Ver `/home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/ENTREGAS.md`.

## Estado atual (2026-09-24)

- **Filtro por data não é sobrescrito (24/09)**: a carga inicial do histórico e a aplicação do
  filtro rodam em segundo plano. Cada consulta agora recebe uma sequência; somente a mais recente
  pode atualizar a tabela, impedindo que a carga completa termine depois e substitua um resultado
  filtrado (inclusive `De` e `Até` iguais a hoje).

- **Exportação do histórico espelha a UI (24/09)**: o relatório PDF deixou de agrupar uma
  Entrada com sua Saída vinculada. Cada registro presente na lista filtrada gera uma linha no PDF;
  assim, por exemplo, cinco resultados para a placa `RED9I24` passam a gerar cinco linhas.

- **Campos de pesagem por etapa (24/09)**: os inputs do formulário passaram de **Tara** e
  **Peso bruto** para **Entrada (Kg)** e **Saída (Kg)**, sem migration ou mudança nos dados
  persistidos. Na tela de Entrada, só Entrada pode ser capturada; Saída fica somente leitura.
  Na tela de Saída, Entrada vem da pesagem vinculada e fica somente leitura; só Saída pode
  ser capturada. O líquido agora usa `|saída - entrada|`, aceitando tanto carregamento quanto
  descarregamento (ex.: Entrada 4000, Saída 2000 = líquido 2000). Suíte: **268 testes, 0 falhas**.

## Estado atual (2026-09-21)

- **SimpleTable com todos os dados das telas (21/09)**: a pedido do usuário (mesmo ajuste já feito
  no `plics-sw`), as `SimpleTable` ganharam todas as colunas dos modelos/visões de detalhe e as
  larguras fixas (ID `60.0` de `ClienteScreen`/`UsuarioScreen`) foram removidas. `ClienteScreen`
  agora mostra endereço completo (UF, CEP formatado, Cidade, Bairro, Rua, Número), Complemento e
  Ativo; `ProdutoScreen` ganhou Ativo e Observações; `UsuarioScreen` ganhou Telefone (formatado) e
  Ativo; `PesagemHistoricoScreen` ganhou Documento do motorista, Nota fiscal, Tara, Peso bruto e
  Observações + `.horizontalScroll()` (que os CRUD já recebiam via `ContratoTelaCrudV3.listPage`);
  `LicensaScreen` ganhou Situação (Válida/Expirada). `formatCep`/`formatPhone` com null-guard nos
  novos campos. Suíte: **271 testes, 0 falhas**.

## Estado atual (2026-09-19)

- **Descontos que só exibem % não descontam do peso (19/09)**: Avariados, Ardidos, Impurezas e
  Umidade passaram a ser **só-exibição** — aparecem no ticket (A4 e térmica) com o percentual
  aplicado, mas **não reduzem o peso**. Só descontam de fato: **Quebra ardidos, Quebra impurezas,
  Quebra umidade e Outros**. `TicketPesagemDados` ganhou flag `desconta` no `Desconto` (quilos=0
  pros só-exibição) e `totalDescontado()` soma só os que descontam — quando só há só-exibição, o
  ticket A4/térmico mostra "Total descontado: -" e "-" na coluna Total (Kg). O líquido final
  salvo (`PesagemFormViewModel.calcLiquido`) desconta só os 4 tipos que descontam; a validação
  "soma dos descontos > 100%" continua somando os 8 (decisão do usuário). Suíte: **271 testes,
  0 falhas**.
- **Ticket térmico só com descontos aplicados (19/09)**: a nota térmica imprimia os **8 tipos de
  desconto sempre**, inclusive os zerados (Avariados, Ardidos, Impurezas, etc.). Corrigido em
  `TicketThermalExporter.montarLinhas` seguindo o mesmo padrão que o A4 já usava
  (`TicketPdfExporter`): filtra `percentual().signum() != 0`; sem nenhum desconto aplicado, saí
  "Nenhum desconto aplicado." e a linha "Total descontado" é omitida. Preencher só "Quebra
  umidade" agora imprime somente esse tipo. Suíte: **266 testes, 0 falhas**. (depois disso,
  suíte passou a 271 testes)
- **Assinaturas da térmica espaçadas com linha acima (19/09)**: "ADMINISTRADOR"/"MOTORISTA"
  saíam colados (o `padEsq`/`padDir` de 33 chars estourava a bobina de 32). Agora cada nome tem
  sua linha de sublinhado acima e os nomes saem espaçados, como no A4 — novo helper
  `assinaturas()` + `LARGURA_BOBINA`, `padEsq`/`padDir` removidos. Suíte: **267 testes, 0 falhas**.

## Estado atual (2026-09-17)

- **Logomarca horizontal nas Configurações (17/09)**: `preferencias.imagem_horizontal` (V21)
  + campo no `PreferenciasModel` + `ImageSelector` em Gerencial → Configurações salva e exibe a
  logomarca escolhida. Erros da implementação inicial corrigidos: migration `NOT NULL` sem
  `DEFAULT` (SQLite recusa `ADD COLUMN` assim em tabela com dados — quebrava o boot),
  `salvar()` fazia INSERT na linha singleton já existente (agora `salvarConfiguracoes` cria ou
  atualiza) e NPEs sem null-check. Ver `DECISIONS.md` 2026-09-17.
- **Botão "Remover logomarca"**: pedido do usuário — o cliente pode querer remover a logomarca
  horizontal. `limparLogo()` limpa o state (persiste `""` ao salvar); quando a imagem está vazia
  a prévia mostra o ícone `Entypo.FOLDER_IMAGES` (placeholder) em vez de um espaço vazio — o
  componente `Image` do Megalodonte renderiza nada pra fonte vazia.
- **Conexão única com a balança (`BalancaService` singleton, 17/09)**: a Pesagem estava com
  "Balança não conectada" depois que a Dashboard ganhou peso ao vivo — cada ViewModel abria uma
  conexão própria com a balança e a Dashboard (nunca destruída, navegação em janela própria via
  `spawnWindow`) vencia a corrida; no Serial a porta COM é exclusiva e no TCP o conversor aceita
  um cliente só → a Pesagem ficava sem conexão. Novo `my_app/infra/balanca/BalancaService.java`
  mantém a **única** conexão e expõe `pesoAoVivo()`/`lendoBalanca()` compartilhados;
  `DashboardViewModel` e `PesagemFormViewModel` delegam a ele. Fecha no encerramento
  (`Main.handleClose`) e troca de config (`reconectar()`). Ver `DECISIONS.md` 2026-09-17.

## Estado atual (2026-09-15)

- **Impressão configurável**: nova `ConfiguracoesScreen` + `ConfiguracoesViewModel`, acessível
  em Gerencial → Configurações. `preferencias.tipo_impressao` (V20) persiste `laser` (padrão)
  ou `termica`, preservando as preferências existentes.
- Histórico e detalhes têm um único **Imprimir**. `ImpressaoTicketService` consulta a escolha
  salva a cada envio; janelas já abertas também recebem a mudança. Ambos os tipos usam a
  impressora padrão do sistema: laser imprime o layout PDF A4 de duas vias via `PrinterJob`;
  térmica mantém ESC/POS 80 mm. Operação assíncrona, com bloqueio de clique repetido.
- **Pendências de impressão implementadas (15/09)**: **Salvar e imprimir** nos quatro
  formulários; Fornecedor removido; tickets com oito descontos (% e Kg), total descontado e
  líquido final salvo. Peso de entrada usa o bruto registrado ou a tara quando só ela existe.
- **Layout A4 conforme a foto do André (15/09)**: fonte Helvetica, dados à esquerda,
  tabela **Descontos aplicados ao produto** à direita, pesos/datas/placa em negrito, título
  sublinhado e assinaturas Operador/Motorista. A4 mostra só descontos com percentual não zero;
  sem descontos exibe mensagem. Fornecedor continua removido por pedido anterior.
- `% Classificado` aparece como **—**, pois não há campo/regra independente no modelo atual;
  `% Aplicado` e Kg usam os descontos salvos. Nenhuma mudança de cálculo nesta revisão visual.
- Regra confirmada: cada desconto incide sobre o **líquido inicial (bruto − tara)**;
  **líquido final** é o peso líquido salvo, após a soma dos descontos. Os dois tickets
  identificam explicitamente o líquido inicial. Validação específica: 22 testes passaram.
- `TicketPesagemDados` compartilha pesos/datas/descontos entre laser e térmica.
  `RelatorioPesagemDados` monta pares/eventos sem UI; saídas sem vínculo preenchem a coluna
  Saída, avulsas/manuais ambas as datas com o horário do registro. Entradas vinculadas fora do
  filtro são buscadas em lote apenas para complementar a linha da saída.
- PDF A4 tem duas vias em áreas fixas, campos ajustados à largura e observações com quebra;
  exemplo da referência em `build/reports/printing/ticket-layout-andre.pdf`.
  Validação do fluxo pela UI e impressão física
  pendentes (M24/M31), sem alterar banco de produção nesta tarefa.
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
- **Fix (2026-09-08)**: `EmpresaViewModel.handleSave` descartava `e.getMessage()` e exibia uma
  mensagem genérica fixa ao salvar a empresa — o `IllegalArgumentException` de validação (ex.:
  "Telefone inválido (informe DDD + Número)") chegava à exceção, mas o `catch (Exception e)`
  mostrava "Não foi possível salvar os dados da empresa. Tente novamente.". Era o único CRUD VM
  que o padrão M7 tinha deixado de fora (só o `fetchData` havia sido ajustado). Corrigido com o
  mesmo padrão dos demais (ver **Histórico** 2026-09-08). Degustado: varredura de todos os
  ViewModels/Services/telas confirmou que os demais já seguem o padrão correto.
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
- Testes (2026-09-15): **264 executados, 0 falhas, 0 ignorados** → `gradlew.bat test --offline`
  com JDK 25 → **BUILD SUCCESSFUL** (inclui preferências, migração, pesagens, descontos,
  pares/datas do relatório, limites do PDF e impressão simulada). Prévia A4 conferida visualmente.

## Histórico

### 2026-09-19 — Descontos que só exibem % não descontam do peso
- Pedido do usuário: alguns descontos (Avariados, Ardidos, Impurezas, Umidade) são só **exibição**
  de percentual no ticket — não devem reduzir o peso. Os que descontam de fato são **Quebra
  ardidos, Quebra impurezas, Quebra umidade e Outros**.
- **Implementado**: `TicketPesagemDados.Desconto` ganhou a flag `desconta`; `descontos()` marca
  os válidos e zera `quilos` dos só-exibição; `totalDescontado()` soma apenas os que descontam.
  A4 e térmica mostram "-" na coluna Total (Kg) / linha do desconto e "Total descontado: -"
  quando nenhum desconto de fato se aplica. `PesagemFormViewModel` ganhou
  `somaDescontosQueDescontam()`, usada no `calcLiquido()` e no `liquidoNegativo`; a validação
  "soma > 100%" continua com os 8 tipos (decisão do usuário).
- Testes: `descontosSoloExibicaoNaoDescontamDoTotal`, `descontosQueDescontamSaoMarcadosComoTal`,
  `descontosSoloExibicaoMostramPercentualSemKgNaTermica`, `descontosSoloExibicaoMostramPercentualSemKgNoA4`;
  casos existentes atualizados (Ardidos/Impurezas dos testes trocados por Quebra *). Suíte:
  `./gradlew test --offline` → **271 testes, 0 falhas** (JDK 25).

### 2026-09-19 — Fix: assinaturas do ticket térmico saindo coladas
- Sintoma: no rodapé da térmica, "ADMINISTRADOR" e "MOTORISTA" saíam um em cima do outro/colados
  em vez de espaçados com a linha de assinatura acima de cada nome (como no A4). Causa: a linha
  usava `padEsq` (%-33s) + `padDir` (%33s) = 66 caracteres, estourando a bobina de **32 colunas**
  (o `_underline` quebrado na impressão).
- **Corrigido**: novo helper `assinaturas()` monta uma linha de sublinhados (`_`) alinhada acima
  de cada nome, com os nomes espaçados dentro da largura da bobina (`ADMINISTRADOR` à esquerda,
  `MOTORISTA` à direita; usado `LARGURA_BOBINA = 32`). `padEsq`/`padDir` removidos.
- Testes: +1 caso (`assinaturasComLinhaAcimaDeCadaNome`). Suíte: `./gradlew test --offline` →
  **267 testes, 0 falhas** (JDK 25).

### 2026-09-19 — Fix: ticket térmico imprimia todos os descontos
- Sintoma: a nota térmica (80mm) saía com os **8 descontos sempre** (Avariados, Ardidos,
  Quebra ardidos, Impurezas, Quebra impurezas, Umidade, Quebra umidade, Outros), mesmo os que o
  operador não preencheu (0%). O layout A4 já só mostrava os aplicados
  (`TicketPdfExporter.java:99`, `filter(d -> d.percentual().signum() != 0)`), mas a térmica
  iterava `TicketPesagemDados.descontos()` sem nenhum filtro (`TicketThermalExporter.java:101`).
- **Corrigido**: `montarLinhas` filtra os descontos aplicados (mesmo critério do A4); quando não
  há nenhum, imprime "Nenhum desconto aplicado." e omite a linha "Total descontado" (consistente
  com o A4, que também não imprime total sem descontos).
- Testes: +2 casos novos em `TicketThermalExporterTest` (só aplicados / mensagem quando nenhum).
  Suíte: `./gradlew test --offline` → **266 testes, 0 falhas** (JDK 25).

### 2026-09-17 — Fix: logomarca horizontal nas Configurações (erros da implementação do usuário)
- O usuário adicionou save/carregar de logomarca horizontal (V21 + `imagem_horizontal` +
  `ImageSelector`). Na conferência: (1) migration `ADD COLUMN ... NOT NULL` sem `DEFAULT`
  violava o SQLite em tabela com dados (seed V10) → app não subia; (2) `ConfiguracoesViewModel.
  salvar()` fazia `session().insert()` na linha singleton já existente → duplicação/PK; (3)
  acessos sem null-check (`getPreferencias()`, `preferenciasModelState.get()`).
- **Corrigido**: V21 com `NOT NULL DEFAULT ''`; novo `PreferenciasService.salvarConfiguracoes
  (tipo, imagem)` cria/atualiza a linha única (normaliza null→``) e `salvarTipoImpressao`
  restaurado (preserva a imagem, usado nos testes); `PreferenciasModel` com campo default `""`;
  `ConfiguracoesViewModel` sem model mutável, salva via serviço; null-check em
  `getImagemHorizontalLogo()`.
- Testes: `./gradlew test --offline` → **264 testes, 0 falhas** (JDK 25).

### 2026-09-17 — Fix: "Balança não conectada" na Pesagem — conexão única com a balança
- Sintoma pós-refatoração da Dashboard (peso ao vivo): abrir a tela de Pesagem e clicar em
  Capturar Tara/Bruto dava "Balança não conectada". Causa: a Dashboard (janela principal, nunca
  destruída) e a Pesagem (aberta em janela própria via `spawnWindow`) cada uma abria sua própria
  conexão com a balança; a Dashboard ganhava a corrida e a Pesagem ficava sem conexão.
- **Corrigido**: `BalancaService` singleton (`my_app/infra/balanca`) mantém a conexão única e os
  `State` `pesoAoVivo`/`lendoBalanca` compartilhados. `DashboardViewModel` e
  `PesagemFormViewModel` removidos os campos/leitores próprios e agora delegam ao singleton.
  `ConexaoBalancaViewModel` chama `reconectar()` após salvar; `Main.handleClose()` chama
  `parar()` antes de fechar as sessões do DB. Em erro de leitura o leitor é descartado pra
  reconexão na próxima montagem de tela.
- Testes: `./gradlew test --offline` → **264 testes, 0 falhas** (JDK 25).
- Validação manual pendente com o simulador `scripts/simular_balanca_tcp.py`: Dashboard exibindo
  peso ao vivo + Pesagem capturando tara/bruto sem erro.

### 2026-09-08 — Fix: `EmpresaViewModel.handleSave` descartava a mensagem de validação
- Bug real reportado: salvar a empresa com telefone inválido mostrava a mensagem genérica "Não
  foi possível salvar os dados da empresa. Tente novamente." em vez de "Telefone inválido
  (informe DDD + Número)". Causa raiz: no `handleSave`, o `catch (Exception e)` capturava o
  `IllegalArgumentException` lançado por `EmpresaService.validarCampos()`/`Validacoes.validarTelefone`
  mas **descartava `e.getMessage()`** e exibia uma string fixa. O `fetchData` da mesma classe já
  tinha sido ajustado na rodada M7, mas o `handleSave` tinha ficado de fora.
- **Corrigido**: `catch (IllegalArgumentException e)` → exibe `e.getMessage()`; `catch (Exception e)`
  genérico mantido como fallback (mensagem amigável fixa) pras exceções realmente inesperadas
  (SQL, NPE, infra). Mesmo padrão já usado por `ClienteViewModel`/`UsuarioScreenViewModel`/
  `ProdutoScreenViewModel`/`ConexaoBalancaViewModel`/`ConexaoCameraViewModel`/
  `PesagemFormViewModel`.
- **Varredura completa**: todos os ViewModels, Services e telas revisados em busca do mesmo
  padrão — nenhum outro caso real encontrado (os demais já seguem o padrão M7). `LicensaViewModel.
  gerar()` tem um catch genérico, mas o `LicensaService.gerarNova` gera UUID randômico (colisão/
  valor-blank em prática impossíveis) → **mantido como está por decisão do usuário**.

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
