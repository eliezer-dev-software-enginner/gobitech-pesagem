# Plano de Testes Manuais — Gobitech Sistema de Pesagem

**Versão testada:** 1.0.0
**Telas cobertas:** Pesagem entrada, Pesagem de saída, Pesagem avulsa, Pesagem manual

## Regras de negócio observadas (premissas a confirmar com o time)

- **Placa** é o único campo obrigatório em todas as telas (marcado com `*`).
- **Peso líquido** é somente leitura, calculado automaticamente (`|saída - entrada|`).
- Os campos se chamam **Entrada (Kg)** e **Saída (Kg)**. O botão **Capturar** preenche o
  respectivo campo com o valor atual da balança.
- Na tela **Entrada**, somente **Entrada (Kg)** pode ser capturada; **Saída (Kg)** é somente
  leitura. Na tela **Saída**, a Entrada da pesagem vinculada é somente leitura e somente
  **Saída (Kg)** pode ser capturada.
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
| B1 | Capturar Entrada | Na tela Entrada, clicar "Capturar" ao lado de Entrada | Entrada preenchida com o valor da balança | |
| B2 | Bloquear Saída na Entrada | Na tela Entrada, conferir Saída | Campo sem botão e somente leitura | |
| B3 | Capturar Saída | Na tela Saída com entrada vinculada, clicar "Capturar" ao lado de Saída | Saída preenchida com o valor da balança | |
| B4 | Bloquear Entrada na Saída | Na tela Saída com entrada vinculada, conferir Entrada | Valor trazido da entrada, sem botão e somente leitura | |
| B5 | Cálculo ao carregar | Registrar Entrada e depois capturar Saída maior | Peso líquido = saída - entrada, calculado automaticamente | |
| B6 | Cálculo ao descarregar | Informar Entrada 4000 e Saída 2000 | Salva e calcula peso líquido 2000 | |

---

## C. Fluxo: caminhão chega **vazio**, carrega, sai **cheio**

| # | Cenário | Passos | Resultado esperado | Resultado                     |
|---|---|---|---|-------------------------------|
| C1 | Registrar Entrada | Na tela Entrada: preencher Placa, capturar Entrada, salvar | Salva com sucesso; Saída permanece vazia | |
| C2 | Buscar na Saída | Ir em Pesagem de Saída, digitar a mesma placa | Sistema localiza a entrada e traz Entrada, motorista, cliente, produto e nota fiscal já preenchidos | |
| C3 | Concluir a Saída | Capturar Saída na tela de Saída, salvar | Peso líquido calculado pela diferença absoluta e registro fechado | |

---

## D. Isolamento entre as telas de Entrada e Saída

| # | Cenário | Passos | Resultado esperado | Resultado                          |
|---|---|---|---|------------------------------------|
| D1 | Entrada não captura Saída | Abrir a tela Entrada | Apenas Entrada tem botão Capturar | |
| D2 | Saída não recaptura Entrada | Buscar uma placa com Entrada na tela Saída | Entrada fica somente leitura, sem botão Capturar | |
| D3 | Saída captura apenas Saída | Com a mesma placa, capturar o peso atual | Apenas Saída é alterada | |

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
| F1 | Fluxo completo numa tela só | Informar Entrada e capturar Saída | Líquido calculado normalmente | OK                        |
| F2 | Salvar só com Placa | Placa preenchida, nenhum peso capturado, salvar | Pede confirmação ("Nenhum peso foi informado... Deseja salvar mesmo assim?"). Sim → salva; Não → cancela | ok (fix aplicado: mostra aviso de confirmação) |

---

## G. Pesagem manual (sem captura de balança)

| # | Cenário | Passos | Resultado esperado | Resultado                                                                                                                          |
|---|---|---|---|------------------------------------------------------------------------------------------------------------------------------------|
| G1 | Digitar pesos manualmente | Digitar Entrada e Saída sem usar botão de captura (tela não tem esse botão) | Líquido calcula normalmente | OK                                                                                                                                 |
| G2 | Texto em campo numérico | Digitar letras no campo Entrada/Saída | Rejeita ou limpa caracteres não numéricos | OK                                                                                                                                 |
| G3 | Valor decimal | Digitar `8500,5` ou `8500.5` | Verificar aceitação de vírgula/ponto e formatação resultante | ok (bug mantido ao colar: `8500.5` vira `85.005`) é uma aplicação de balança, ou o valor vai ser digitado ou vai ser  capturado :) |
| G4 | Saída menor que Entrada | Digitar Saída < Entrada manualmente | Salva e calcula a diferença absoluta | |
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
