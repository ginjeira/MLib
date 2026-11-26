# MLib — roadmap de implementação por waves

**Versão:** 1.0  
**Data:** 20-11-2026  
**Âmbito:** Android primeiro. Desktop permanece congelado.  
**Documentos de referência:** `docs/mlib/PRODUCT-UX-PROPOSAL.md`, `docs/mlib/MCAT-PLAYER-PORT-STATUS.md`, `docs/mlib/PORTABILITY-GAPS-REPORT.md`, `docs/mlib/PORTABILITY-CONTRACT.md`.

Este é o plano de execução corrente. O histórico detalhado permanece em `docs/mlib/ROADMAP.md`.

## 0. Estado das versões 0.1.27 a 0.1.33

Verificado em **26-11-2026, 13:47 UTC**.

| Item | Estado |
|---|---|
| Última release pública | **v0.1.33** (publicada em 26-11-2026 13:31:34) |
| Tag `v0.1.27` | **Criada** (`fda3eb0`); build `45039489724` |
| Tag `v0.1.28` | **Criada** (`6ccff1b`); build `45043559683` |
| Tag `v0.1.29` | **Criada** (`b8c5162`); build `45047965275` |
| Tag `v0.1.30` | **Criada** (`980d09f`); build `45061552047` |
| Tag `v0.1.31` | **Criada** (`f4d08ca`); build `45088467464` |
| Tag `v0.1.32` | **Criada** (`6c2a3a0`); build `45803904113` |
| Tag `v0.1.33` | **Criada** (`0532428`); build `45823768450` |
| Packages | 0.1.27 / 1027, 0.1.28 / 1028, 0.1.29 / 1029, 0.1.30 / 1030, 0.1.31 / 1031, 0.1.32 / 1032 e 0.1.33 / 1033 |
| Assinatura | `6e8fc1d09b55e70c6183b8cbbebad96d935f01a24558af356b697a942541e200` |
| Publicação automática | **Desativada**; cada versão é acordada com o owner |
| Próxima versão | **A acordar**; Wave 1 do servidor em validação; **Google TV congelada** |

**Hashes publicados (0.1.33):**

- `mlib-0.1.33-arm64.apk` — `19e435630b5fa2904252b791842ba63bb10e23d915aba313956067e78588917d`
- `mlib-0.1.33-x86_64.apk` — `198890e6be00f94c87e3a390cef393137f744dccc5c21fc5b8edd3ea9f9f91f3`
- `SHA256SUMS.txt` — `e56eedbb07f1cd8ebf267bc34259ae5f4399c674a711379e24f37420ceffacf7`

**Hashes publicados (0.1.32):**

- `mlib-0.1.32-arm64.apk` — `1c41e9eece29859556c8785f9d6bcdd856abe53342e49829321e4ece3f491593`
- `mlib-0.1.32-x86_64.apk` — `79a3b3bb59b8fe8497f34f52162c2bfc2024d984af027c645d249eb1375bd0eb`
- `SHA256SUMS.txt` — `5e4e7d3f5507765e37dcf472e599f9b8c9af0034e2314094c2c90ee85864f940`

**Hashes publicados (0.1.31):**

- `mlib-0.1.31-arm64.apk` — `29487e747d7e200b8d1a869c2d31e5b1c435519f67acf3c24231e2f33559eb1c`
- `mlib-0.1.31-x86_64.apk` — `1c3ded77fe0d1419de7bac4734a96906105ed178a8771a249d93dffa93f62c2c`
- `SHA256SUMS.txt` — `e55279021365bda77e69cb83f65c75cb673cc2c1af069353c6f2fa887aa87a28`

**Hashes publicados (0.1.30):**

- `mlib-0.1.30-arm64.apk` — `6e9c06cbcc4962de08fdcbbdd5457ce240cbbcc7afe4b33129bbc48f0070fec5`
- `mlib-0.1.30-x86_64.apk` — `0a0d261e6457d220151cda5c9841dfd534b35740831620a8a2c3ee62ab74f455`
- `SHA256SUMS.txt` — `76b677ced0e06a66d3b869f3a51b03a8571962f62dd2bdd0fcd5d32ebf459fd6`

