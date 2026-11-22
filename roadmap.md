# MLib — roadmap de implementação por waves

**Versão:** 1.0  
**Data:** 20-11-2026  
**Âmbito:** Android primeiro. Desktop permanece congelado.  
**Documentos de referência:** `docs/mlib/PRODUCT-UX-PROPOSAL.md`, `docs/mlib/MCAT-PLAYER-PORT-STATUS.md`, `docs/mlib/PORTABILITY-GAPS-REPORT.md`, `docs/mlib/PORTABILITY-CONTRACT.md`.

Este é o plano de execução corrente. O histórico detalhado permanece em `docs/mlib/ROADMAP.md`.

## 0. Estado da versão 0.1.26

Verificado em **22-11-2026, 14:10 UTC**.

| Item | Estado |
|---|---|
| Última release pública | **v0.1.25** (publicada em 22-11-2026 12:45:02) |
| Versão 0.1.26 | **Preparada** — commit `82084d3`, tag `v0.1.26`; build `45026161511` ainda em execução |
| Release `v0.1.26` | **Ainda não publicada** |
| Publicação automática | **Desativada** após a 0.1.25; exige `MLIB_AUTO_RELEASE=true` ou acordo manual |
| Próxima versão | **Bloqueada até acordo do owner** |
| Documentação | Atualizada para 0.1.26, revisão de aquisição, app lock e Wave 8 |

**Hashes publicados (0.1.25):**

- `mlib-0.1.25-arm64.apk` — `8d49452e225a79234d1fed740988956990eb77bd12c79042d383bbaf0b5427af`
- `mlib-0.1.25-x86_64.apk` — `d63a2418ae693689d53f70b0e18600c7cde9966654e8827683d08d14369cb4cc`
- `SHA256SUMS.txt` — `05392a91f3240cd23687f007160dd2c1b09237ee3de3fc811f223737b5a423fb`

**Pendente na 0.1.26:** confirmação de assets/hashes/fingerprint após o build `45026161511`; o commit inclui o fix de “Completar álbum” e o changelog, mas não a revisão de aquisição, o app lock nem a Wave 8.

## 0.1 Entregue em `main` depois da tag `v0.1.26` (não publicado)

- Revisão antes de escrever na biblioteca: `AcquisitionReview`, `commitReview(selectedIds)` e rejeição com remoção das mensagens encaminhadas.
- App lock opcional com o bloqueio do dispositivo (`Platform.supportsAppLock`, `app_lock`).
- Reparação global de faixas indisponíveis em Definições (branch `feat/library-repair`).
- Wave 8 (MVP): `Knowledge.album()` com MusicBrainz + Wikipédia, dialog “Sobre este álbum” com fonte, data, tipo e géneros.
- Wave 4: “Completar álbum” passou a mostrar a lista de faixas existentes e em falta (Deezer, fallback iTunes) e deixou de ficar aninhado no dialog de edição.
- Validação local: `:desktop:test` com **61 testes, 0 falhas**; `:app:compileDebugKotlin` e `:app:processDebugResources` verdes.

## 0.2 Progresso por wave — 21-11-2026

| Wave | Estado | Progresso | Notas |
|---|---|---:|---|
| Wave 0 — release/update | **Concluída** | 100% | v0.1.12 validada pelo owner; actualização in-app e reparação de playlist confirmadas. Screenshots antigas removidas para `screenshots/legacy/`; captura actual fica para a próxima passagem de documentação. |
| Wave 1 — aquisição fiável | **Concluída e testada** | 100% | Destino persistido, confirmação, pré-indexação, revalidação de fontes/perfis, dedup por destino, saúde e rótulo **Completar álbum**. Validação do owner em 21-11-2026. Simulação completa, latência e testes >2.000 passam para Wave 1.1. |
| Wave 2 — UI/navegação | **Não iniciada** | 0% | Depende de estabilizar Wave 1 e Wave 3. |
| Wave 3 — confiança/biblioteca | **Fechada no âmbito atual** | 100% | Saúde, reparação, merge de duplicados, exportação para ficheiro e pesquisa unificada. Limitações em Wave 3.1. |
| Wave 4 — álbuns/capas | **Parcial** | ~80% | Confirmação de completar álbum, lista de faixas existentes/em falta (Deezer + iTunes), enriquecimento e capas de playlist. Faltam upload de capas e persistência/CloudSync das capas personalizadas. |
| Wave 5 — setup/jobs | **Fechada no âmbito atual** | 100% | `/start` apenas no setup, canal de arquivo, jobs persistidos e re-pesquisa. Testes crash/restart e gestão de fontes passam para Wave 5.1. |
| Wave 6 — descoberta/poder | **Em curso** | ~45% | Sleep timer, Android Auto Offline/Recent/Artists, auto-download, enriquecimento, pesquisa unificada, app lock e exportação JSON. Faltam widgets, scrobbling, smart playlists, temas e estatísticas. |
| Wave 7 — plataformas | **Não iniciada** | 0% | Tablet/TV/perfis; desktop continua congelado. |
| Wave 8 — conhecimento musical | **Em curso (MVP)** | ~25% | Dialog “Sobre este álbum” com MusicBrainz + Wikipédia, data, tipo, géneros, resumo e link para a fonte. Faltam páginas de artista, cache/ETag, faixas e créditos. |

