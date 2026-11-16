# MLib — Relatório de lacunas de portabilidade
## MCat Player → MLib

**Versão:** 1.0 — baseline de 16-11-2026  
**Repositório:** `ginjeira/MLib`  
**Branch auditada:** `feat/mlib-independent-identity`  
**HEAD de baseline:** `7231aa08022863a5a3f22138ff881fcf4045c340`  
**Referência MCat Player:** `ginjeira/MCat-Player` / `main` / `a41cf5ceeea023d7b86484cf5c7bf5a7ae7b2132`  
**Schema SQLite no HEAD de baseline:** 11  
**Estado:** portabilidade parcial; não aprovada como concluída.

Este documento é a versão Markdown de referência do relatório de auditoria disponibilizado no Project MLib. A auditoria e o plano abaixo preservam o seu âmbito, decisões, prioridades e critérios. O relatório DOCX original permanece como artefacto de origem do Project; este ficheiro torna o conteúdo de orientação pesquisável e versionável no repositório.

## 1. Conclusão executiva

A portabilidade ainda não está concluída. O MLib contém uma implementação nativa relevante do fluxo de aquisição pelo bot Telegram, mas existem diferenças funcionais concretas e capacidades por provar. A existência de classes ou ecrãs equivalentes não demonstra paridade.

A auditoria abrange o motor Telegram, pesquisa, jobs, deduplicação, playlists, álbuns, detecção automática de álbuns, capas e integração com a biblioteca. Não pretende copiar a aplicação Web/PWA nem o servidor MCat Player.

Prioridade: corrigir a semântica de conclusão da aquisição e a classificação multi-faixa; depois fechar fontes de deduplicação e descoberta/merge de álbuns. `cleanCollectionTitle`, `cleanAlbumTitle`, `areTracksDuplicate`, `areAlbumsDuplicate` e `genreCoverPreset` já existem como ports directos/equivalentes e não devem ser reescritos sem golden tests que demonstrem diferença.

## 2. Âmbito e regras vinculativas

### Portar
- Comportamento observável de pesquisa, filtros, selecção, recepção de resultados, encaminhamento e actualização da biblioteca.
- Limpeza/normalização, deduplicação, merge de metadata e manutenção de playlists/álbuns.
- Jobs: estados por opção, resultados parciais, falhas e recuperação.
- Ferramentas de organização de álbuns e capas que pertencem à biblioteca.
- Integração com biblioteca local e CloudSync MLib.

### Não copiar
- GramJS/TelegramClient, StringSession, autenticação MTProto do servidor MCat ou endpoints `/api/telegram/auth/*`; MLib usa a sessão TDLib existente.
- Express e rotas HTTP `/api/mcat/*`, `/api/albums/*`, `/api/playlists/*`, `/api/tracks/*`; traduzir para Kotlin/TDLib/SQLite.
- `localStorage`, `mcatApi` e armazenamento/configuração Cloud MCat; MLib usa SQLite e CloudSync MLib.
- Proxy HTTP de áudio MCat; reprodução MLib continua em TDLib/Media3.
- Tokens, credenciais, API hashes, sessões e estado operacional do servidor MCat.
- Playlists canónicas/demo e blacklist específicas do catálogo sem decisão explícita.

Regra: portar semântica e resultados, adaptando o mecanismo à arquitectura MLib. Diferenças deliberadas são decisões documentadas, nunca “portabilidade exacta”.

## 3. Inventário resumido

