# Regras do Projeto

## Antes de qualquer alteração
- Ler `docs/CONTEXT.md`, `docs/DECISIONS.md` e `docs/TODO.md`, nessa ordem.
- Pra contexto de negócio/histórico do bug original: ver
  `/home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/` (o projeto Java Swing antigo, já
  auditado — `CONTEXT.md`, `DECISIONS.md`, `TODO.md`, `ENTREGAS.md`).
- Analisar a estrutura existente antes de criar algo novo — este projeto já tem um padrão
  estabelecido (Screen+ViewModel, Model+Repository+Service), seguir ele em vez de inventar outro.
- Não substituir funcionalidades sem confirmar antes.

## Linguagem e stack
- Java 25.
- SQLite (não MySQL nem outro banco cliente-servidor — ver decisão em `DECISIONS.md`).
- JavaFX + Megalodonte (framework de UI próprio) — não introduzir outro framework de UI sem
  autorização.

## Código
- Não criar arquivos desnecessários.
- Não gerar comentários óbvios.
- Priorizar simplicidade.
- Telas dentro de `my_app/screens` sempre devem ter sua ViewModel correspondente.
- Se a ViewModel ficar muito extensa, fragmentar em uma Service.
- Alterações nas Models devem refletir nas migrations dentro de
  `src/main/resources/flyway_migrations` — nunca editar o schema só na Model.
- `dataCriacao` nas Models deve ser do tipo `LocalDateTime`, mapeado pra coluna `TIMESTAMP` no
  SQLite (não `REAL` nem `INTEGER` — os dois quebram o Persism ao reler a linha do banco:
  `REAL`/`INTEGER` viram `Double`/`Long` puros em Java, sem conversão automática pra
  `LocalDateTime`, e o `atualizar()`/`buscarById()`/`listar()` lançam `PersismException` —
  achado só ao rodar os testes automatizados, `salvar()` sozinho não pega isso porque não relê
  do banco. Ver `DECISIONS.md`). Campos `BigDecimal` (dinheiro, percentual) continuam `REAL`
  normalmente — o problema é só com `LocalDateTime`. `id` deve ser `Integer`.
- Erros do Persism que não conseguir resolver: consultar
  https://sproket.github.io/Persism/manual2.html
- Pra campos de endereço numa Model, reaproveitar `EnderecoState`/`Components.enderecoComponent`
  em vez de reinventar — os nomes de campo precisam bater com o que esse componente espera
  (`cep`/`uf`/`cidade`/`bairro`/`rua`/`numero`).
- Pra objetos hierárquicos dentro de uma Model, pode-se usar a anotação `@NotColumn`.

## Escopo
- Este projeto é uma reescrita do software de pesagem original — antes de adicionar qualquer
  funcionalidade nova, confirmar que ela faz parte do escopo da Fase 1 (CRUD de usuário/cliente/
  produto + balança funcionando) ou se é Fase 2 (câmera) — ver `TODO.md`. Não reintroduzir
  funcionalidades de varejo do `plics-sw` (vendas, compras, estoque, PDV, contas a pagar/receber)
  sem pedido explícito — foram removidas de propósito, não por esquecimento.

## Testes
- Sempre testar a repository cuja tela tiver sido refatorada.

## Após realizar alterações
- Atualizar `docs/CONTEXT.md` (estado atual), `docs/DECISIONS.md` (se houve decisão
  arquitetural) e `docs/TODO.md` (pendências) — manter os três concisos.
- Se o usuário pedir commit: usar padrões `feat`, `fix`, `refactor`, `test` ou `clean`.