**Hashes publicados (0.1.29):**

- `mlib-0.1.29-arm64.apk` — `2c2f74c6dc597d245a60d52d36f683f517c36fd9546784d67e25764c7b563e7b`
- `mlib-0.1.29-x86_64.apk` — `38fee437d4bddd69e3f325d13dc54e5f878445c51d53b3c625ea1d1ff35d0848`
- `SHA256SUMS.txt` — `f0f28f722a5ed15711f646b7e1d5317d00812c1f0106492b8acf8c45661c8eea`

**Hashes publicados (0.1.28):**

- `mlib-0.1.28-arm64.apk` — `efab5104a5feffe24fb2e8126ba8e4f27d3c146ab0180ad83ee3242883f1584c`
- `mlib-0.1.28-x86_64.apk` — `5da8eb4bf25f7b3f00d2dbb45ca8a90fa77ea001e83b0b55495de0f4d46efe47`
- `SHA256SUMS.txt` — `cf66a27baa03d0e4194951e4b88cc0681ecbe8e402896733f05803d18836b6e7`

**Hashes publicados (0.1.27):**

- `mlib-0.1.27-arm64.apk` — `cff337aaa7eb77da5dc831e4569ab3f06c9abea5331e5bfa8381e56cc5280879`
- `mlib-0.1.27-x86_64.apk` — `6e388daf030e6fa4969666644a95d4d166226dcc51a00ab90597f192c141a7d9`
- `SHA256SUMS.txt` — `4eb98fb62b4c4d4b80059abcfca9d96b5d936d3a22ba43ee9d09c258a14ab4ae`

**Conteúdo por versão:**

- **0.1.27:** fix de “Completar álbum”, revisão de aquisição antes de escrever na biblioteca, app lock, Wave 8 (conhecimento com fonte e cache), capas personalizadas, anel de completude, temas AMOLED/contraste e reparação de biblioteca.
- **0.1.28:** Android Auto completo para reprodução (Início, pesquisa por voz na biblioteca, notificações e capas personalizadas).
- **0.1.29:** Wave 2 — navegação de 4 destinos, Início, Biblioteca com chips e artistas, Procurar, Definições em ecrã completo, gradiente da arte no player e superfícies sólidas.
- **0.1.30:** correções — recolha de playlists espera a contagem anunciada, cancelamento de pesquisa silencioso, cancelamento de obtenção com `/cancel` ao bot, página de artista e conhecimento localizado, aquisição pré-preenchida e confirmação de downloads offline.
- **0.1.31:** CloudSync transporta capas de faixa (favoritas já viajavam), formato 6 compatível com pacotes antigos e segredos sempre excluídos.
- **0.1.32:** correção do Android Auto — escolher uma faixa inicia a reprodução em vez de limpar a fila partilhada com o telemóvel.
- **0.1.33:** Android Auto com play e anterior/seguinte em playlists, álbuns, artistas, favoritas, offline, recentes e todas as faixas; a lista visível passa a ser a fila ao escolher uma faixa.

## 0.1 Entregue em `main` depois da tag `v0.1.26` (não publicado)

