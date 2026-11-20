# MLib — estado da portabilidade MCat Player

**Data:** 20-11-2026  
**MLib auditado:** `993eafb` (`fix: address independent audit findings`; ainda por publicar quando este documento foi escrito)  
**Release pública de referência:** `v0.1.9` (`d7c68d2`)  
**MCat Player — último baseline auditado:** `a41cf5ceeea023d7b86484cf5c7bf5a7ae7b2132`  
**Âmbito:** comportamento funcional, não cópia da arquitectura Web/PWA, servidor, GramJS ou API.

## 0. Limitação de acesso importante

Em 20-11-2026, `https://github.com/ginjeira/MCat-Player` devolveu **HTTP 404** na API e no arquivo de código. O repositório está privado, foi movido ou foi apagado; não foi possível reauditar o HEAD actual do MCat Player.

Este documento usa:

- o último baseline auditado e registado no MLib: `a41cf5c`;
- o inventário funcional de `docs/mlib/PORTABILITY-GAPS-REPORT.md`, `PORTABILITY-CONTRACT.md`, `MCAT-INTEGRATION.md` e `PORTABILITY-WAVE-0.md`;
- inspecção directa do código MLib actual em `993eafb`.

**Consequência:** alterações feitas no MCat Player depois de `a41cf5c` não estão refletidas. Antes de declarar paridade, é obrigatório repetir a auditoria com acesso ao commit actual (ver secção 8).

## 1. Origem: MCat e MCat Player

O MCat original está público em `https://github.com/ginjeira/MCat` (Python/Telethon, HEAD auditado `78e8279`). O MCat Player é a evolução Web/JavaScript que foi usada como referência de produto. O MLib está a portar **comportamento**, não a stack Python/Telethon/Express.

Três diferenças do MCat original que são relevantes para o roadmap:

1. **`/arm` e `/live`** — o MCat tinha um modo de simulação e só encaminhava depois de uma autorização explícita. O MLib actual encaminha logo após a selecção; falta um passo de pré-visualização/arm antes do envio real.
2. **Diagnóstico operacional** — `/music-check`, `/duplicates`, `/hist-music-find`, `/status` e `/export` davam visibilidade e exportação. O MLib tem saúde da biblioteca planeada, mas não tem exportação/auditoria equivalente.
3. **Latência da selecção** — o MCat documentou uma proposta para medir cada etapa e evitar cliques cegos. O MLib usa polling fixo; vale a pena instrumentar e adaptar as esperas.

## 2. Sumário executivo

O núcleo de aquisição já está implementado no MLib e as duas lacunas P0 antigas — conclusão da recepção e cardinalidade de artista — estão fechadas ao nível de código e testes automatizados no HEAD actual:

- `MusicAcquisitionRules.completionReason()` implementa contagem esperada, sinal textual, faixa única, silêncio de 9 s e deadlines de 45 s/240 s.
- `isMultiTrack()` usa a contagem esperada, não o tipo `artist` isoladamente.
- Os golden tests de conclusão e protocolo passam em `:desktop:test`.

**Importante:** a release pública `v0.1.9` foi construída antes destas correcções. A próxima release (`v0.1.10`) é a primeira que as inclui.

O que resta portar é sobretudo **produto e biblioteca**, não o protocolo do bot:

1. destino/configuração de fontes e indexação antes da deduplicação;
2. merge de metadata de faixas duplicadas;
3. fluxo de completar álbum com pré-visualização das faixas em falta;
4. onboarding/setup do bot e do arquivo pessoal;
5. enriquecimento de álbuns e gestão completa de capas;
6. capas de playlists;
7. observabilidade e persistência por opção nos jobs;
8. revisão explícita de duplicados;
9. validação live T01–T20.

Nenhum destes itens bloqueia a release Android `v0.1.10` nem o teste de actualização in-app. Bloqueiam apenas a declaração de **paridade funcional com o MCat Player**.

## 3. Estados usados

| Estado | Significado |
|---|---|
| **Portado** | Comportamento implementado e coberto por teste/golden no HEAD. |
| **Portado, falta live** | Código e testes presentes; falta prova com Telegram/dispositivo real. |
| **Parcial** | Existe implementação, mas falta comportamento relevante do MCat Player. |
| **Em falta** | Não existe equivalente funcional no MLib. |
| **Divergência deliberada** | MLib faz diferente por segurança/produto; não copiar sem decisão explícita. |
| **Não portar** | Arquitectura do MCat Player excluída por decisão. |

## 4. Matriz de estado actual