| Área | Estado observado no MLib | Conclusão |
|---|---|---|
| Pesquisa, filtros e resultados | Implementado em código | Paridade estrutural provável; faltam fixtures e validação live |
| Recepção/conclusão de áudio | Implementado com diferença | MLib só reconhece exactamente `Finished.`; MCat usa contagem, vários sinais, faixa única e silêncio |
| Artista | Implementado com diferença | MLib trata sempre ARTIST como multi; MCat depende da cardinalidade esperada |
| Encaminhamento/parsing | Implementado | TDLib presente; falta validação end-to-end |
| Dedup durante aquisição | Helper portado | Falta corpus golden e fontes adicionais |
| Merge de faixas | Parcial | MLib não reproduz integralmente merge de metadata MCat |
| Playlists | CRUD, membros, descrição/capa | Faltam golden tests e gestor de capas |
| Álbuns | CRUD, membership, metadata | Detecção/enriquecimento e merge diferem |
| Capas | Parcial | Pesquisa simples; faltam modos Smart/Online, capas de faixas, presets navegáveis e upload |
| Jobs/retoma | Próximo no essencial | Testar estados por opção e journal; journal SQLite completo não é requisito de paridade |
| Destinos/fontes dedup | Parcial | Descoberta orientada a GinjMusic; falta equivalência de configuração |
| Aceitação real | Pendente | Sem prova de fluxo live completo |

## 4. Registo de lacunas

| ID | Prioridade | Estado observado | Wave |
|---|---:|---|---:|
| ACQ-01 — Critérios de conclusão da recepção de áudio | P0 | Implementação Wave 1 submetida a CI; gate pendente | 1 |
| ACQ-02 — Classificação multi-faixa para artista | P0 | Implementação Wave 1 submetida a CI; gate pendente | 1 |
| ACQ-03 — Fixtures de protocolo para pesquisa/callbacks | P1 | Implementado, não provado | 0/1 |
| ACQ-04 — Ordem, paginação e limite de recolha | P2 | Adaptação mais ampla; limite por validar | 1 |
| SRC-01 — Destino Telegram e fontes de deduplicação | P1 | Parcial | 2 |
| DED-01 — Regras de duplicados de faixas | P1 | Port directo; golden tests em falta | 0/2 |
| DED-02 — Merge de metadata de faixas | P1 | Em falta/modelo diferente | 2 |
| PL-01 — Limpeza de nomes de playlist | P2 | Equivalente em código; validar | 0/3 |
| PL-02 — Criação/actualização e ordenação de playlists | P1 | Implementado, validar | 3 |
| PL-03 — Gestor de capas de playlist | P2 | Em falta/UI parcial | 3/5 |
| ALB-01 — Detecção/enriquecimento de álbuns | P1 | Parcial | 4 |
| ALB-02 — Deduplicação/merge de álbuns | P1 | Diferença funcional | 4 |
| ALB-03 — Fluxo dedicado “completar álbum” | P1 | Parcial/UX diferente | 4 |
| COV-01 — Ferramentas de capas de álbum | P2 | Parcial | 5 |
| JOB-01 — Estados por opção/encaminhamento parcial | P2 | Próximo do MCat; testar | 6 |
| JOB-02 — Resiliência do journal de retoma | P2 | Melhoria MLib, não lacuna de paridade | 6 |
| DOC-01 — Consistência documental | P0 | Documentação desactualizada | 0 |

## 5. O que já existe — não recomeçar sem evidência

- Sessão Telegram TDLib existente, sem login/sessão paralela.
- Resolução de `@MusicsHuntersbot`, `/start`, pesquisa e callbacks inline com payload devolvido pelo TDLib.
- Tipos track/album/playlist/artist, fornecedores Deezer/SoundCloud, parsing numerado, selecção simples/múltipla e select-all.
- Destino explícito, encaminhamento individual por `forwardMessages` e leitura de title/performer/duration dos atributos de áudio.
- Mutex para serialização do bot e expiração local da pesquisa em 20 minutos.
- Criação/actualização de playlists, SQLite, migração e sincronização de metadata.
- Modelo de álbum separado, CRUD, membership/ordem, metadata, capa, remoção e deduplicação local.
- Regras de normalização/comparação em `MusicAcquisitionRules` e testes JVM iniciais.
- Journal básico de retoma e protecção contra encaminhamento silencioso para chat arbitrário.

Estas áreas necessitam de testes e ajustes identificados, mas não justificam reescrita de raiz.

## 6. Waves de implementação