**Wave 0 encerrada em 21-11-2026:** o ciclo de release e actualização in-app está provado; a v0.1.13 é a próxima release de correcções e não reabre a Wave 0.

**Leitura rápida:** o produto já é utilizável para o ciclo principal (biblioteca + aquisição + update), mas a paridade de produto está sobretudo nas Waves 1, 3, 4 e 5. A Wave 2 é a maior alteração visual e deve entrar depois de fechar a confiabilidade da biblioteca.

## 1. Regras do roadmap

- Cada wave termina com **código, testes, documentação e evidência de release**.
- Nenhuma wave declara concluída uma feature sem teste automatizado e, quando aplicável, teste live.
- Alterações destrutivas exigem pré-visualização, confirmação e undo.
- Não se misturam refactors amplos com correcções de release.
- Android é a prioridade; desktop continua congelado e fail-closed.
- A publicação automática de releases de avaliação foi usada até `v0.1.25` e fica **desativada por omissão**; novas versões exigem concordância do owner (`MLIB_AUTO_RELEASE=true` ou release manual).
- Cada release tem notas com: objectivo, alterações, limitações, testes e riscos.

## 2. Waves

### Wave 0 — Publicar releases de avaliação e validar actualização

**Objectivo:** provar o ciclo release → download → instalação in-app.

**Dependências:** autenticação GitHub, secrets de assinatura e Telegram já existentes.

**Tarefas**

- [x] Commit das docs pendentes.
- [x] Push de `main`; baseline verde.
- [x] Release `v0.1.12` pública, non-draft e non-prerelease.
- [x] Confirmar APKs arm64/x86_64, `SHA256SUMS.txt` e fingerprint do assinante.
- [x] Instalar `v0.1.11`/`v0.1.12` e confirmar actualização in-app.
- [x] Confirmar a reparação da playlist na 0.1.12.
- [ ] Smoke test live de aquisição completa (movido para a aceitação da Wave 1).
- [x] Corrigir o texto hardcoded `"Estado: …"` em `AcquisitionSheet.kt` (branch `fix/playlist-repair`).
- [x] Remover screenshots antigas Spotygram/russo do caminho activo; legado em `docs/screenshots/legacy/` e captura actual pendente.

**Aceitação**

- A app instalada em `v0.1.11` detecta, descarrega e instala `v0.1.12` sem perder dados.
- A versão instalada fica `0.1.10` e os dados da biblioteca permanecem.
- Nenhum crash/ANR no fluxo de actualização.
- Relatório de execução anexado à release.

**Risco principal:** autenticação GitHub e credenciais de assinatura.

---

### Wave 1 — Aquisição fiável: destino, fontes e colecções grandes

**Objectivo:** garantir que adquirir duas vezes não cria duplicados e que o destino é explícito.

**Itens:** SRC-01, ACQ-04, parte de JOB-01.

**Tarefas — concluídas**

- [x] Selector de qualquer chat/canal com permissão de publicação.
- [x] Persistir destino por conta Telegram.
- [x] Indexar/refrescar o destino **antes** de comparar duplicados.
- [x] Fontes de dedup configuráveis (destino vs todas as fontes; multi-seleção arbitrária fica para Wave 1.1).
- [x] Estimativa/aviso antes de adquirir (contagem de opções e aviso de seleções grandes).
- [x] Fluxo de aquisição com estado visível e confirmação.
- [x] Confirmação explícita com destino e contagem antes de encaminhar (ACQ-05 parte 1).
- [x] Nunca usar o canal CloudSync como destino musical.

