# Dev com hot-restart (dev.py)

Reinício automático ao salvar código — não há "hot reload" de classe viva (não existe
`Reloader`); o `dev.py` *mata e reinicia* o app via `gradle run` quando o **conteúdo** de um
arquivo de `src/main/java/` ou `src/main/resources/` muda de verdade.

## Setup

```bash
python -m venv .venv
# Windows:
.venv\Scripts\pip install watchdog
# Linux/Mac:
.venv/bin/pip install watchdog
```

## Uso

```bash
# Windows:
.venv\Scripts\python dev.py
# Linux/Mac:
.venv/bin/python dev.py
```

Ou ative o venv:

```bash
# Windows:
.venv\Scripts\activate
python dev.py
# Linux/Mac:
source .venv/bin/activate
python dev.py
```

## Como funciona (o que o dev.py realmente faz)

1. Monitora `src/main/java/` e `src/main/resources/` com `watchdog`.
2. Na subida, calcula o hash SHA-256 de cada arquivo existente (evita restart falso no
   primeiro evento "phantom" do sistema de arquivos).
3. A cada evento, compara o hash novo com o conhecido — só conteúdo mudado dispara.
4. `DEBOUNCE_SECONDS = 1.5` agrupa salvamentos em sequência.
5. No restart: `taskkill /F /T` na **árvore do processo** (Windows) ou `terminate` (Linux/Mac),
   e sobe de novo com `gradlew.bat run` / `./gradlew run`.
6. Ruído ignorado: arquivos começando com `.`, e marcadores `___jb_tmp___`, `___jb_old___`,
   `.swp`, `.swx`, `~` (IntelliJ safe-write, swap do vim, etc.).

O `.desktop` de desenvolvimento (ícone na dock/taskbar do Linux) quem cria é o próprio app
(`megalodonte.application.LinuxDesktopEntry`, chamado de `Main.java`) — não o script.