### Wave 0 — Congelar contrato funcional e baseline
**Objectivo:** transformar o código MCat actual num contrato verificável e remover contradições documentais.  
**Trabalho:** fixtures anonimizadas de pesquisa/resposta/botões/áudio/conclusão para track, album, playlist e artist, Deezer e SoundCloud; golden cases de limpeza/comparação/merge (track, album, playlist); mapa função/rota MCat → Kotlin/adaptação/exclusão; actualizar documentos para HEAD `7231aa0`, schema 11 e separar código presente, paridade de fonte e teste live aprovado.  
**Entregáveis:** contrato versionado, fixtures, matriz de rastreabilidade e documentação coerente.  
**Gate:** uma pessoa externa identifica comportamento esperado e ficheiro/teste MLib correspondente para cada capacidade.

### Wave 1 — Protocolo de pesquisa e recepção
Corrigir ACQ-01 (contagem fiável, sinais finais MCat, faixa única, silêncio, deadlines 45s/240s, ausência de áudio e resultados parciais); ACQ-02 (artist só multi quando a opção/contagem exigir); fechar callbacks, mensagens editadas/novas, polling, cursor, paginação, ordem e limite; logs por opção sem dados sensíveis. Gate: track/album/playlist/artist terminam/falham correctamente sem depender de texto que o bot não enviou.

### Wave 2 — Destinos e deduplicação
Implementar SRC-01; golden cross-language DED-01 e identidade Telegram; DED-02 com merge controlado preservando formato, bitrate, duração, tamanho, capas, artista, álbum, favoritos e relações; definir duplicados na mesma resposta, biblioteca, destino e fontes. Gate: pedidos repetidos não criam duplicados; melhores metadados preservados; nada é removido silenciosamente.

### Wave 3 — Playlists
Golden PL-01 sem duplicar helper; manter blacklist/canonical fora salvo decisão; validar PL-02 em ordem, opções múltiplas, nova/existente, duplicados, falhas parciais e retry; PL-03 com capas por faixa, online, género e custom/upload compatível com CloudSync; testar migração e sync bi-direccional. Gate: nome/fallback, faixas/ordem e metadata persistem/convergem sem duplicados.

### Wave 4 — Álbuns
ALB-01: enriquecimento iTunes→Deezer para faixas Telegram e locais, tags genéricas, cache/timeouts/matching; ALB-02: golden cleanAlbumTitle/areAlbumsDuplicate/mergeAlbums/deduplicateAlbumsList e integração transaccional Library/CloudSync; ALB-03: completar álbum com query artista+álbum, fallback e edição. Gate: não criar álbuns falsos nem fundir edições incompatíveis; actualizar o álbum alvo.

### Wave 5 — Capas
Completar COV-01 e playlists: online iTunes/Deezer, capas das faixas, sugestões rotuladas, presets, URL e ficheiro local; definir formato/tamanho/armazenamento multiplataforma; capa personalizada prevalece sobre placeholder. Gate: métodos definidos persistem após reinício/upgrade/sync.

### Wave 6 — Jobs e observabilidade
Validar JOB-01 sem alterar semântica sem testes; JOB-02 com reindexação do destino antes de retry; testar restart, rede, FloodWait, sessão, destino e expiração; UI de progresso/resultado parcial. Journal SQLite completo é melhoria opcional, não requisito MCat. Gate: retoma não reencaminha mensagens indexadas.

### Wave 7 — Aceitação integrada
Executar T01–T20 numa conta Telegram real; builds Android ARM64/x86_64 e Desktop no HEAD final; migração/upgrade; sync dois dispositivos e reprodução; actualizar evidências e decisão final. Gate: todos os casos obrigatórios passam ou exclusões aprovadas ficam registadas.

## 7. Matriz obrigatória de aceitação