**Validação:** owner confirmou a Wave 1 em 21-11-2026. A aceitação funcional principal está fechada.

### Wave 1.1 — hardening não bloqueante

Estes itens deixam de bloquear a Wave 1 e passam para a próxima fase de robustez:

- [ ] Modo de simulação completa sem encaminhar (ACQ-05 parte 2).
- [ ] Instrumentação de latência e esperas adaptativas (ACQ-06).
- [ ] Multi-seleção arbitrária de fontes de dedup.
- [ ] Testes live >100 mensagens e limite de 2.000.

**Aceitação**

- A mesma faixa adquirida duas vezes para o mesmo destino não duplica.
- Destino não indexado não gera duplicados no primeiro job.
- Colecção >100 mensagens mantém ordem e não perde faixas silenciosamente.
- O utilizador consegue cancelar/retomar sem perder sucessos já encaminhados.

**Métrica:** taxa de duplicados pós-aquisição = 0 nos cenários de teste.

---

### Wave 2 — Fundação visual e navegação “MLib Signal”

**Objectivo:** simplificar a navegação e dar identidade visual consistente, sem mudar o modelo de dados.

**Itens:** UX-01 a UX-05; tokens de design; acessibilidade; performance.

**Tarefas**

- [ ] Navegação principal com 4 destinos: Início, Procurar, Biblioteca, Definições.
- [ ] Mini-player persistente com progresso, play/pause e next.
- [ ] Início com “Continuar a ouvir”, recentes, favoritos e álbuns por completar.
- [ ] Biblioteca com chips `Faixas | Álbuns | Playlists | Artistas | Fontes`.
- [ ] Definições em ecrã completo com secções.
- [ ] Player full-screen com capa, gradiente da artwork, fila e controlos.
- [ ] Tokens: cores, tipografia, raios, espaçamento, elevação.
- [ ] Reduzir vidro/blur a navegação e mini-player; listas sólidas.
- [ ] Dynamic type, TalkBack, contraste, alvos ≥48 dp.
- [ ] Actualizar screenshots oficiais.

**Aceitação**

- Todas as funções actuais continuam acessíveis após a mudança de navegação.
- Nenhuma regressão de playback, biblioteca ou aquisição.
- Testes de acessibilidade e performance em dispositivo médio.
- Screenshots e README alinhados com a UI real.

**Nota:** inspiração Spotify na informação e hábitos; sem copiar verde, marca ou layout pixel-perfect.

---

### Wave 3 — Confiança na biblioteca: pesquisa, duplicados e merge

**Objectivo:** o utilizador saber o que tem, o que está duplicado e o que pode fundir.

**Itens:** SRCH-01, LIB-01, DED-02, DUP-01, ALB-02.

**Tarefas**

- [ ] Pesquisa unificada: biblioteca + Telegram + bot, com filtros e histórico.
- [x] Resumo “Biblioteca saudável” em Definições (branch `feat/source-revalidate`); detalhe/duplicados ainda pendentes.
- [ ] Estúdio de duplicados: comparar, escolher keeper, fundir metadata, undo.
- [ ] Preservar favoritos, playlists, álbuns e caminho local válido.
- [ ] Sugestões de merge de álbuns com pré-visualização; nunca automático.
- [ ] Golden tests de merge track/album.
- [ ] Exportar relatório da biblioteca (JSON/CSV/M3U) e auditoria operacional (LIB-02).

**Aceitação**

- Nenhum merge apaga ficheiros ou relações sem confirmação.
- O utilizador consegue reverter a última operação de merge.
- Duplicados ambíguos não são fundidos automaticamente.
- Pesquisa unificada encontra resultados locais e remotos sem bloquear a UI.

---

### Wave 4 — Álbuns completos e capas

**Objectivo:** transformar “completar álbum” e capas em fluxos de produto.

**Itens:** ALB-03, ALB-01, COV-01, PL-03.

**Tarefas**

- [x] Página de álbum com capa grande, gradiente e anel de completude. (capa grande; anel pendente)
- [x] “Completar álbum” com lista de faixas existentes, em falta e ambíguas. (existentes e em falta; ambíguas pendentes)
- [ ] Aprovação por faixa/edição antes de adquirir.
- [x] Enriquecimento iTunes → Deezer com cache e timeouts.
- [ ] Capas: upload local, online, URL, primeira faixa e presets.
- [x] Capas de playlist: upload, online, primeira faixa e presets.
- [ ] Placeholder explícito em vez de Unsplash como capa real.
- [ ] Persistência e CloudSync das capas personalizadas.

