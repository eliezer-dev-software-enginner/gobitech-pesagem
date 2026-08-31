# TODO

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

## Pendente — branding/empacotamento (herdado do plics-sw, ainda não trocado)
- [ ] `Main.APP_NAME`/`BASE_TITLE` ainda dizem "Plics SW" — trocar quando for empacotar de
      verdade pro cliente
- [ ] `gradle.properties`, `scripts/*.py` (MSI/DEB/Flatpak) ainda referenciam Plics SW —
      revisar antes de gerar o instalador final
- [ ] "Buscar atualização" foi removido da Home porque `my_app.infra.UpdaterService` não existe
      nesta cópia do projeto — se for reintroduzir, é trabalho de infraestrutura de release, não
      de tela

