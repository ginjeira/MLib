# Fecho admissível das waves — 22-11-2026

Este documento fixa o **máximo que considero admissível fechar** sem declarar concluído o que ainda não está implementado e validado.

A **v0.1.25** é a última release automática. A **v0.1.26** foi publicada como release candidate após verificação manual de package, hashes e assinatura; as releases seguintes exigem acordo do owner.

## Entregue em `main` depois da tag `v0.1.26` (não publicado)

- **Revisão de aquisição:** as faixas encaminhadas são revistas antes de entrarem na biblioteca; rejeitar remove as mensagens de teste.
- **App lock:** bloqueio opcional com o mecanismo do dispositivo.
- **Wave 8 (MVP):** dialog “Sobre este álbum” com MusicBrainz + Wikipédia, atribuição e link para a fonte.
- **Wave 4:** lista de faixas existentes e em falta em “Completar álbum”, com o dialog a renderizar fora do dialog de edição; anel de completude no detalhe; capas personalizadas de álbum e playlist a partir do dispositivo, persistidas e sincronizadas; placeholder explícito em vez de Unsplash.
- Validação local: 100 testes, 0 falhas.

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

### Wave 4 — Álbuns e capas

**Fechada no âmbito atual.**

Inclui:

- fluxo **recolher → rever → confirmar** por faixa (revisão de aquisição);
- lista real de faixas existentes e em falta, com anel de completude;
- capas personalizadas de álbum e playlist a partir do dispositivo, persistidas e sincronizadas;
- enriquecimento iTunes/Deezer e placeholder explícito.

Limitações aceites (Wave 4.1): capa grande com gradiente, faixas ambíguas e presets de capa.

## Não fechadas

### Wave 2 — Navegação e UI “MLib Signal”

**Não iniciada.** Requer refactor de navegação, mini-player/Home e novos ecrãs. Não é admissível declarar fechada.

### Wave 6 — Descoberta e poder

**Fechada no âmbito atual.**

Inclui:

- sleep timer e velocidade de reprodução;
- Android Auto completo para reprodução: Início, todas as faixas, playlists, álbuns, artistas, favoritos, offline e recentes, com pesquisa por voz do Assistente restrita à biblioteca (a aquisição não é permitida no carro);
- auto-download de favoritos;
- enriquecimento e pesquisa unificada;
- app lock com o bloqueio do dispositivo;
- exportação JSON da biblioteca;
- temas AMOLED e alto contraste.

Limitações aceites (Wave 6.1): páginas de artista, widgets, scrobbling, smart playlists, voz, estatísticas, backup completo e retoma de fila entre dispositivos.

### Wave 7 — Plataformas

**Não iniciada.** Desktop continua congelado.

### Wave 8 — Conhecimento musical

**Fechada no âmbito MVP (álbum).**

Inclui:

- “Sobre este álbum” com data de lançamento, tipo, géneros e resumo;
- fonte por licença (MusicBrainz CC0, Wikipédia CC BY-SA), link e data de recolha;
- cache em disco com TTL de 30 dias e limite de 300 entradas;
- parsers de MusicBrainz e Wikipédia com fixtures;
- nenhuma tag local é alterada automaticamente.

Limitações aceites (Wave 8.1): páginas de artista e discografia, créditos de faixa, ETag e testes live.

## Critério de fecho das restantes

- Wave 2: navegação e UI implementadas, testadas e com screenshots atuais.
- Wave 4.1: capa grande/gradiente, faixas ambíguas e presets de capa.
- Wave 6.1: páginas de artista, widgets, scrobbling, smart playlists, voz, estatísticas, backup e retoma de fila.
- Wave 7: só após Android estável e com decisão de descongelar desktop.
- Wave 8.1: páginas de artista, discografia, créditos, ETag e testes live.

Enquanto estes critérios não estiverem cumpridos, as waves permanecem abertas.
