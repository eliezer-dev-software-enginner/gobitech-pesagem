# Plano de Testes Manuais — Gobitech Sistema de Pesagem

**Versão testada:** 1.0.0
**Telas cobertas:** Pesagem entrada, Pesagem de saída, Pesagem avulsa, Pesagem manual

## Regras de negócio observadas (premissas a confirmar com o time)

- **Placa** é o único campo obrigatório em todas as telas (marcado com `*`).
- **Peso líquido** é somente leitura, calculado automaticamente (assumido: `bruto - tara`).
- Botão **Capturar** preenche Tara ou Peso bruto com o valor atual em "Peso da balança agora".
- **Entrada** tem os campos Tara e Peso bruto, ambos capturáveis — sugere que o caminhão pode
  chegar vazio (captura a Tara) ou cheio (captura o Peso bruto), deixando o outro peso pra
  quando ele passar pela **Saída**.
- **Saída** busca a pesagem de Entrada pela placa e deve trazer os dados já preenchidos.
- **Avulsa**: fluxo de pesagem única (não depende de um par Entrada/Saída).
- **Manual**: sem botões de captura — todos os pesos são digitados. Tem 2 campos extras
  (Quebra umidade, Outros) que as outras telas não têm.
- **Cliente** e **Produto** são dropdowns, sem `*` — aparentemente opcionais.

---

## A. Campo obrigatório (Placa) — aplica-se às 4 telas

| # | Cenário | Passos | Resultado esperado | Resultado                       |
|---|---|---|---|---------------------------------|
| A1 | Salvar só com Placa | Preencher apenas Placa, deixar todo o resto vazio, salvar | Salva com sucesso | OK                              |
| A2 | Salvar sem Placa | Deixar Placa vazia, preencher o resto, tentar salvar | Bloqueia e sinaliza que Placa é obrigatória | OK                              |
| A3 | Placa só com espaços | Digitar `"   "` no campo Placa, tentar salvar | Tratado como vazio — bloqueia | OK                              |
| A4 | Placa formato antigo | Digitar `ABC1234` | Aceita | OK                              |
| A5 | Placa formato Mercosul | Digitar `ABC1D23` | Aceita | ok                              |
| A6 | Placa minúscula | Digitar `abc1d23` | Verificar se normaliza pra maiúsculo ou salva como está (definir comportamento esperado com o time) | ok deve normalizar pra maisculo |
| A7 | Placa com símbolos | Digitar `ABC-1D23` ou `ABC1D23!` | Verificar se rejeita, limpa automaticamente, ou aceita literal | ok - aceita                     |

---

## B. Captura de peso da balança

| # | Cenário | Passos | Resultado esperado | Resultado           |
|---|---|---|---|---------------------|
| B1 | Capturar Tara | Clicar "Capturar" ao lado de Tara | Tara preenchida com o valor de "Peso da balança agora" | ok                  |
| B2 | Capturar Peso bruto | Clicar "Capturar" ao lado de Peso bruto | Peso bruto preenchido com o valor da balança | ok                  |
| B3 | Cálculo do líquido | Capturar Tara e Peso bruto (bruto > tara) | Peso líquido = bruto - tara, calculado automaticamente | ok                  |
| B4 | Bruto igual à tara | Capturar mesmo valor nos dois campos | Peso líquido = 0 | ok                  |
| B5 | Bruto menor que tara | Capturar bruto < tara (ex: por erro de operação) | Definir e testar: aceita negativo? bloqueia? zera? mostra alerta? | deve mostrar alerta |
| B6 | Balança instável entre capturas | Capturar Tara, aguardar o valor da balança mudar, capturar Peso bruto | Cada captura usa o valor no momento exato do clique, não o valor antigo | ok                  |
| B7 | Sobrescrever valor capturado | Capturar um peso e depois editar manualmente o número | Peso líquido recalcula com o novo valor digitado | ok                  |
| B8 | Digitar sem usar "Capturar" | Digitar Tara/Peso bruto direto no campo (sem clicar no botão) | Aceita normalmente e calcula o líquido | ok                  |

---

## C. Fluxo: caminhão chega **vazio**, carrega, sai **cheio**