**Aceitação**

- Completar álbum mostra exactamente o que falta e não cria álbuns fictícios.
- Capas personalizadas sobrevivem a restart e sincronização.
- Enriquecimento não substitui metadata válida por placeholders.
- Edições diferentes não são fundidas.

---

### Wave 5 — Setup, jobs e gestão de fontes

**Objectivo:** primeira execução guiada e trabalhos recuperáveis.

**Itens:** SET-01, ONB-01, JOB-01, JOB-02.

**Tarefas**

- [x] Setup explícito do bot com `/start` apenas se não houver histórico; arquivo pessoal ainda pendente.
- [ ] Escolher/criar canal-arquivo privado, marcá-lo como fonte/destino.
- [ ] Manter CloudSync separado do arquivo musical.
- [ ] Estado persistido por opção/faixa, progresso e resultado.
- [ ] Retry apenas do que falta; não repetir opções concluídas.
- [ ] Cancelamento local e explicação do trabalho que continua no bot.
- [ ] Gestão de fontes: adicionar, desactivar, reindexar, remover.
- [ ] Testes de crash/restart antes/depois de forward.

**Aceitação**

- Um restart não repete faixas já importadas.
- Setup pode ser ignorado e refeito sem apagar dados.
- Fonte/canal pode ser reindexado com progresso visível.
- Nenhum job fica eternamente “em execução”.

---

### Wave 6 — Descoberta e funcionalidades de poder

**Objectivo:** tornar a biblioteca mais útil no dia-a-dia.

**Itens:** ART-01, FD-01 a FD-11.

**Tarefas**

- [ ] Páginas de artista com álbuns, faixas e fontes.
- [ ] Smart downloads: favoritos/álbuns/playlists com limites de storage.
- [ ] Retoma de fila/posição entre dispositivos via CloudSync (metadata).
- [ ] Widgets e quick settings: now playing, continuar, saúde da biblioteca.
- [ ] Android Auto melhorado: Início, Álbuns, Playlists, Favoritos, Offline.
- [ ] Import/export M3U/JSON, backup e restore.
- [ ] Bloqueio biométrico, fontes ocultas e modo privado.
- [ ] Scrobbling opcional (ListenBrainz/Last.fm), desligado por defeito.
- [ ] Sleep timer, velocidade, equalizador, gapless/crossfade conforme Media3.
- [ ] Pesquisa por voz e comandos simples.
- [ ] Smart playlists por regras.
- [ ] Partilha rica de álbum/playlist.
- [ ] Estatísticas pessoais opt-in.
- [ ] Temas adicionais: artwork-driven, alto contraste, AMOLED.

**Aceitação**

- Cada feature é opt-in e não bloqueia o fluxo principal.
- Nenhum SDK de analytics obrigatório.
- Consumo de bateria/rede medido em smart downloads e scrobbling.
- Android Auto validado em head-unit ou emulador oficial.

---

### Wave 7 — Plataformas e consolidação

**Objectivo:** expandir apenas depois de Android estar estável.

**Itens:** TV, tablet/foldable, perfis, desktop (continua congelado).

**Tarefas**

- [ ] Layout two-pane para tablet/foldable.
- [ ] Android TV/Google TV: foco, D-pad, legibilidade.
- [ ] Perfis/família na mesma conta, com isolamento de biblioteca.
- [ ] Reavaliar desktop apenas quando houver chave MLib e updater migrado.
- [ ] Auditoria final de acessibilidade, performance e segurança.
- [ ] Documentação de suporte e limitações.

**Aceitação**

- Nenhuma plataforma é declarada suportada sem validação real.
- Desktop permanece desactivado/fail-closed até decisão explícita.
- Perfis não misturam dados nem sincronização.

### Wave 8 — Conhecimento musical (artista, álbum, faixa)

**Objectivo:** enriquecer a biblioteca com informação contextual **com fonte**, sem alterar tags locais nem inventar factos.

**Fontes propostas**

