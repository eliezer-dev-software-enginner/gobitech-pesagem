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

**Fora da Fase 1, adiado pra Fase 2**: câmera Intelbras (`camera_settings` — tabela nem foi
criada ainda). Ver `/home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/ENTREGAS.md`.

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