| # | Cenário | Passos | Resultado esperado | Resultado                     |
|---|---|---|---|-------------------------------|
| C1 | Registrar entrada vazia | Na tela Entrada: preencher Placa, capturar só a Tara, deixar Peso bruto vazio, salvar | Salva com sucesso; líquido fica 0/vazio | ok (bug corrigido: líquido ficava negativo) |
| C2 | Buscar na saída | Ir em Pesagem de saída, digitar a mesma placa | Sistema localiza a entrada e traz Tara, motorista, cliente, produto e nota fiscal já preenchidos | ok                            |
| C3 | Concluir a saída | Capturar o Peso bruto na tela de Saída, salvar | Peso líquido calculado (bruto - tara) e registro fechado | ok                            |

---

## D. Fluxo: caminhão chega **cheio**, descarrega, sai **vazio**

| # | Cenário | Passos | Resultado esperado | Resultado                          |
|---|---|---|---|------------------------------------|
| D1 | Registrar entrada cheia | Na tela Entrada: preencher Placa, capturar só o Peso bruto, deixar Tara vazia, salvar | Salva com sucesso | OK                                 |
| D2 | Buscar na saída | Ir em Pesagem de saída, digitar a mesma placa | Sistema traz o Peso bruto já registrado na entrada | ok (bug corrigido: não trazia o peso bruto) |
| D3 | Concluir a saída | Capturar a Tara na tela de Saída, salvar | Peso líquido calculado e registro fechado | OK                                 |

---

## E. Pesagem de saída — busca por placa

| # | Cenário | Passos | Resultado esperado | Resultado                         |
|---|---|---|---|-----------------------------------|
| E1 | Placa com entrada em aberto | Digitar placa de uma entrada existente ainda sem saída | Preenche automaticamente os dados da entrada | ok                                |
| E2 | Placa sem entrada | Digitar placa que nunca teve entrada registrada | Sistema avisa que não encontrou entrada — definir se bloqueia ou permite continuar do zero | ok                                |
| E3 | Placa com saída já concluída | Digitar placa de um par Entrada+Saída já fechado | Verificar se abre nova saída (duplicando) ou bloqueia | OK nova saída ocorre normalmente. |
| E4 | Placa incompleta | Digitar só parte da placa | Não deve buscar/travar até a placa estar completa (ou buscar parcial, se for o comportamento desejado) |                                   |
| E5 | Placa com 2 entradas em aberto | Simular a mesma placa com duas entradas sem saída | Definir e validar qual entrada o sistema traz (mais antiga? mais recente?) | ok - traz a mais recente          |

---

## F. Pesagem avulsa

| # | Cenário | Passos | Resultado esperado | Resultado                 |
|---|---|---|---|---------------------------|
| F1 | Fluxo completo numa tela só | Capturar Tara e Peso bruto na mesma tela | Líquido calculado normalmente | OK                        |
| F2 | Salvar só com Placa | Placa preenchida, nenhum peso capturado, salvar | Pede confirmação ("Nenhum peso foi informado... Deseja salvar mesmo assim?"). Sim → salva; Não → cancela | ok (fix aplicado: mostra aviso de confirmação) |

---

## G. Pesagem manual (sem captura de balança)

| # | Cenário | Passos | Resultado esperado | Resultado                                                                                                                          |
|---|---|---|---|------------------------------------------------------------------------------------------------------------------------------------|
| G1 | Digitar pesos manualmente | Digitar Tara e Peso bruto sem usar botão de captura (tela não tem esse botão) | Líquido calcula normalmente | OK                                                                                                                                 |
| G2 | Texto em campo numérico | Digitar letras no campo Tara/Peso bruto | Rejeita ou limpa caracteres não numéricos | OK                                                                                                                                 |
| G3 | Valor decimal | Digitar `8500,5` ou `8500.5` | Verificar aceitação de vírgula/ponto e formatação resultante | ok (bug mantido ao colar: `8500.5` vira `85.005`) é uma aplicação de balança, ou o valor vai ser digitado ou vai ser  capturado :) |
| G4 | Bruto menor que tara | Digitar bruto < tara manualmente | Bloqueia salvamento com alerta "Peso bruto não pode ser menor que a Tara" | ok (fix aplicado: bloqueia)                                                                                                        |
| G5 | Campos exclusivos da manual | Preencher "Quebra umidade" e "Outros" | Verificar se entram no cálculo final de desconto/líquido | ok — entram no cálculo                                                                                                             |
| G6 | Só placa, nenhum peso | Preencher só Placa, salvar | Verificar se permite registro manual "pendente" sem nenhum peso | OK - Exibe o alerta se deseja continuar                                                                                            |