- Revisão antes de escrever na biblioteca: `AcquisitionReview`, `commitReview(selectedIds)` e rejeição com remoção das mensagens encaminhadas.
- App lock opcional com o bloqueio do dispositivo (`Platform.supportsAppLock`, `app_lock`).
- Reparação global de faixas indisponíveis em Definições (branch `feat/library-repair`).
- Wave 8 (MVP): `Knowledge.album()` com MusicBrainz + Wikipédia, dialog “Sobre este álbum” com fonte, data, tipo e géneros.
- Wave 4: “Completar álbum” passou a mostrar a lista de faixas existentes e em falta (Deezer, fallback iTunes) e deixou de ficar aninhado no dialog de edição.
- Wave 4: capas personalizadas de álbum e playlist a partir do dispositivo, guardadas como data URI em `cover_url` (persistem e sincronizam); presets Unsplash substituídos por placeholder explícito; anel de completude no detalhe do álbum.
- Wave 8: cache em disco do conhecimento (TTL 30 dias, 300 entradas), licença por fonte, data de recolha e parsers com fixtures.
- Temas AMOLED e alto contraste, com validação de sync e testes.
- Android Auto completo para reprodução: Início, pesquisa por voz (biblioteca), notificações de todas as ramificações e capas personalizadas; aquisição continua fora do carro por política do Android Auto.
- Correções pós-0.1.29: recolha de playlists espera pela contagem anunciada (o silêncio de 9 s já não fecha 1 de 27), “Procurar no Telegram” abre o diálogo pré-preenchido sem pesquisar, confirmação de descarregar offline por artista e conhecimento na língua da app.
- Validação local: `:desktop:test` com **111 testes, 0 falhas**; `:app:compileDebugKotlin` e `:app:processDebugResources` verdes.

## 0.2 Progresso por wave — 21-11-2026

| Wave | Estado | Progresso | Notas |
|---|---|---:|---|
| Wave 0 — release/update | **Concluída** | 100% | v0.1.12 validada pelo owner; actualização in-app e reparação de playlist confirmadas. Screenshots antigas removidas para `screenshots/legacy/`; captura actual fica para a próxima passagem de documentação. |
| Wave 1 — aquisição fiável | **Concluída e testada** | 100% | Destino persistido, confirmação, pré-indexação, revalidação de fontes/perfis, dedup por destino, saúde e rótulo **Completar álbum**. Validação do owner em 21-11-2026. Simulação completa, latência e testes >2.000 passam para Wave 1.1. |
| Wave 2 — UI/navegação | **Fechada no âmbito atual** | 100% (âmbito) | 4 destinos (Início, Biblioteca, Procurar, Definições), Home com shelves, chips Faixas/Favoritos/Playlists/Álbuns/Artistas, página de artista com álbuns, faixas, fontes e “Sobre”, gestão de fontes em Definições, pesquisa local com hand-off e mini-player. Definições em ecrã completo, gradiente ambiente no player e superfícies sólidas. Falta apenas validação de acessibilidade e captura de screenshots em dispositivo. |
| Wave 3 — confiança/biblioteca | **Fechada no âmbito atual** | 100% | Saúde, reparação, merge de duplicados, exportação para ficheiro e pesquisa unificada. Limitações em Wave 3.1. |
| Wave 4 — álbuns/capas | **Fechada no âmbito atual** | 100% | Completar álbum com faixas existentes/em falta e anel de completude, enriquecimento, capas personalizadas de álbum e playlist a partir do dispositivo (persistem via `cover_url` e sincronizam), placeholder explícito. Gradiente/artwork grande e faixas ambíguas passam para Wave 4.1. |
| Wave 5 — setup/jobs | **Fechada no âmbito atual** | 100% | `/start` apenas no setup, canal de arquivo, jobs persistidos e re-pesquisa. Testes crash/restart e gestão de fontes passam para Wave 5.1. |
| Wave 6 — descoberta/poder | **Fechada no âmbito atual** | 100% (âmbito) | Sleep timer, Android Auto Offline/Recent/Artists, auto-download, enriquecimento, pesquisa unificada, app lock, exportação JSON, temas AMOLED/alto contraste e reparação de biblioteca. Widgets, scrobbling, smart playlists, voz, estatísticas e backup completo passam para Wave 6.1. |
| Wave 7 — plataformas | **Congelada** | 0% | Google TV adiada por decisão do owner (22-11-2026); tablet/TV/perfis e desktop continuam fora de âmbito. |
| Wave 8 — conhecimento musical | **Fechada no âmbito MVP** | 100% (MVP) | Álbum com MusicBrainz + Wikipédia, data, tipo, géneros, resumo, licença por fonte, link e data de recolha; cache em disco com TTL de 30 dias e limite de 300 entradas; parsers com fixtures. Páginas de artista, discografia, créditos e ETag passam para Wave 8.1. |

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

