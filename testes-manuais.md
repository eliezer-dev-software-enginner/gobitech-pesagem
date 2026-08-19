# Testes Manuais — Balanças Gobitech

Casos de teste manual para o sistema de pesagem. Diferente do `plics-sw` (de onde esse projeto
começou como cópia), não existem "perfis de negócio" diferentes aqui — é um sistema único, então
todos os casos ficam neste arquivo só.

## Como usar
Rode `./gradlew run` (ou `python3 dev.py` pra hot-reload), siga o cenário, compare o efeito
observado com o esperado e anote o resultado.

## Legenda
- **OK**: Funcionou conforme esperado
- **PENDENTE**: Aguardando teste
- **ERRO**: Comportamento inesperado (detalhar na coluna Observação)

---

## 1. AuthScreen (login)

Login padrão do admin (seed `V10__dados_padrao.sql`): peça as credenciais reais de quem
administra o ambiente — não ficam documentadas aqui (nem em nenhum outro lugar do repo).

| # | Cenário | Login | Senha | Efeito Esperado | Resultado |
|---|---------|-------|-------|------------------|-----------|
| 1 | Login válido (admin) | (credencial real do admin) | (credencial real do admin) | Entra, popup "Seja bem-vindo(a), André!", vai pra Home | |
| 2 | Senha errada | (login real do admin) | qualquer senha errada | Alerta "Login ou senha inválidos". Não entra. | |
| 3 | Login inexistente | `naoexiste@x.com` | `12345` | Alerta "Login ou senha inválidos". Não entra. | |
| 4 | Campos vazios | (vazio) | (vazio) | Alerta "Informe login e senha". Não tenta autenticar. | |
| 5 | Usuário não-admin com licença vencida ou ausente | login de um usuário comum | senha dele | Alerta "Licença expirada. Contate o administrador para gerar uma nova." Não entra. | ok |
| 6 | Usuário não-admin com licença válida | login de um usuário comum | senha dele | Entra normalmente. | |
| 7 | Admin com licença vencida ou ausente | (credencial real do admin) | (credencial real do admin) | **Entra normalmente mesmo assim** — admin nunca fica bloqueado (senão não teria como gerar licença nova). | |
| 8 | Primeiro acesso | (banco recém-criado) | — | Depois do primeiro login com sucesso, `primeiro_acesso` vira `0` no banco (`preferencias`). | |

---

## 2. Navegação (Sidebar + fluxo Lista/Formulário)

Sidebar fixa à esquerda, mesmos 5 itens do app original: Pesagem, Produto, Cliente, Usuários,
Logout. Empresa/Conexão da balança/Gerar licença/Suporte ficam no menu do topo.

| # | Cenário | Efeito Esperado | Resultado |
|---|---------|------------------|-----------|
| 9 | Clicar em cada item da sidebar | Troca o conteúdo à direita sem abrir janela nova; sidebar continua visível | |
| 10 | Entrar numa seção (ex.: Produtos) | Mostra a lista (busca + tabela), **sem** barra de menu no topo | |
| 11 | Clicar em "+ Criar novo" (embaixo da tabela) | Navega pra página só com o formulário, campos vazios | |
| 12 | Dentro do formulário, clicar em "< Voltar" (embaixo) | Volta pra lista, formulário descartado (sem salvar) | |
| 13 | Preencher e salvar com sucesso | Volta pra lista automaticamente, item novo aparece na tabela | |
| 14 | Duplo-clique numa linha da tabela | Abre modal "Detalhes" com os dados + botões Editar/Clonar/Excluir | |
| 15 | No modal, clicar "Editar" | Modal fecha, navega pro formulário já preenchido com os dados do item, salvar faz `atualizar` | |
| 16 | No modal, clicar "Clonar" | Modal fecha, navega pro formulário preenchido mas em modo "novo" (salvar cria outro registro) | |
| 17 | No modal, clicar "Excluir" | Modal fecha, mostra confirmação; confirmando, inativa/remove o item e some da lista | |
| 18 | Trocar de seção com o formulário aberto (ex.: editando um Produto, clica em "Cliente" na sidebar) | Sai do formulário sem perguntar (perde o que não foi salvo) e mostra a lista de Clientes | |
| 19 | Campo de busca na lista | Filtra a tabela conforme digita | |
| 20 | Clicar em "Logout" na sidebar | Confirmação "Deseja realmente sair?"; confirmando, volta pra tela de login | |

---

## 3. ClienteScreen

Campos: Loja*, Razão social*, CPF/CNPJ, Telefone, Endereço (CEP/UF/Cidade/Bairro/Rua/Número),
Complemento.