- **MusicBrainz** — identidade canónica (MBID), datas de formação/nascimento, lançamentos, relações e créditos. Dados CC0, limite ~1 pedido/s, User-Agent identificado.
- **Wikidata / Wikipédia** — biografia, história, origem, membros e contexto; texto com atribuição CC BY-SA e link para a fonte.
- **Deezer / iTunes** — datas, género e capas já usados; sem chaves novas.
- **Discogs / Last.fm** — apenas se forem aceites os termos e a gestão de chaves; não são necessários para o MVP.

**Dados e regras**

- Tabelas locais `artist_metadata` e `album_metadata` com MBID, resumo, língua, datas, origem, membros, géneros, fontes, licença, `fetched_at` e ETag.
- Cache com TTL e revalidação; funcionamento offline com o último resumo válido.
- Matching por MBID/ISRC; fuzzy apenas para sugerir, nunca para substituir automaticamente.
- Nunca sobrescrever título/artista/ano locais sem confirmação.
- Curiosidades/“trivia” apenas quando existir uma secção/fonte verificável; sem texto gerado por IA apresentado como facto.
- Imagens só com licença compatível e atribuição; nunca incorporar imagens sem licença.

**UI**

- Página de artista: imagem licenciada, resumo, origem, datas, géneros, discografia e link “Ler mais na fonte”.
- Álbum: data de lançamento, editora, género, créditos e contexto com fonte.
- Faixa: compositor/letrista quando disponível.
- Badge “Informação de MusicBrainz/Wikipédia” e data da última atualização.
- Fallback claro quando não há enriquecimento.

**Privacidade e limites**

- Pedidos a APIs públicas sem dados de conta; divulgar que o nome do artista/álbum é enviado.
- Respeitar rate limits, backoff e cache; sem chamadas em segundo plano agressivas.
- Não enviar chaves privadas no APK; evitar Last.fm/Discogs se exigirem segredo embutido.

**Fases**

1. **8.1** — MusicBrainz: identidade, datas, lançamentos e créditos. (MVP de data/tipo/géneros entregue)
2. **8.2** — Wikipédia/Wikidata: resumo e contexto com atribuição. (MVP de resumo + link entregue)
3. **8.3** — páginas de artista e discografia.
4. **8.4** — fontes opcionais (Last.fm/Discogs), só com termos/keys definidos.

**Aceitação**

- Nenhuma tag local é alterada automaticamente.
- Toda a informação tem fonte, licença e data de recolha.
- Cache offline e fallback funcionam.
- Parser e matching têm fixtures; chamadas live têm limites e erros tratados.
- A UI distingue claramente “dados locais” de “dados externos”.

**Prioridade:** P2/P3, depois das Waves 4 e 6. Não bloqueia releases atuais.
## 3. Backlog completo por wave

