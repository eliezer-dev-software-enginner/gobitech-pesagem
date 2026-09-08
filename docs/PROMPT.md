# Agente Oficial do Projeto

Você é o agente oficial deste projeto. Siga este processo em toda sessão.

## 1. Contexto obrigatório (leia antes de qualquer ação)
- docs/AI_RULES.md
- docs/CONTEXT.md
- docs/DECISIONS.md
- docs/TODO.md
- README.md

Não inicie nenhuma tarefa sem ter lido esses arquivos primeiro.

## 2. Entendimento do projeto
- Analise a estrutura atual de pastas e arquivos.
- Identifique os padrões de código, arquitetura e nomenclatura já em uso.
- Siga os padrões existentes. Nunca introduza bibliotecas, frameworks ou tecnologias diferentes das já usadas sem autorização explícita.

## 3. Antes de modificar arquivos
- Explique brevemente o plano da tarefa antes de executar qualquer mudança.

## 4. Tarefa: Vistoria completa do projeto

Faça uma varredura **completa e exaustiva** de todo o projeto — não uma amostragem superficial. Isso significa:

- Percorrer **todos os diretórios e arquivos de código-fonte** (não pular pastas por parecerem secundárias).
- Analisar cada arquivo relevante individualmente, não apenas os arquivos de entrada/principais.

Procure especificamente por:

- **Código incompleto**: funções vazias, `TODO`/`FIXME`/`XXX` no código, stubs sem implementação, trechos comentados como "implementar depois".
- **Erros e falhas potenciais**: tratamento de erro ausente, exceções não capturadas, validações faltando, possíveis null/undefined não tratados.
- **Inconsistências**: código que não segue os padrões já identificados no projeto, duplicação de lógica, nomenclatura inconsistente.
- **Dependências e configuração**: pacotes não utilizados, versões conflitantes, variáveis de ambiente esperadas mas não documentadas.
- **Segurança**: segredos/chaves hardcoded, endpoints sem autenticação, inputs sem sanitização.
- **Testes**: funcionalidades sem cobertura de teste, testes quebrados ou desabilitados.
- **Documentação**: funções/módulos importantes sem documentação, README ou docs desatualizados em relação ao código atual.
- **Débito técnico visível**: arquivos obsoletos, código morto, imports não usados.

Para cada item encontrado, registre em `docs/TODO.md` com:
- **Prioridade** (alta / média / baixa)
- **Arquivo(s) e localização** (caminho + linha, se aplicável)
- **Descrição do problema**
- **Sugestão de correção** (se houver)

Não pare na primeira leva de problemas óbvios — continue até ter revisado toda a árvore de arquivos do projeto.

## 5. Ao final da tarefa
Registre em `docs/DECISIONS.md` (se houve decisão arquitetural) e atualize:
- **docs/CONTEXT.md**: estado atual do projeto após a vistoria.
- **docs/DECISIONS.md**: decisões arquiteturais tomadas, se houver.
- **docs/TODO.md**: lista completa dos pontos encontrados, conforme formato acima.

Mantenha todos os arquivos de documentação concisos e objetivos.

Ao final, resuma para mim:
- O que foi analisado.
- Quantos pontos de falha/pendências foram encontrados.
- Quais são os 3 mais críticos.