| ID | Teste | Resultado exigido |
|---|---|---|
| T01 | Track / Deezer e SoundCloud | Filtros aplicados; resultados actuais; expiração 20 min |
| T02 | Album/playlist/artist | Tipo correcto; sem resultados obsoletos; artist pode ser uma ou várias faixas |
| T03 | Callbacks/teclado | Payload TDLib exacto; mensagem editada/nova; sem selecção errada |
| T04 | Conclusão faixa | Primeira faixa termina pedido simples; sem áudio falha claramente; sinais MCat |
| T05 | Conclusão colecção | Contagem/sinal/silêncio; parcial reportado e recuperável |
| T06 | Mais de 100 mensagens | Paginação sem perda/repetição/troca; limite explícito |
| T07 | Encaminhamento | Mensagens individuais, metadata correcta, falhas isoladas |
| T08 | Deduplicação | Repetição não cria duplicados; fontes consultadas; artistas distintos não fundidos |
| T09 | Merge faixas | Melhores metadata; relações/favoritos/ficheiros preservados |
| T10 | Playlist nova/existente | Nome, ordem, sem membros repetidos, descrição/capa persistem |
| T11 | Playlist parcial/retry | Sucessos mantidos; retry só obtém o que falta |
| T12 | Detecção álbuns | Telegram/locais, iTunes/Deezer, tags genéricas/cache/sem match |
| T13 | Dedup álbuns | Título/artista/edição/ano/capa/faixas; sem fundir edições indevidas |
| T14 | Completar álbum | Artista+álbum, escolha edição, fallback SoundCloud, actualiza alvo |
| T15 | Capas | Capas de faixa/pesquisa/presets/URL/upload persistem |
| T16 | FloodWait/rede | Retry limitado; espera longa mantém pendente |
| T17 | Crash/restart | Recuperação idempotente antes/depois de encaminhar/actualizar |
| T18 | SQLite upgrade | Migração sem perda de dados ou journal |
| T19 | CloudSync dois dispositivos | Metadata/membros/ordem convergem; áudio/cache/sessão não sincronizam |
| T20 | Build/reprodução | Android ARM64/x86_64 e Desktop; faixas visíveis e reproduzíveis |

## 8. Governação e riscos

- Não iniciar wave sem estado da anterior e commit exacto.
- Cada alteração referencia ID de lacuna no commit/teste.
- Função com nome semelhante não prova paridade.
- Android só validado com ambos APKs e verificação ABI concluídos.
- Não pedir testes de utilizador antes dos testes automatizados/estáticos relevantes.
- Não alterar o MCat de produção para acomodar MLib.
- Alterações do bot exigem fixtures/transcripts; metadata ambígua exige revisão; CloudSync exige testes de dois dispositivos; FloodWait/retry exige checkpoint e refresh do destino; capas locais exigem armazenamento multiplataforma.
- Portabilidade só concluída com P0/P1 fechados ou exclusões aprovadas, golden TS/Kotlin, migrações/CloudSync, T01–T20, CI e documentação consistentes.

## 9. Fontes e baseline

- MLib baseline: `7231aa08022863a5a3f22138ff881fcf4045c340`, branch `feat/mlib-independent-identity`.
- MCat Player baseline: `a41cf5ceeea023d7b86484cf5c7bf5a7ae7b2132`, branch `main`.
- MCat Player: `server.ts` (processMcatJob, parsing, dedup, album merge, playlists), `src/utils/trackSanitizer.ts`, `src/utils/trackDeduplication.ts`, `src/services/mcatApi.ts`, SearchView, LibraryView e EditCoverModal.
- MLib: `MusicAcquisition.kt`, `MusicAcquisitionRules.kt`, `AcquisitionSheet.kt`, `Library.kt`, `Models.kt`, `LibrarySync.kt`, `SpotygramApp.kt`, `AlbumScreens.kt`, `PlaylistScreens.kt` e testes Desktop.
- No HEAD baseline, `Library.kt` abre `library.db` com schema version **11**. A documentação anterior que referia schema 8 estava desactualizada.
- A CI run `43858862348` referida na auditoria é uma fotografia histórica e deve ser consultada novamente; não prova builds actuais nem aceitação live.

## 10. Definição de portabilidade concluída

A portabilidade só pode ser declarada concluída quando as lacunas P0/P1 estiverem fechadas ou formalmente excluídas, playlists/álbuns/capas estiverem integrados no modelo MLib, golden/unit/migration/CloudSync passarem, T01–T20 tiverem evidência, CI confirmar Android ARM64/x86_64 e Desktop e os documentos apontarem para o mesmo estado. Teste live e build são provas distintas.