| # | Cenário | Loja | Razão social | Efeito Esperado | Resultado                                                                                        |
|---|---------|------|---------------|------------------|--------------------------------------------------------------------------------------------------|
| 21 | Cadastro válido | Fazenda Santa Rita | Santa Rita Agropecuária Ltda | Salvo com sucesso. Aparece na tabela. | ok                                                                                               |
| 22 | Loja vazia | (vazio) | Qualquer | Alerta "Loja é obrigatória". Não salva. | ok                                                                                               |
| 23 | Razão social vazia | Qualquer | (vazio) | Alerta "Razão social é obrigatória". Não salva. | ok                                                                                               |
| 24 | Loja duplicada | (nome já cadastrado) | Outra razão social | Alerta "Já existe um cliente cadastrado com essa loja". | ok                                                                                               |
| 25 | CPF/CNPJ duplicado (loja diferente) | Nome novo | Razão nova, mesmo CPF/CNPJ de outro cliente | Alerta "CPF/CNPJ já cadastrado para outro cliente". | ok — bug de formatação (máscara sempre de CNPJ, mesmo digitando CPF) corrigido, ver DECISIONS.md |
| 26 | Telefone inválido | Nome novo | Razão nova | Preencher telefone sem DDD → alerta "Telefone inválido (informe DDD + Número)" | ok                                                                                               |
| 27 | CEP inválido | Nome novo | Razão nova | CEP incompleto/errado → alerta "CEP inválido" | ok                                                                                               |
| 28 | Editar cliente existente | (via modal de detalhes → Editar) | | Atualiza, some da lista e reaparece com dado novo | ok                                                                                               |

---

## 4. ProdutoScreen

Campos: Nome*, Unidade* (dropdown: **Quilos / Toneladas / Gramas** — trocado do app antigo,
antes eram as unidades genéricas de varejo), Observações.

| # | Cenário | Nome | Unidade | Efeito Esperado | Resultado |
|---|---------|------|---------|------------------|-----------|
| 29 | Cadastro válido | Soja | Toneladas | Salvo com sucesso. | ok         |
| 30 | Nome vazio | (vazio) | Quilos | Alerta "Nome do produto é obrigatório". | ok        |
| 31 | Nome duplicado | (nome já cadastrado) | Gramas | Alerta "Já existe um produto cadastrado com esse nome". |           |
| 32 | Conferir as 3 opções do dropdown | — | — | Só aparecem **Quilos, Toneladas, Gramas** (nada de UN/CX/PCT/etc. do varejo) | ok        |
| 33 | Editar unidade de um produto existente | (via modal → Editar) | trocar unidade | Atualiza corretamente | ok        |

---

## 5. UsuarioScreen

Campos: Nome*, Login*, Senha*, Telefone, Administrador? (Sim/Não). **Login e senha ficam
criptografados no banco** (AES/ECB) — a tela sempre trabalha com texto puro, quem decripta/
criptografa é a camada de serviço.

| # | Cenário | Login | Senha | Efeito Esperado | Resultado |
|---|---------|-------|-------|------------------|-----------|
| 34 | Cadastro válido (não-admin) | joaosilva | 12345 | Salvo. Consegue logar depois (se licença válida). | |
| 35 | Cadastro válido (admin) | mariaadmin | 12345 | Salvo com Administrador=Sim. Consegue logar mesmo com licença vencida. | |
| 36 | Login vazio | (vazio) | 12345 | Alerta "Login é obrigatório". | |
| 37 | Senha vazia | joaosilva2 | (vazio) | Alerta "Senha é obrigatória". | |
| 38 | Nome vazio | joaosilva3 | 12345 | Alerta "Nome é obrigatório" (nome também obrigatório, campo separado) | |
| 39 | Login duplicado | joaosilva | outra senha | Alerta "Login já em uso por outro usuário". | |
| 40 | Editar usuário mantendo login | (via modal → Editar, só troca nome/telefone) | | Atualiza sem reclamar de duplicidade (é o mesmo id) | |
| 41 | Excluir (inativar) usuário | (via modal → Excluir) | | Usuário some da lista de ativos; não consegue mais logar com ele | |
| 42 | Conferir no banco que login/senha NÃO estão em texto puro | — | — | `SELECT login, senha FROM usuarios` mostra base64 cifrado, não o texto digitado | |

---

## 6. PesagemScreen (núcleo do sistema)

Campos: Placa*, Nome do motorista*, Documento do motorista, Cliente*, Produto*, Nota fiscal,
Tara/Peso do veículo, Peso bruto/total, Peso líquido/final (calculado), 8 tipos de desconto (%),
4 fotos, Observações. Mostra o peso ao vivo da balança conectada (topo da lista, fora da tela de
lista/formulário — ver seção "Conexão da balança").

### Testando sem balança física (casos #50–54, #79)