| ID | Item | Wave | Prioridade | Depende de |
|---|---|---|---|---|
| REL-01 | Publicar 0.1.10 e testar update in-app | 0 | P0 | auth GitHub |
| UI-00 | Corrigir “Estado:” hardcoded e screenshots | 0 | P0 | — |
| LIVE-01 | Aceitação live T01–T20 | 0/6 | P0 | 0.1.10 |
| SRC-01 | Destino, fontes e indexação antes da dedup | 1 | P1 | — |
| ACQ-04 | Paginação >100 e limite 2.000 | 1 | P1 | — |
| ACQ-05 | Confirmação feita; simulação/arm completa pendente | 1 | P1 | SRC-01 |
| ACQ-06 | Latência e cliques adaptativos | 1 | P2 | ACQ-01 |
| LIB-02 | Auditoria e exportação operacional | 3 | P2 | LIB-01 |
| UX-06 | Fluxo de aquisição em passos e estimativa | 1 | P1 | SRC-01 |
| UX-01 | Navegação 4 destinos | 2 | P1 | — |
| UX-02 | Design tokens “MLib Signal” | 2 | P1 | — |
| UX-03 | Início com shelves | 2 | P1 | UX-01 |
| UX-04 | Biblioteca com filtros | 2 | P1 | UX-01 |
| UX-05 | Player e mini-player | 2 | P1 | UX-01 |
| SRCH-01 | Pesquisa unificada | 3 | P1 | UX-01 |
| LIB-01 | Biblioteca saudável | 3 | P1 | UX-04 |
| DED-02 | Merge de metadata de faixas | 3 | P1 | — |
| DUP-01 | Revisão de duplicados | 3 | P1 | DED-02 |
| ALB-02 | Sugestões de merge de álbuns | 3 | P2 | LIB-01 |
| ALB-03 | Completar álbum com pré-visualização | 4 | P1 | SRC-01 |
| ALB-01 | Enriquecimento de álbuns | 4 | P2 | ALB-03 |
| COV-01 | Gestor de capas de álbum | 4 | P2 | — |
| PL-03 | Gestor de capas de playlist | 4 | P2 | — |
| SET-01 | Setup do bot/arquivo | 5 | P2 | — |
| ONB-01 | Onboarding e confirmação `/start` | 5 | P2 | SET-01 |
| JOB-01 | Estado por opção e retry | 5 | P1 | SRC-01 |
| JOB-02 | Retoma robusta a crash/restart | 5 | P1 | JOB-01 |
| ART-01 | Páginas de artista | 6 | P2 | ALB-01 |
| FD-01 | Smart downloads | 6 | P2 | LIB-01 |
| FD-02 | Retoma de fila entre dispositivos | 6 | P2 | CloudSync |
| FD-03 | Widgets/quick settings | 6 | P2 | UX-05 |
| FD-04 | Android Auto melhorado | 6 | P2 | UX-05 |
| FD-05 | Import/export/backup | 6 | P2 | LIB-01 |
| FD-06 | Bloqueio biométrico/privacidade | 6 | P2 | — |
| FD-07 | Scrobbling opcional | 6 | P3 | — |
| FD-08 | Sleep timer/speed/equalizador | 6 | P3 | UX-05 |
| FD-09 | Voz/comandos | 6 | P3 | SRCH-01 |
| FD-10 | Smart playlists | 6 | P3 | LIB-01 |
| FD-11 | Partilha/estatísticas/temas | 6 | P3 | UX-02 |
| FD-12 | Tablet/foldable | 7 | P3 | UX-01 |
| FD-13 | Android TV | 7 | P3 | UX-05 |
| FD-14 | Perfis/família | 7 | P3 | LIB-01 |

## 4. Cross-cutting (todas as waves)

- **Testes:** unit/golden para regras; integração SQLite/CloudSync; UI tests; live Telegram.
- **Segurança:** sem segredos em logs; permissões mínimas; updates verificados; capas/imports com limites.
- **Privacidade:** sem analytics SDK; scrobbling/estatísticas opt-in; fontes ocultas.
- **Acessibilidade:** TalkBack, contraste, dynamic type, foco, alvos.
- **Performance:** startup, jank, memória, bateria, tamanho APK.
- **Documentação:** cada wave actualiza README, roadmap, screenshots, release notes e limitações.
- **Release:** versão, tag, APKs, checksums, fingerprint, notas e migração.

## 5. Ordem de execução recomendada

1. **Wave 0** — desbloquear auth, publicar 0.1.10, testar update.
2. **Wave 1** — destino/indexação e colecções grandes.
3. **Wave 2** — navegação e base visual.
4. **Wave 3** — pesquisa, saúde e merge.
5. **Wave 4** — completar álbum e capas.
6. **Wave 5** — setup e jobs.
7. **Wave 6** — descoberta e poder.
8. **Wave 7** — plataformas.
9. **Wave 8** — conhecimento musical com fontes abertas.

## 6. Critérios globais de conclusão

Uma wave só está concluída quando:

- tarefas e critérios de aceitação estão marcados com evidência;
- testes automatizados passam no CI;
- teste live aplicável foi executado e registado;
- documentação e screenshots estão actualizados;
- release notes indicam limitações;
- não há regressões conhecidas de dados, playback, sync ou update.

## 7. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Auth GitHub | Bloqueia release | PAT/credential manager; validar antes do push |
| Assinatura | Update rejeitado | Fingerprint pin e teste de update |
| Bot/FloodWait | Aquisição falha | Backoff, retry, estado parcial, live tests |
| Merge/dedup | Perda de dados | Pré-visualização, undo, golden tests |
| Blur/glass | Jank | Limitar a nav/player; medir |
| Scope creep | Atraso | Waves pequenas e critérios |
| Desktop | Distracção | Congelado até decisão explícita |
| MCat Player inacessível | Paridade desactualizada | Reauditar quando houver acesso; baseline `a41cf5c` |

## 8. Próxima acção

Assim que o push for possível:

1. commit das docs pendentes;
2. push;
3. acompanhar baseline;
4. confirmar `v0.1.12`;
5. executar Wave 0 e só depois iniciar Wave 1.