- [x] Navegação principal com 4 destinos: Início, Procurar, Biblioteca, Definições.
- [x] Mini-player persistente com progresso, play/pause e next.
- [x] Início com “Continuar a ouvir”, recentes, favoritos, playlists e álbuns, com contagem de faixas em falta quando conhecida.
- [x] Biblioteca com chips `Faixas | Favoritos | Playlists | Álbuns | Artistas`; gestão de fontes em Definições (decisão do owner).
- [x] Definições em ecrã completo com back, alcançável pela barra, rail largo e cabeçalho.
- [x] Player full-screen com capa, gradiente ambiente da artwork, fila e controlos.
- [x] Tokens partilhados (`MlTokens`) e uso nas superfícies revistas.
- [x] Navegação e mini-player sem vidro forte: superfícies sólidas com contorno.
- [ ] Dynamic type, TalkBack, contraste e alvos ≥48 dp: verificação automática parcial; falta teste em dispositivo.
- [ ] Actualizar screenshots oficiais (requer captura em dispositivo).

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

- [x] Página de álbum com anel de completude (“X de Y faixas · Z em falta”). Arte grande e gradiente em Wave 4.1.
- [x] “Completar álbum” com lista de faixas existentes e em falta. Faixas ambíguas em Wave 4.1.
- [x] Aprovação por faixa antes de escrever na biblioteca (revisão de aquisição com checkboxes).
- [x] Enriquecimento iTunes → Deezer com cache e timeouts.
- [x] Capas: imagem local do dispositivo (JPEG 512 px), URL e pesquisa online. Presets de primeira faixa em Wave 4.1.
- [x] Capas de playlist: imagem local, URL e pesquisa online.
- [x] Placeholder explícito em vez de Unsplash como capa real.
- [x] Persistência e sincronização das capas personalizadas (guardadas em `cover_url` como data URI e sincronizadas com a metadata).

### Wave 4.1 — refinamento visual

- [ ] Capa grande com gradiente no detalhe do álbum.
- [ ] Marcar faixas ambíguas (título igual, duração muito diferente).
- [ ] Presets de capa pela primeira faixa e por género, sem URLs externas fixas.

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

**Tarefas — entregue no âmbito atual**

- [x] Android Auto: Início, todas as faixas, playlists, álbuns, artistas, favoritos, offline e recentes; pesquisa por voz do Assistente limitada à biblioteca; capas `data:`; testes do browse tree.
- [x] Auto-download de favoritos com limite de storage.
- [x] Exportação JSON da biblioteca.
- [x] Bloqueio com o dispositivo (app lock).
- [x] Sleep timer e velocidade de reprodução.
- [x] Temas alto contraste e AMOLED.
- [x] Pesquisa unificada local + bot.

**Wave 6.1 — restante do âmbito**

- [x] Páginas de artista com álbuns, faixas e fontes, tocar/aleatório/offline e “Sobre” com fonte na língua da app (Wikipédia PT com fallback EN; tipos do MusicBrainz traduzidos; géneros mantêm o texto original da comunidade).
- [ ] Retoma de fila/posição entre dispositivos via CloudSync.
- [ ] Widgets e quick settings.
- [ ] Import/export M3U, backup e restore completo.
- [ ] Fontes ocultas e modo privado.
- [ ] Scrobbling opcional (ListenBrainz/Last.fm).
- [ ] Pesquisa por voz e comandos.
- [ ] Smart playlists por regras.
- [ ] Partilha rica de álbum/playlist.
- [ ] Estatísticas pessoais opt-in.
- [ ] Equalizador e gapless/crossfade.

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

### Wave 8.1 — restante do âmbito

- [ ] Página de artista: resumo, origem, datas, géneros e discografia.
- [ ] Créditos de faixa (compositor/letrista) quando disponíveis.
- [ ] Cache com ETag/revalidação condicional e badge de “desactualizado”.
- [ ] Cobertura de testes live com limites de pedidos documentados.

**Âmbito MVP entregue:** “Sobre este álbum” com data de lançamento, tipo, géneros, resumo, fonte com licença, link e data de recolha; cache em disco com TTL de 30 dias e 300 entradas; parsers de MusicBrainz e Wikipédia com fixtures; nenhuma tag local é alterada.

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