`LeitorBalancaTcp` (`my_app.infra.balanca`) não faz handshake nem manda comando nenhum pra
balança — só abre um socket TCP e lê o que vier, então dá pra simular com um script simples
sem precisar de hardware nem porta serial de verdade:

- **`scripts/simular_balanca_tcp.py [porta] [peso_tara] [peso_bruto] [duracao_rampa_seg]`** —
  manda um peso a cada ~1s que **sobe** do piso (peso_tara, padrão 8500) até o teto (peso_bruto,
  padrão 32000) ao longo da rampa (padrão 20s) e depois se estabiliza lá, com um pequeno ruído
  aleatório (imita uma balança "tremendo"). Capture Tara logo no início (perto do piso) e Peso
  bruto depois de esperar a rampa terminar (perto do teto) — assim bruto sempre fica maior que
  tara, sem risco do ruído aleatório fazer uma leitura posterior vir menor que uma anterior (o
  que daria peso líquido negativo, sem representar nenhum cenário real). Rampa reinicia a cada
  nova conexão, dá pra repetir o teste sem reiniciar o script.
- **`scripts/simular_balanca_tcp_manual.py [porta]`** — só manda um peso quando você digita um
  valor e aperta Enter no terminal (Enter vazio repete o último). Bom pra testar sequências
  exatas — ex.: mandar uma Tara específica, capturar, mandar um Peso bruto específico, capturar,
  e só então calcular, conferindo a conta exata sem o ruído do script automático atrapalhar.

Em ambos: rode o script primeiro, **depois** configure a tela "Conexão da balança" (Tipo TCP,
IP `127.0.0.1`, Porta igual à passada pro script) e só então abra a tela de Pesagem — o leitor
conecta uma vez ao montar a tela e não parece reconectar sozinho se não achar nada escutando
ainda.

Isso não cobre o caminho **Serial** (`LeitorBalancaSerial`, via JSSC) — testar esse exigiria uma
porta serial virtual (`socat` cria um par, mas configurar o app pra usar uma porta Linux
arbitrária pela tela ainda não foi verificado nesta reescrita). Ver "Pendências conhecidas" no
fim deste arquivo.

| # | Cenário | Placa | Motorista | Cliente | Produto | Efeito Esperado | Resultado |
|---|---------|-------|-----------|---------|---------|------------------|-----------|
| 43 | Cadastro válido completo | ABC1D23 | José da Silva | (selecionar) | (selecionar) | Salvo, aparece na lista com Operação (Entrada/Saída) | |
| 44 | Placa vazia | (vazio) | José | Cliente | Produto | Alerta "Placa é obrigatória". | |
| 45 | Motorista vazio | ABC1D23 | (vazio) | Cliente | Produto | Alerta "Nome do motorista é obrigatório". | |
| 46 | Sem cliente selecionado | ABC1D23 | José | (vazio) | Produto | Alerta "Cliente é obrigatório". | |
| 47 | Sem produto selecionado | ABC1D23 | José | Cliente | (vazio) | Alerta "Produto é obrigatório". | |
| 48 | Determinar Entrada/Saída pela placa | mesma placa, 1ª pesagem | | | | Operação = "Entrada" | |
| 49 | Determinar Entrada/Saída pela placa | mesma placa, 2ª pesagem | | | | Operação = "Saída" | |
| 50 | Capturar Tara com balança conectada | — | | | | Botão "Capturar" no campo Tara copia o peso ao vivo mostrado no topo | |
| 51 | Capturar Peso Bruto com balança conectada | — | | | | Idem, no campo Peso bruto | |
| 52 | Capturar sem balança conectada | — | | | | Alerta "Balança não está conectada" | |
| 53 | Calcular peso líquido sem desconto | Tara 8500, Bruto 32000, descontos 0 | | | | Líquido = 23500.00 (bruto − tara) | |
| 54 | Calcular peso líquido com desconto | Tara 8500, Bruto 32000, Avariados 2% | | | | Líquido = bruto − tara, menos 2% — conferir a conta | |
| 55 | Filtrar por placa | | | | | Digitar placa no filtro (topo da lista) reduz a tabela | |
| 56 | Filtrar por motorista | | | | | Idem, por nome do motorista | |
| 57 | Filtrar por período (data início/fim) | | | | | Só mostra pesagens no intervalo | |
| 58 | Anexar as 4 fotos | | | | | Cada slot abre seletor de arquivo, mostra preview | |
| 59 | Editar pesagem existente | (via modal → Editar) | | | | Recalcula/atualiza mantendo vínculo com cliente/produto/desconto | |
| 79 | Clicar "Calcular" **antes** de capturar Tara e Peso bruto, depois capturar os dois | Capturar Tara, clicar Calcular, capturar Peso bruto (nessa ordem) | | | | Líquido calculado fica **desatualizado** — usa bruto=0 no momento do clique (ex.: líquido ≈ −Tara). "Calcular" não recalcula sozinho quando Tara/Peso bruto mudam depois; precisa capturar os dois **antes** de clicar Calcular por último | |
| 80 | Tara sugerida na "Saída" | 1. Cadastrar uma pesagem com placa X e Tara preenchida (vira "Entrada"). 2. Criar nova pesagem, mesma placa X | | | | Ao digitar a placa X no formulário novo, campo Tara já vem preenchido sozinho com o valor da pesagem anterior + popup avisando. Ainda editável por cima. | |
| 81 | Sem tara sugerida numa "Entrada" nova | Placa nunca usada, ou placa já com Entrada+Saída fechadas (3ª pesagem seria Entrada de novo) | | | | Campo Tara continua vazio — sem sugestão, preenche manual/captura normal | |

