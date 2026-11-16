# Wave 0 — execução e evidência
**Estado:** implementada no commit a criar a partir do baseline `7231aa08022863a5a3f22138ff881fcf4045c340`; testes/CI sujeitos a execução no novo HEAD.  
**Data:** 16-11-2026.

## Baseline congelada
- MLib branch: `feat/mlib-independent-identity`
- MLib HEAD: `7231aa08022863a5a3f22138ff881fcf4045c340`
- MCat Player main: `a41cf5ceeea023d7b86484cf5c7bf5a7ae7b2132`
- SQLite: `Library.kt` abre `library.db` com versão **11** (a referência 8 em `PERSISTENCE-AND-SYNC.md` estava errada).
- Estado de prova separado em: código presente / paridade de fonte / teste automatizado / teste live aprovado.

## Entregáveis desta wave
1. `PORTABILITY-CONTRACT.md`: comportamento funcional e exclusões de arquitectura.
2. `fixtures/mcat-protocol-v1.json`: 8 combinações tipo/provider, callbacks opacos, áudio e sinais terminais sintéticos.
3. `fixtures/mcat-golden-v1.json`: casos esperados para sanitização, nomes de playlist, comparação track/album e semântica de merge.
4. `PORTABILITY-GAPS-REPORT.md`: registo versionado das lacunas e waves.
5. Matriz MCat → MLib → estado → teste → wave e correcções de estado nos documentos existentes.
6. Testes JVM adicionais que carregam e executam directamente os golden JSON; fixtures de protocolo também são carregadas e verificam as oito combinações tipo/provider e o parsing dos resultados.

## Rastreabilidade funcional

| Função/rota MCat | MLib | Estado de código no baseline | Teste/fixture | Wave |
|---|---|---|---|---:|
| `getMcatBotEntity` | `MusicAcquisition.resolveBot` | Adaptado TDLib | PROTO search | 0/1 |
| `parseBotSearchResults` | `MusicAcquisitionRules.parseResults` | Adaptado | testes parser + PROTO | 0/1 |
| `clickButtonByKeyword` / opção numerada | `clickButton` / `clickNumberButton` | Adaptado com payload TDLib exacto | PROTO callbacks | 0/1 |
| `processMcatJob` / recepção áudio | `acquireOption` / `collectAudioMessages` | Parcial: exige apenas Finished. | completion/cardinality fixtures | 1 |
| `sanitizeSearchQuery` / `sanitizeTrackTitle` | `MusicAcquisitionRules` | Portado; comparar golden | GOLD Q-01..04 | 0 |
| `cleanPlaylistName` | `cleanCollectionTitle` | Equivalente em código | GOLD PL-01..03 | 0/3 |
| `areTracksDuplicate` | `areTracksDuplicate` | Port directo | GOLD TR-01..05 | 0/2 |
| `mergeTracks` / `deduplicateTracksList` | `TrackDuplicates` / Library | Parcial; merge não equivalente | GOLD MERGE-TRACK-01 | 2 |
| `cleanAlbumTitle` / `areAlbumsDuplicate` | `MusicAcquisitionRules` | Portado | GOLD AL-01..04 | 0 |
| `mergeAlbums` / `deduplicateAlbumsList` | `Library.deduplicateAlbums` | Diferente | GOLD MERGE-ALBUM-01 | 4 |
| `getGenreCoverPreset` | `genreCoverPreset` | Portado | extensão golden futura | 0/5 |
| `/api/playlists/*` | `Library.*Playlist` | Excluída a rota; CRUD nativo presente | T10/T11 | 3 |
| `/api/albums/*` | `Library.*Album` | Excluída a rota; CRUD nativo presente | T12–T14 | 4 |
| `/api/tracks/deduplicate` | `TrackDuplicates` / Library | Parcial; sem merge completo | T08/T09 | 2 |
| `mcatApi.getChats/selectChat`, `sourceChatIds`, `dedupSources` | `SpotygramApp.searchChats` / `AcquisitionSheet` | Parcial | SRC-01 | 2 |
| GramJS auth / Express / proxy áudio | TDLib / SQLite / Media3 | Excluído/substituído por decisão | arquitectura | — |

## Decisões
- Fixtures de protocolo são **sintéticas** e não alegam ser logs reais. Os callbacks usam tokens opacos fictícios e não são executados contra Telegram.
- Golden cases de comparação são derivados das funções do MCat Player no commit de referência indicado. Merge é documentado como oráculo futuro; não se declara portado.
- Não se alterou o código do MCat Player nem o MCat de produção.
- Não se alterou lógica ACQ-01/02 nesta wave; isso pertence à Wave 1.
- A Wave 0 não declara portabilidade concluída nem teste live aprovado.

## Gate
O gate documental e de baseline fecha quando:
- contrato e fixtures estão versionados;
- golden tests executam contra o Kotlin actual;
- matriz aponta para ficheiro/teste e wave;
- documentos corrigem o schema e distinguem código/paridade/live;
- CI do commit Wave 0 é consultada e registada.

A aprovação live continua pendente e não é condição para declarar apenas a Wave 0 documental fechada.
