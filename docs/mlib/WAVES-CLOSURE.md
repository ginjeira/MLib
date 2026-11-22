# Fecho admissível das waves — 22-11-2026

Este documento fixa o **máximo que considero admissível fechar** sem declarar concluído o que ainda não está implementado e validado.

A **v0.1.25** é a última release automática. A **v0.1.26** foi publicada como release candidate após verificação manual de package, hashes e assinatura; as releases seguintes exigem acordo do owner.

## Entregue em `main` depois da tag `v0.1.26` (não publicado)

- **Revisão de aquisição:** as faixas encaminhadas são revistas antes de entrarem na biblioteca; rejeitar remove as mensagens de teste.
- **App lock:** bloqueio opcional com o mecanismo do dispositivo.
- **Wave 8 (MVP):** dialog “Sobre este álbum” com MusicBrainz + Wikipédia, atribuição e link para a fonte.
- **Wave 4:** lista de faixas existentes e em falta em “Completar álbum”, com o dialog a renderizar fora do dialog de edição.
- Validação local: 61 testes, 0 falhas.

## Fechadas

### Wave 0 — Release e atualização

**Fechada.** O ciclo de release, verificação de assinatura e atualização in-app foi validado. A política mudou: releases automáticas ficam desativadas após a 0.1.25.

### Wave 1 — Aquisição fiável

**Fechada e testada pelo owner.** Destino, confirmação, pré-indexação, dedup por destino, revalidação, saúde da biblioteca e “Completar álbum” estão implementados. Itens avançados (simulação completa, latência, >2.000) passaram para Wave 1.1.

### Wave 3 — Confiança e biblioteca

**Fechada no âmbito atual.**

Inclui:

- saúde da biblioteca;
- reparação de faixas indisponíveis;
- revisão e merge de duplicados;
- exportação JSON para ficheiro;
- pesquisa unificada “Procurar no Telegram”;
- histórico de jobs.

Limitações aceites:

- exportação para ficheiro ainda não tem teste de integração;
- merge tem teste de regressão, mas não uma suite golden completa;
- pesquisa unificada é local + bot, não um índice global.

Estas limitações passam para Wave 3.1 e não bloqueiam o fecho funcional.

### Wave 5 — Setup e jobs

**Fechada no âmbito atual.**

Inclui:

- `/start` apenas no setup explícito;
- canal de arquivo;
- histórico de jobs persistido;
- “Pesquisar novamente” para jobs antigos.

Limitações aceites:

- retry por item/faixa fica para Wave 5.1;
- testes de crash/restart ficam para Wave 5.1.

## Não fechadas

### Wave 2 — Navegação e UI “MLib Signal”

**Não iniciada.** Requer refactor de navegação, mini-player/Home e novos ecrãs. Não é admissível declarar fechada.

### Wave 4 — Álbuns e capas

**Em curso, não fechada.** Já tem dedup exata, confirmação de completar álbum, lista de faixas existentes, capas de playlist e enriquecimento iTunes/Deezer. Falta o essencial:

- fluxo **recolher → rever → confirmar** por faixa;
- lista real de faixas em falta;
- gestão completa de capas/upload local.

O fix que faz o diálogo de completar álbum fechar o detalhe e abrir a aquisição está em `main`, mas não na 0.1.25.

### Wave 6 — Descoberta e poder

**Parcial.** Sleep timer, Android Auto Offline/Recent/Artists, auto-download de favoritos e enriquecimento estão feitos. Faltam widgets, import/export completo, privacidade, scrobbling e smart playlists.

### Wave 7 — Plataformas

**Não iniciada.** Desktop continua congelado.

### Wave 8 — Conhecimento musical

**Não iniciada.** Proposta registada com MusicBrainz/Wikipédia, fontes, licenças, cache e regras de não sobrescrever tags.

## Critério de fecho das restantes

- Wave 2: navegação e UI implementadas, testadas e com screenshots atuais.
- Wave 4: revisão/confirmação por faixa e lista de faixas em falta validadas.
- Wave 6: funcionalidades escolhidas implementadas e testadas.
- Wave 7: só após Android estável e com decisão de descongelar desktop.
- Wave 8: fontes, licenças, cache e páginas de artista implementadas.

Enquanto estes critérios não estiverem cumpridos, as waves permanecem abertas.