---

## H. Descontos (%) — Avariados, Ardidos, Quebra ardidos, Impurezas, Quebra impurezas, Umidade

| # | Cenário | Passos | Resultado esperado | Resultado                   |
|---|---|---|---|-----------------------------|
| H1 | Valores válidos individuais | Preencher cada desconto com um valor razoável (ex: 2, 5, 10) | Compõe corretamente o cálculo final de peso líquido/desconto |                             |
| H2 | Desconto acima de 100 | Digitar 150 em algum campo | Sistema deve validar/bloquear (definir com o time) |                             |
| H3 | Desconto negativo | Digitar -5 | Deve bloquear | ok                          |
| H4 | Soma de descontos > 100% | Preencher vários campos até ultrapassar 100% no total | Bloqueia salvamento com alerta "A soma dos descontos não pode ultrapassar 100%" | ok (fix aplicado: bloqueia) |
| H5 | Descontos zerados (padrão) | Deixar todos em 0/vazio | Não deve afetar o peso líquido calculado |                             |
| H6 | Texto em campo de desconto | Digitar letras | Deve rejeitar | ok                          |

---

## I. Cliente e Produto (dropdowns)

| # | Cenário | Passos | Resultado esperado | Resultado |
|---|---|---|---|-----------|
| I1 | Sem selecionar nenhum | Salvar com Cliente e Produto vazios | Permite, já que só Placa é obrigatória | ok        |
| I2 | Trocar seleção | Selecionar um Cliente, depois trocar por outro | Produto não deve resetar de forma inesperada (a menos que seja dependente do cliente — confirmar) | ok        |
| I3 | Lista vazia | Testar com nenhum Cliente/Produto cadastrado no sistema | Dropdown mostra estado vazio sem travar a tela |           |

---

## J. Campos textuais opcionais

| # | Cenário | Passos | Resultado esperado | Resultado                            |
|---|---|---|---|--------------------------------------|
| J1 | Motorista vazio | Deixar Nome do motorista vazio, salvar | Permite | ok                                   |
| J2 | Documento inválido | Digitar um RG/CPF com formato incorreto | Verificar se valida ou aceita texto livre | ok - Bloqueia salvar se documento preenchido não for RG (8-9) ou CPF (11) válido (Utils.isValidDocumento) |
| J3 | Nota fiscal vazia | Deixar vazia, salvar | Permite | ok                                   |
| J4 | Texto muito longo | Digitar 200+ caracteres em Nome do motorista | Verificar truncamento ou quebra de layout | ok - Bloqueia salvar se Nome > 100 caracteres |

---

## K. Navegação e persistência de estado

| # | Cenário | Passos | Resultado esperado | Resultado        |
|---|---|---|---|------------------|
| K1 | Trocar de tela sem salvar | Preencher parcialmente a Entrada, clicar em outro item do menu lateral | Definir: perde os dados (esperado) ou mantém rascunho? | OK - perde dados |
| K2 | Limpeza após salvar | Salvar um registro com sucesso | Formulário limpa os campos pra próxima pesagem | Ok - limpa       |
| K3 | Conferência no histórico | Após salvar, abrir "Histórico de pesagens" | Registro aparece com os dados corretos (placa, pesos, cliente, produto) | ok               |

---

## L. Regressão — exportação em PDF

| # | Cenário | Passos | Resultado esperado | Resultado |
|---|---|---|---|---|
| L1 | Pesos sem casas decimais no PDF | Cadastrar pesagens e exportar relatório em PDF | Pesos aparecem como número inteiro, sem sobreposição de colunas (regressão do bug já corrigido) | |

---

## Observações para priorização

- **Alta prioridade:** A (obrigatoriedade da placa), B (captura/cálculo de peso), C e D (os dois
  fluxos completos vazio→cheio e cheio→vazio), E (busca por placa na saída) — são o núcleo do
  sistema.
- **Média prioridade:** H (descontos), G (manual sem captura).
- **Baixa prioridade / exploratório:** I, J, K — mais sobre robustez de UI do que regra de negócio.