| ID | Capacidade MCat Player | Estado no MLib | Evidência MLib | Estado | Prioridade |
|---|---|---|---|---|---|
| ACQ-01 | Conclusão da recepção de áudio | Contagem, sinal textual, faixa única, silêncio 9 s, deadline 45/240 s | `MusicAcquisitionRules.kt`; `MusicAcquisition.kt:890-940`; testes `wave1GoldenCompletion…` | **Portado, falta live** | P0 live |
| ACQ-02 | Artista multi-faixa apenas com contagem >1 | `isMultiTrack()` usa `expectedCount` | `MusicAcquisitionRules.kt`; teste `artistCardinalityUsesExpectedCount…` | **Portado** | — |
| ACQ-03 | Fixtures de pesquisa/callbacks | 8 pares tipo/provider e parsing numerado | `mcat-protocol-v1.json`; testes golden | **Portado** | — |
| ACQ-04 | Paginação e colecções grandes | 100/página, 20 páginas, limite 2.000 mensagens | `MusicAcquisition.kt:1155-1168` | **Parcial** | P2 |
| ACQ-05 | Simulação/arm antes do envio real | Encaminha imediatamente após a selecção | `MusicAcquisition.kt` acquire/forward | **Em falta** | P1 |
| ACQ-06 | Medição/adaptação da latência de selecção | Polling fixo; sem métricas por etapa | `MusicAcquisition.kt` wait/click | **Parcial** | P2 |
| LIB-02 | Auditoria/exportação operacional | Saúde planeada; sem `/status`, `/duplicates`, `/export` equivalentes | `Library`/UI | **Parcial** | P2 |
| SRC-01 | Destino escolhido e fontes de dedup | Só procura “GinjMusic”; destino é adicionado às fontes **depois** do job; dedup usa todas as fontes da biblioteca | `AcquisitionSheet.kt:39-53`; `MusicAcquisition.kt:417,793` | **Parcial** | P1 |
| SET-01 | Setup do bot e arquivo pessoal | `/start` é enviado ao pesquisar; não há wizard nem criação do canal-arquivo | `MusicAcquisition.kt` (sendText `/start`); sem ecrã de setup | **Em falta** | P2 |
| FWD-01 | Encaminhamento e importação | `forwardMessages`, parser, upsert, refresh | `MusicAcquisition.kt:806-850` | **Portado, falta live** | P1 live |
| DED-01 | Igualdade de gravações | Port directo de `areTracksDuplicate` | `MusicAcquisitionRules.kt:130`; golden tests | **Portado** | — |
| DED-02 | Merge de metadata de faixas | `TrackDuplicates` só agrupa/apresenta; não escolhe keeper nem funde metadata | `TrackDuplicates.kt` | **Em falta** | P1 |
| DUP-01 | Revisão de duplicados | Apenas “esconder duplicados”; sem escolha de representante/merge | `TrackDuplicates.kt`; `SettingsSheet.kt` | **Parcial** | P2 |
| PL-01 | Limpeza de nomes de playlist | `cleanCollectionTitle` + golden PL | `MusicAcquisitionRules.kt`; `mcat-golden-v1.json` | **Portado** | — |
| PL-02 | Criar/actualizar playlist e ordem | CRUD, membros únicos, ordem, descrição/capa URL | `Library.kt:1045-1100`; `MusicAcquisition.kt:460-490` | **Portado, falta live** | P2 live |
| PL-03 | Gestor de capas de playlist | Só campo URL; fallback Unsplash genérico | `PlaylistScreens.kt:288`; `MusicAcquisition.kt:476` | **Parcial** | P2 |
| ALB-01 | Detecção/enriquecimento de álbuns | Agrupa tags locais; capas iTunes/Deezer; sem enriquecimento de metadata em falta | `SpotygramApp.kt:803-880` | **Parcial** | P2 |
| ALB-02 | Dedup/merge de álbuns | Regra exacta + confirmação; merge fuzzy do MCat não foi copiado | `Library.kt:856-925`; `AlbumScreens.kt:230` | **Divergência deliberada** | P2 |
| ALB-03 | Completar álbum | Pesquisa artista+álbum, `targetAlbumId`, reutiliza faixas; adiciona automaticamente, sem pré-visualização | `SpotygramUI.kt:850`; `MusicAcquisition.kt:429-458` | **Parcial** | P1 |
| COV-01 | Ferramentas de capa de álbum | URL + pesquisa iTunes/Deezer; sem upload local, capas por faixa, presets navegáveis ou modos Smart/Online | `AlbumScreens.kt:146-167`; `SpotygramApp.kt:803` | **Parcial** | P2 |
| JOB-01 | Estados por opção e progresso | Job em memória; pending em prefs com títulos; restart repete opções | `MusicAcquisition.kt:55-76,660-710` | **Parcial** | P2 |
| JOB-02 | Retoma/resiliência | Journal de retoma + dedup; sem transacção por faixa | `MusicAcquisition.kt` pending; sem prova live | **Parcial** | P2 |
| ONB-01 | Activação do bot | `/start` automático na pesquisa; sem confirmação de resposta | `MusicAcquisition.kt` | **Parcial** | P2 |
| PLAY-01 | Player/MediaSession Web | MLib usa Media3/TDLib nativo | `PlaybackService.kt`, `PlayerConnection.kt` | **Não portar** | — |
| WEB-01 | PWA, Express, localStorage, API MCat, proxy de áudio | Excluído por arquitectura | Decisões MLib | **Não portar** | — |
| LIVE-01 | Aceitação T01–T20 | Sem prova real Telegram/dispositivo | — | **Em falta** | P0 live |

