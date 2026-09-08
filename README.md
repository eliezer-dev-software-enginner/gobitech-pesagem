# Gobitech — Sistema de pesagem

Sistema desktop de pesagem de caminhões para a Balanças Gobitech, usado em uma estação
única (PC da balança), desenvolvido com JavaFX + Megalodonte, banco SQLite local.

Reescrita completa do software legado (Swing + MySQL) sobre a infraestrutura do
`plics-sw` — ver `docs/CONTEXT.md` e `docs/DECISIONS.md` para o histórico e as decisões.

## Funcionalidades

- **Pesagens por tipo**: Entrada, Saída, Avulsa e Manual (`pesagens.tipo_pesagem`), cada
  uma com comportamento próprio de captura/digitação dos pesos (tara, bruto e líquido).
  Só a **Placa** é obrigatória.
- **Balança**: leitura ao vivo do peso via **Serial** (jSerialComm) ou **TCP**, com
  botão "Capturar" para tara/bruto. Há simuladores em `scripts/` para testar a TCP sem
  hardware.
- **Saída por placa**: ao digitar a placa, puxa a última Entrada do caminhão (motorista,
  cliente, produto, tara e bruto) para só confirmar o peso de saída.
- **Descontos** (8 tipos, soma ≤ 100%) e validação **bruto ≥ tara** na camada de serviço.
- **Fotos** (frente/costas) por pesagem.
- **Ticket de pesagem**: impressão em **PDF** (2 vias na mesma folha) e em **térmica
  ESC/POS** (impressora padrão do sistema).
- **Relatório do histórico** em PDF (pares Entrada+Saída por placa, com totais) e
  **resumo de entradas/saídas** com o layout do cliente.
- **Cadastros**: clientes, produtos, usuários, empresa (logo no ticket), conexão da
  balança, preferências.
- **Licença com validade**: gerada pelo próprio admin na tela "Gerar licença" (menu
  exclusivo de admin). Admin sempre loga; usuário comum é bloqueado com licença vencida.
- **Logs por usuário**, com o menu "Ver logs da aplicação" restrito a admin; notificações
  de erro/log enviadas ao **Telegram** (suporte remoto).
- **Migrações** do banco via Flyway (`src/main/resources/flyway_migrations`).

## Stack

- Java 25 (toolchain Gradle), JavaFX 25.0.1
- Megalodonte: `base`, `components`, `reactivity`, `router`, `theme`
- Persism (ORM) + SQLite (banco embutido, sem servidor)
- Flyway (schema e dados padrão)
- SLF4J/Logback, jSerialComm/jSSC (serial), escpos-coffee (térmica), PDFBox (relatórios)

Padrão de tela: `Screen` + `ViewModel` (uma pasta por entidade em `my_app/screens/`);
camada de dados: `Model` (Persism) + `Repository` (`BaseRepository`) + `Service`
(`BaseService`) em `my_app/db/`.

## Pré-requisitos

- JDK 25 (o Gradle toolchain resolve a versão; `JAVA_HOME` é usada pelos scripts de
  empacotamento)
- Variável de ambiente **`JAVAFX_MODULES_HOME`** apontando para a pasta que contém
  `windows-25.0.1/` e `linux-25.0.1/` — obrigatória para o `./gradlew run`.
- Variáveis opcionais: `DEV_MODE=true` (liga `-Dprism.verbose` no run), `GITHUB_TOKEN`
  (workflow de release).

## Execução

```bash
./gradlew run
```

O banco fica em `%APPDATA%\gobitech\erp.db` (Windows) ou `~/.gobitech/erp.db` (Linux).
Na primeira execução o Flyway cria o schema e o usuário admin padrão (`V10`).

## Testes

```bash
./gradlew test
```

## Empacotamento

A versão vive em `gradle.properties` (`appVersion` + `appPatch`). Incremente com:

```bash
python scripts/bump_version.py patch            # 1.0.1 -> 1.0.1.1 (patch)
python scripts/bump_version.py release 1.1.0    # base nova, zera o patch
```

Geração de pacotes (leem `gradle.properties` e embutem a versão no runtime como
`-Dgobitech.appVersion`):

```bash
python scripts/create-msi.py   # Windows (.msi)
python scripts/create-deb.py   # Linux (.deb)
```

Flatpak: existe a opção experimental `scripts/create-flatpak.py` (build local de teste
via flatpak-builder) — o app não usa mais auto-update nem `Main.isFlatpak`.

## Desenvolvimento

- Separador/help de testes manuais: `docs/testes-pesagem.md`
- Simulador de balança TCP: `scripts/simular_balanca_tcp.py` (e a versão manual
  `simular_balanca_tcp_manual.py`); seed de produtos: `scripts/seed_produtos.py`
- Watch/reload simples: `python dev.py` (reinicia via `gradlew run`)

## Documentação

- `docs/CONTEXT.md` — contexto, stack, estado atual e histórico do projeto
- `docs/DECISIONS.md` — decisões técnicas/negócio registradas
- `docs/TODO.md` — pendências e vistorias (READ-ME primeiro: vale como guia de trabalho)