## 7.1 Pedidos futuros

Ideias registadas, por amadurecer; não estão comprometidas em nenhuma versão.

### FR-01 — Mixes automáticos (“Feito para si”, “Mix do artista A”)

**Data:** 22-11-2026. **Estado:** ideia em maturação; não implementar sem revisão.

**Objectivo:** gerar mixes virtuais a partir da biblioteca, no estilo “Made for you”, sem alterar ficheiros nem tags.

**MVP sem histórico (regras):**
- Mix do artista: ≥8 faixas tocáveis e ≥2 álbuns; 25–30 faixas proporcionais aos álbuns (máx. 2–3 por álbum), favoritas primeiro e deep cuts.
- Essenciais do artista: 1–2 faixas por álbum + favoritas.
- Mais como A: artistas relacionados por género/época/partilha, com diversidade.
- Novidades, favoritos escondidos e “Feito para si” em cold start (favoritos + recentes + álbuns variados).

**Fase com sinais locais:** `play_count`, `last_played`, `skip_count` por faixa, guardados só no dispositivo; alimentam redescobertas, anti-repetição (48 h) e “Feito para si” diário com seed determinístico.

**Decisões em aberto:** onde vivem os mixes (virtuais vs playlist materializada); regras de diversidade; limiares por artista; explicabilidade do cartão; opt-out por tipo; se o histórico pode ou não entrar no CloudSync (recomendação: nunca).
### FR-12 — CloudSync completo, sem segredos

**Data:** 23-11-2026. **Estado:** requisito registado; corrige lacunas do sync atual.

**Problema corrigido no diagnóstico:** as favoritas **já sincronizam** através de registos `favorite/{id}`. O que faltava era a **capa de faixa** (`art`), ausente do descritor. Caminhos locais e estado offline não devem viajar.

**Incluir no sync:**
- `art` (capa de faixa) com validação `http`/`https`/`data:image` e limite de tamanho.
- Manter as favoritas via `favorite/{id}` (já existente).
- Manter o que já existe: origens, descritores base, playlists (nome/descrição/capa/ordem/membros), álbuns (metadados/capa/ordem), definições da whitelist.

**Excluir sempre (segredos e estado local):**
- `api_id`/`api_hash`, sessões TDLib, chaves de base de dados, passwords, tokens de convite, TOTP.
- Caminhos locais (`path`), estado de download/offline, `available`, ficheiros importados.

**Requisitos técnicos:**
- `FORMAT` 5 → 6, com leitura retrocompatível de pacotes antigos (sem `art`).
- Validação estrita dos novos campos no protocolo.
- Testes: ida-e-volta de favoritas e capas; payload nunca contém segredos nem caminhos.
- Merge por último escritor por campo, como no resto do sync.

**Aceitação:** dois dispositivos convergem favoritas e capas de faixa; pacotes antigos continuam a ser lidos; inspeção do payload confirma ausência de segredos; servidor restaura favoritas/capas no onboarding.

## 8. Próxima acção

Assim que o push for possível:

1. commit das docs pendentes;
2. push;
3. acompanhar baseline;
4. confirmar `v0.1.12`;
5. executar Wave 0 e só depois iniciar Wave 1.
## 9. Waves 9–11 — acabamento profissional

Registadas por pedido do owner a **22-11-2026**. Não estão atribuídas a nenhuma versão; entram quando acordado.

### Wave 9 — Sincronização transparente

Objectivo: o utilizador sabe sempre o que está sincronizado e resolve conflitos sem medo.

- [ ] Indicador no cabeçalho: “Sincronizado há X” / “N alterações por enviar”.
- [ ] Ecrã “Estado da sincronização”: último envio/receção por dispositivo, o que mudou, tamanho e hora.
- [ ] Conflitos com pré-visualização e três opções: manter esta, manter a outra, manter ambas.
- [ ] “Sincronizar agora” e “Repetir envio” para itens falhados.
- [ ] Explicitar o que nunca sincroniza: histórico de reprodução, diagnósticos e cache.
- [ ] Mensagens de falha accionáveis (sem ligação, sessão expirada, etc.).