## 5. Trabalho restante, por prioridade

### P1 — necessário para paridade útil

#### SRC-01 — Destino, fontes e indexação antes da dedup

**Problema actual**

- `AcquisitionSheet.loadDestinations()` procura apenas “GinjMusic” localmente e no servidor; não oferece um selector de qualquer chat com permissão de escrita.
- O destino só é adicionado às fontes depois de processar o job (`MusicAcquisition.kt:417`), mas a dedup compara com `library.state.value.tracks` antes disso (`MusicAcquisition.kt:793`).
- Uma primeira aquisição para um destino que já contém áudio mas ainda não é fonte pode voltar a encaminhar duplicados.
- A dedup usa todas as fontes da biblioteca, não um conjunto configurável de fontes de comparação.

**Trabalho**

1. Selector de destino com chats onde a conta pode publicar, incluindo canais e grupos, com pesquisa.
2. Persistir destino por conta Telegram.
3. Antes de seleccionar resultados, adicionar/indexar o destino e refrescar o histórico (com estado de progresso).
4. Definir fontes de dedup por perfil: destino + fontes explicitamente escolhidas; não usar automaticamente toda a biblioteca.
5. Nunca usar o canal `mlib-sync` como destino de música.
6. Testes: primeira aquisição num destino não indexado; retry; destino sem permissão; dois destinos.

**Critério de aceitação:** adquirir a mesma faixa duas vezes para o mesmo destino não cria duplicados, mesmo quando o destino não era fonte antes do primeiro job.

#### ACQ-05 — Pré-visualização/arm antes do encaminhamento

**Problema actual:** o MLib encaminha assim que a opção é processada. O MCat original separava a simulação (`/arm`) da autorização real (`/live`).

**Trabalho**

- Modo “simular” que corre o fluxo e mostra o que seria encaminhado, sem enviar.
- Confirmação explícita do destino e da lista final antes do primeiro envio.
- Mostrar faixas já existentes, faixas a adquirir e duplicados ignorados.
- Guardar a autorização por job, não globalmente.
- Testes de que simulação nunca chama `forwardMessages`.

**Critério de aceitação:** nenhuma mensagem é encaminhada sem uma confirmação explícita que mostre destino e contagem.

#### DED-02 — Merge de metadata de faixas

**Problema actual:** `TrackDuplicates` é apenas uma vista. Não existe equivalente ao `mergeTracks`/`deduplicateTracksList` do MCat Player.

**Trabalho**

- Escolher registo primário e fundir metadata sem apagar ficheiros silenciosamente.
- Preferir, por ordem documentada: FLAC/qualidade superior, maior bitrate/tamanho, duração válida, capa não genérica, artista/álbum úteis.
- Preservar favorito, playlists, álbuns, histórico e caminho local válido.
- Registar aliases/IDs antigos para não quebrar referências.
- UI de revisão: mostrar os dois registos, escolher keeper e pré-visualizar alterações.
- Golden tests cross-language para os casos de merge.

**Critério de aceitação:** dois registos da mesma gravação em formatos diferentes convergem para um registo com os melhores metadados, sem perder relações nem ficheiros.

#### ALB-03 — Completar álbum com pré-visualização

**Problema actual:** o botão de descarregar álbum inicia uma pesquisa `artista + álbum` com `targetAlbumId` e adiciona automaticamente as faixas recebidas. Não mostra a lista de faixas existentes/em falta nem pede aprovação por faixa.

**Trabalho**

