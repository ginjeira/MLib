# MLib — roadmap de implementação por waves

**Versão:** 1.0  
**Data:** 20-11-2026  
**Âmbito:** Android primeiro. Desktop permanece congelado.  
**Documentos de referência:** `docs/mlib/PRODUCT-UX-PROPOSAL.md`, `docs/mlib/MCAT-PLAYER-PORT-STATUS.md`, `docs/mlib/PORTABILITY-GAPS-REPORT.md`, `docs/mlib/PORTABILITY-CONTRACT.md`.

Este é o plano de execução corrente. O histórico detalhado permanece em `docs/mlib/ROADMAP.md`.

## 0. Estado da versão 0.1.10

Verificado em **20-11-2026, 22:41 UTC**.

| Item | Estado |
|---|---|
| Última release pública | **v0.1.9** (publicada em 20-11-2026 20:48:53) |
| Tag `v0.1.10` | **Não existe** |
| Release `v0.1.10` | **Não existe** |
| Run de baseline para o commit local | **Não executado** |
| `origin/main` | `d7c68d2` (`chore(release): bump version to 0.1.9`) |
| `main` local | `993eafb` — **1 commit à frente**, por publicar |
| Alterações de documentação | **Não commitadas** (`roadmap.md`, propostas, revisões) |
| Bloqueio actual | **Autenticação GitHub** no `git push` |

**Conclusão:** a `v0.1.10` ainda não começou no GitHub. O código local está pronto para publicar, mas o push falhou por credenciais inválidas; não há baseline, tag, build nem release.

Sequência para desbloquear:

1. Autenticar o Git Credential Manager com um PAT válido com permissões de repositório e workflow.
2. Commit das alterações de documentação pendentes.
3. `git -c http.sslVerify=false push origin main`.
4. Baseline `MLib baseline validation` verde (inclui Android debug compile).
5. O workflow automático cria a versão **0.1.10** (patch), tag e release.
6. Instalar a release **v0.1.9** num dispositivo e actualizar in-app para **v0.1.10**.

A `v0.1.10` inclui as correcções do commit `993eafb`; a `v0.1.9` não as inclui.

## 1. Regras do roadmap

- Cada wave termina com **código, testes, documentação e evidência de release**.
- Nenhuma wave declara concluída uma feature sem teste automatizado e, quando aplicável, teste live.
- Alterações destrutivas exigem pré-visualização, confirmação e undo.
- Não se misturam refactors amplos com correcções de release.
- Android é a prioridade; desktop continua congelado e fail-closed.
- A publicação automática de releases de avaliação mantém-se.
- Cada release tem notas com: objectivo, alterações, limitações, testes e riscos.

## 2. Waves

### Wave 0 — Publicar 0.1.10 e validar actualização

**Objectivo:** provar o ciclo release → download → instalação in-app.

**Dependências:** autenticação GitHub, secrets de assinatura e Telegram já existentes.

**Tarefas**

- [ ] Commit das docs pendentes.
- [ ] Push de `main`; baseline verde.
- [ ] Release `v0.1.10` pública, non-draft e non-prerelease.
- [ ] Confirmar APKs arm64/x86_64, `SHA256SUMS.txt` e fingerprint do assinante.
- [ ] Instalar `v0.1.9` release; actualizar in-app para `v0.1.10`.
- [ ] Corrigir o texto hardcoded `"Estado: …"` em `AcquisitionSheet.kt:355`.
- [ ] Substituir screenshots antigas que ainda mostram Spotygram/russo.
- [ ] Smoke test live: uma faixa, uma playlist pequena, um cancelamento/retry.

**Aceitação**

- A app instalada em `v0.1.9` detecta, descarrega e instala `v0.1.10` sem perder dados.
- A versão instalada fica `0.1.10` e os dados da biblioteca permanecem.
- Nenhum crash/ANR no fluxo de actualização.
- Relatório de execução anexado à release.

**Risco principal:** autenticação GitHub e credenciais de assinatura.

---

### Wave 1 — Aquisição fiável: destino, fontes e colecções grandes

**Objectivo:** garantir que adquirir duas vezes não cria duplicados e que o destino é explícito.

**Itens:** SRC-01, ACQ-04, parte de JOB-01.

**Tarefas**

- [ ] Selector de qualquer chat/canal com permissão de publicação.
- [ ] Persistir destino por conta Telegram.
- [ ] Indexar/refrescar o destino **antes** de comparar duplicados.
- [ ] Fontes de dedup configuráveis (destino + fontes escolhidas).
- [ ] Estimativa antes de adquirir: faixas, duração aproximada, nº de pedidos.
- [ ] Fluxo de aquisição em passos com estado visível.
- [ ] Testes >100 mensagens e limite de 2.000; comportamento parcial explícito.
- [ ] Nunca usar o canal CloudSync como destino musical.

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
- [ ] Dashboard “Biblioteca saudável”: em falta, duplicados, ficheiros inválidos, capas, sync.
- [ ] Estúdio de duplicados: comparar, escolher keeper, fundir metadata, undo.
- [ ] Preservar favoritos, playlists, álbuns e caminho local válido.
- [ ] Sugestões de merge de álbuns com pré-visualização; nunca automático.
- [ ] Golden tests de merge track/album.
- [ ] Exportar relatório da biblioteca.

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

- [ ] Página de álbum com capa grande, gradiente e anel de completude.
- [ ] “Completar álbum” com lista de faixas existentes, em falta e ambíguas.
- [ ] Aprovação por faixa/edição antes de adquirir.
- [ ] Enriquecimento iTunes → Deezer com cache e timeouts.
- [ ] Capas: upload local, online, URL, primeira faixa e presets.
- [ ] Capas de playlist: upload, online, primeira faixa e presets.
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

- [ ] Setup opcional: verificar bot, enviar `/start`, confirmar resposta.
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

## 3. Backlog completo por wave

| ID | Item | Wave | Prioridade | Depende de |
|---|---|---|---|---|
| REL-01 | Publicar 0.1.10 e testar update in-app | 0 | P0 | auth GitHub |
| UI-00 | Corrigir “Estado:” hardcoded e screenshots | 0 | P0 | — |
| LIVE-01 | Aceitação live T01–T20 | 0/6 | P0 | 0.1.10 |
| SRC-01 | Destino, fontes e indexação antes da dedup | 1 | P1 | — |
| ACQ-04 | Paginação >100 e limite 2.000 | 1 | P1 | — |
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
4. confirmar `v0.1.10`;
5. executar Wave 0 e só depois iniciar Wave 1.