**Aceitação:** nenhum conflito é resolvido silenciosamente; qualquer falha tem motivo e próximo passo.

### Wave 10 — Reprodução de nível produto

Objectivo: controlo e continuidade ao nível das apps comerciais.

- [ ] Secção “Qualidade de reprodução”: gapless, crossfade com duração, normalização de volume (ReplayGain), equalizador com presets e reset.
- [ ] Sleep timer com fade-out e “terminar faixa atual”.
- [ ] Fila editável: tocar a seguir, reordenar, remover, tudo com Desfazer.
- [ ] Continuidade: “Continuar de onde parou?”, comportamento ao desligar Bluetooth e foco de áudio.
- [ ] Controlos consistentes em notificação, lockscreen e Android Auto.

**Aceitação:** toggles persistem, têm valores por defeito seguros e nenhuma alteração de som acontece sem indicação visível.

### Wave 11 — Primeira impressão e acessibilidade

Objectivo: a app parecer madura desde a instalação e ser usável por todos.

- [ ] Onboarding curto: Telegram, destino, fontes e importação, com “Saltar”.
- [ ] Estados vazios com ações (Adquirir, Importar) e carregamento com skeletons.
- [ ] Erros com motivo, ação recomendada e “Tentar novamente”.
- [ ] Pesquisa nas Definições e microcopy PT-PT consistente.
- [ ] Auditoria TalkBack, dynamic type, contraste nos quatro temas, alvos ≥48 dp.
- [ ] Haptics leves, animações curtas e ícones coerentes.

**Aceitação:** nenhum ecrã fica sem saída; critérios de acessibilidade verificados em dispositivo.
## 10. Future release — candidatas

Registadas a **22-11-2026**, por amadurecer. Não estão atribuídas a versão nem avaliadas em esforço.

| ID | Ideia | Valor | Notas |
|---|---|---|---|
| FR-02 | **Dedup e upgrade por fingerprint de áudio** | Alto | Chromaprint local; agrupar gravações reais, escolher a melhor cópia, substituir com undo. Não depende de títulos. |
| FR-03 | **Detetive de edições** | Alto | Fingerprint + MusicBrainz para identificar a edição exata e listar o que falta; complementa “Completar álbum”. |
| FR-04 | **Sala de escuta LAN / handoff por QR** | Médio-alto | Reprodução sincronizada entre dispositivos na mesma rede, sem servidor; transferir fila e posição por QR. |
| FR-05 | **Letras sincronizadas offline** | Médio | Fonte aberta (ex. LRCLIB), cache local, salto por linha e atribuição. |
| FR-06 | **Wrapped local** | Médio | Retrospectiva anual gerada no dispositivo, imagem partilhável, sem servidor. |
| FR-07 | **Watchlist de lançamentos** | Médio | Seguir artistas via MusicBrainz e avisar de releases; apenas informa. |
| FR-08 | **Curador de biblioteca** | Médio | Relatório acionável: duplicados, capas em falta, offline desaparecido, álbuns incompletos, metadata em falta. |
| FR-09 | **Blind A/B de qualidade** | Baixo | Comparar cópias às cegas para decidir upgrades. |
| FR-10 | **Prateleira por cor/humor** | Baixo | Paleta da capa + tags de humor manuais. |
| FR-11 | **MLib Server + web UI (perfis independentes)** | Alto | Servidor caseiro para iPhones: um perfil por adulto, conta Telegram própria, biblioteca e consumo de bot isolados; streaming por Range e PWA. Proposta em `docs/mlib/SERVER-PROPOSAL.md`. |
| FR-12 | **CloudSync completo (sem segredos)** | Alto | Sincronizar favoritas, capas de faixa e restantes dados de curadoria; excluir sempre segredos (sessões, chaves, passwords, TOTP). |

**Excluído por princípio:** recomendação por collaborative filtering ou upload de hábitos; separação de vocais/stems; integrações com serviços de streaming.