1. Pesquisar o álbum alvo e listar opções do bot.
2. Mostrar pré-visualização: faixas já na biblioteca, faixas em falta, faixas ambíguas.
3. Permitir seleccionar/editar a edição e aprovar individualmente.
4. Reutilizar faixas existentes; encaminhar apenas as aprovadas.
5. Actualizar o álbum alvo, nunca criar um álbum fictício se o alvo desapareceu.
6. Testes: álbum completo, parcial, edição diferente, alvo apagado, retry.

**Critério de aceitação:** o utilizador vê exactamente o que falta e o que será adicionado antes de qualquer encaminhamento.

### P2 — paridade de produto

#### SET-01/ONB-01 — Setup do bot e arquivo pessoal

O MCat Player tinha um wizard que enviava `/start`, procurava um arquivo existente e, se necessário, criava um canal privado. O MLib assume que o destino `GinjMusic` já existe e só envia `/start` ao pesquisar.

Portar como setup explícito, não automático:

- verificar o bot e enviar `/start` com confirmação de resposta;
- procurar/criar um canal privado de arquivo;
- marcar o canal como fonte e destino;
- manter o canal de CloudSync separado;
- permitir reconfigurar sem apagar dados.

#### ALB-01 — Enriquecimento de álbuns

- Enriquecer metadados de faixas Telegram/locais com iTunes e fallback Deezer.
- Cache, timeouts e matching conservador.
- Não substituir tags válidas por placeholders.
- Cobrir tags genéricas, álbuns sem ano e capas em falta.

#### ALB-02 — Merge fuzzy de álbuns

O MLib tem agora uma regra exacta com confirmação, deliberadamente mais segura. Para paridade:

- usar `areAlbumsDuplicate` apenas para **sugerir** grupos;
- mostrar as edições, anos, capas e faixas antes de fundir;
- nunca fundir automaticamente edições distintas;
- manter operação reversível/undo;
- golden tests para “Greatest Hits” vs “Greatest Hits 2”, edições e anos.

Sem este cuidado, a regra fuzzy do MCat Player pode destruir identidade de álbuns.

#### PL-03/COV-01 — Gestão de capas

- Playlist: pesquisa online, capa da primeira faixa, upload local, presets e URL.
- Álbum: capa por faixa, modo Smart/Online, presets navegáveis, upload local e URL.
- Definir armazenamento multiplataforma e sincronização de capas personalizadas.
- Remover o fallback Unsplash genérico como capa “real”; usá-lo apenas como placeholder explícito.

#### JOB-01/JOB-02 — Estados e retoma por opção

- Persistir estado por opção/faixa, não apenas títulos seleccionados.
- Mostrar progresso por opção, sucessos, falhas e parciais.
- Retry apenas do que falta; não repetir opções concluídas.
- Tratar cancelamento local e trabalho que continua no bot.
- Reindexar o destino antes de retry.
- Testar crash/restart antes e depois de encaminhar.

#### ACQ-04 — Paginação e colecções grandes

- Testar mais de 100 mensagens e o limite de 2.000.
- Definir comportamento ao atingir o limite: resultado parcial explícito, nunca perda silenciosa.
- Confirmar ordem cronológica, sem duplicados, com mensagens editadas/substituídas.

#### ACQ-06 — Latência e cliques adaptativos

O MCat documentou uma proposta para medir envio, filtro, lista e seleção, e nunca repetir um clique cego. O MLib deve:

- instrumentar tempos por etapa sem guardar conteúdo sensível;
- avançar por evento/estado observado em vez de intervalos fixos;
- repetir no máximo uma vez após timeout confirmado;
- parar e pedir intervenção em estados ambíguos.

#### LIB-02 — Auditoria e exportação

Equivalente operacional a `/status`, `/duplicates`, `/hist-music-find` e `/export`:

- contagens por fonte, disponibilidade, offline e sync;
- lista de duplicados e possíveis correspondências;
- pesquisa por título/artista nos históricos configurados;
- exportação JSON/CSV/M3U com limites e sem expor segredos.

#### DUP-01 — Revisão de duplicados

- Trocar “esconder duplicados” por vista de revisão com escolha de representante.
- Integrar com DED-02 para fundir metadata.
- Mostrar ficheiros/formato/tamanho antes de remover duplicados locais.

### P3 — limpeza observada durante esta revisão

- `AcquisitionSheet.kt:355` ainda mostra `"Estado: …"` hardcoded em português; mover para recurso antes de considerar a UI EN/PT fechada.
- O fallback de capa Unsplash é usado como capa de playlist/álbum em falta; deve ser tratado como placeholder explícito, não como metadata real.