---

## 7. ConexaoBalancaScreen

Campos: Tipo de conexão (Serial/TCP), e conforme o tipo: Porta COM + Baud rate (Serial) OU
Endereço IP + Porta (TCP).

| # | Cenário | Tipo | Efeito Esperado | Resultado |
|---|---------|------|------------------|-----------|
| 60 | Selecionar Serial | Serial | Mostra campos "Porta COM" (dropdown com portas detectadas) e "Baud rate" | |
| 61 | Selecionar TCP | TCP | Mostra campos "Endereço IP" e "Porta" | |
| 62 | Salvar Serial sem porta | Serial, porta vazia | Alerta "Porta COM é obrigatória para conexão Serial". | |
| 63 | Salvar Serial sem baud rate | Serial, baud vazio | Alerta "Baud rate é obrigatório para conexão Serial". | |
| 64 | Salvar TCP sem IP | TCP, IP vazio | Alerta "Endereço IP é obrigatório para conexão TCP". | |
| 65 | Salvar TCP sem porta | TCP, porta vazia | Alerta "Porta IP é obrigatória para conexão TCP". | |
| 66 | Salvar configuração válida (Serial ou TCP) | — | Salvo; `PesagemScreen` passa a tentar ler peso dessa conexão | |
| 67 | Trocar de Serial pra TCP e salvar | — | Sobrescreve a conexão anterior (é config única, não lista) | |

---

## 8. CadastroEmpresaScreen

Campos: Nome*, CPF/CNPJ, Telefone, Email, Endereço (CEP/Cidade/Estado/Bairro/Rua/Número),
Logomarca.

| # | Cenário | Nome | Efeito Esperado | Resultado |
|---|---------|------|------------------|-----------|
| 68 | Cadastro/edição válida | Balanças Gobitech | Salvo com sucesso | |
| 69 | Nome vazio | (vazio) | Alerta "Nome é obrigatório". | |
| 70 | Telefone inválido | Balanças Gobitech | Alerta "Telefone inválido". | |
| 71 | CEP inválido | Balanças Gobitech | Alerta "CEP inválido". | |
| 72 | Trocar logomarca | — | Seleciona arquivo de imagem, atualiza preview | |

---

## 9. LicensaScreen (só admin)

| # | Cenário | Efeito Esperado | Resultado |
|---|---------|------------------|-----------|
| 73 | Acessar como admin | Menu "Gerar licença" aparece no menu Gerencial | ok        |
| 74 | Acessar como não-admin | Item "Gerar licença" **não aparece** no menu | ok        |
| 75 | Gerar licença com "Definir data de expiração?" = Não | DatePicker **não aparece**. Popup "Licença gerada com sucesso"; código aparece na tela e na lista; nunca expira | ok        |
| 76 | Selecionar "Definir data de expiração?" = Sim | DatePicker aparece. Escolher uma data e gerar: idem #75, mas `expirada()` passa a `true` depois da data escolhida (23:59:59 daquele dia) |           |
| 77 | Selecionar "Sim" mas não escolher nenhuma data | Alerta "Selecione a data de expiração". Não gera. | ok        |
| 78 | Usuário não-admin loga depois da licença vencer | Bloqueado (ver caso #5) | ok        |

---

## Pendências conhecidas (não testar até implementar)
- Exportação de pesagem em PDF/Nota Fiscal — ainda não implementada nesta reescrita (só existia
  no app antigo). Ver `docs/TODO.md`.
- Leitura de peso via **Serial** contra hardware/porta real — só testado com a lógica de parsing
  isolada (`PesoParserTest`), não contra um leitor de balança de verdade nem uma porta serial
  virtual. Ver `docs/DECISIONS.md`. **TCP** já dá pra testar sem hardware — ver seção 6 acima
  (`scripts/simular_balanca_tcp.py` / `simular_balanca_tcp_manual.py`).