### P0 live — aceitação real

Executar T01–T20 do `PORTABILITY-GAPS-REPORT.md` numa conta Telegram real e dispositivo Android:

- pesquisa track/album/playlist/artist, Deezer/SoundCloud;
- conclusão 45/240 s, contagem, sinal e silêncio;
- >100 mensagens;
- destino escolhido e primeira aquisição sem duplicados;
- retry/crash/FloodWait;
- playlists/álbuns e capas;
- CloudSync em dois dispositivos;
- instalação e actualização in-app.

Um build verde não substitui estes testes.

## 6. Divergências deliberadas (não “portar”)

| Área | Decisão MLib |
|---|---|
| Dedup de álbuns | Regra exacta + confirmação; não copiar merge fuzzy destrutivo. |
| Arquitectura | TDLib/Kotlin/SQLite/Media3; sem GramJS, Express, localStorage, API MCat ou proxy de áudio. |
| Sessão | Sessão TDLib local já autenticada; sem segundo login/backend. |
| Player | Player nativo MLib; o player Web do MCat Player não é portado. |
| CloudSync | Protocolo MLib próprio; não sincronizar áudio/cache nem reutilizar o canal de sync como arquivo. |
| Catálogo | Sem playlists canónicas, demo ou blacklist específica do MCat Player sem decisão explícita. |
| Dependência | MCat e MCat Player são referências; não são dependências de runtime. |

## 7. Impacto na release Android

- **Nenhum item da secção 4 bloqueia `v0.1.10`.**
- `v0.1.10` deve servir para validar a actualização in-app e o fluxo de aquisição já existente.
- A primeira prioridade após a release é o **teste live de aquisição** (`ACQ-01`, `FWD-01`, `DED-01`), porque é o maior risco não comprovado.
- Só depois faz sentido investir em SRC-01/DED-02/ALB-03 e no resto da paridade.

## 8. Ordem recomendada

1. Publicar e instalar `v0.1.10`; testar actualização a partir de `v0.1.9`.
2. Teste live de uma faixa, uma playlist pequena e uma aquisição parcial.
3. SRC-01: destino/indexação antes de dedup.
4. DED-02 + DUP-01: merge e revisão de duplicados.
5. ALB-03: pré-visualização de álbum em falta.
6. SET-01/ONB-01: setup do bot/arquivo.
7. ALB-01, COV-01, PL-03: enriquecimento e capas.
8. JOB-01/02, ACQ-04: jobs e colecções grandes.
9. T01–T20 e declaração de paridade.

## 9. Procedimento para reauditar o MCat Player

Quando o repositório voltar a estar acessível:

1. Registar o commit exacto de `main`.
2. Confirmar os ficheiros de referência: `server.ts` (`processMcatJob`, parsing, dedup, merge, playlists), `src/utils/trackSanitizer.ts`, `src/utils/trackDeduplication.ts`, `src/services/mcatApi.ts`, `SearchView.tsx`, `LibraryView` e `EditCoverModal`.
3. Comparar função a função com `MusicAcquisitionRules`, `MusicAcquisition`, `Library`, `TrackDuplicates`, `AlbumScreens` e `PlaylistScreens`.
4. Actualizar fixtures/golden cases e executar `:desktop:test`.
5. Repetir a matriz desta página e só então declarar paridade.

Se o repositório tiver sido apagado, usar o commit `a41cf5c` como referência histórica e registar essa limitação em qualquer decisão de produto.

## 10. Definição de portabilidade concluída

A portabilidade considera-se concluída quando:

- P0/P1 estiverem fechados ou formalmente excluídos;
- existirem golden tests cross-language para pesquisa, conclusão, dedup e merge;
- SRC-01/DED-02/ALB-03 estiverem implementados sem perda silenciosa;
- capas/playlists/álbuns tiverem persistência e CloudSync testados;
- T01–T20 tiverem evidência real;
- Android arm64/x86_64 e actualização in-app estiverem validados;
- este documento e o `PORTABILITY-GAPS-REPORT.md` apontarem para o mesmo estado.

## 11. Referências internas

- `docs/mlib/PORTABILITY-GAPS-REPORT.md`
- `docs/mlib/PORTABILITY-CONTRACT.md`
- `docs/mlib/MCAT-INTEGRATION.md`
- `docs/mlib/PORTABILITY-WAVE-0.md`
- `docs/mlib/PORTABILITY-WAVE-1.md`
- `docs/mlib/SOURCE-FEATURE-MATRIX.md`
- `docs/mlib/INDEPENDENT-AUDIT-REMEDIATION.md`
