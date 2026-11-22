# MLib Server — proposta de implementação (FR-11)

Actualizada a **22-11-2026**. Versão 3: servidor **sem downloads persistentes**, tudo online.

## 0. Decisões fixadas

- Um CT Proxmox, um processo, **BD partilhada** com `profile_id`.
- Contas criadas apenas pelo administrador; sem registo público.
- **Sem downloads/offline no servidor** e **sem transcode**. O áudio é servido à medida do pedido.
- Publicação por Cloudflare Tunnel em `mlib.ginjeira.com`.
- Um perfil por adulto, **conta Telegram própria**, biblioteca e consumo de bot isolados.
- iOS por PWA (Safari + ecrã inicial).

## 1. Reutilização (evidência)

| Peça | Onde | Reutilização |
|---|---|---|
| `Platform` (ficheiros, prefs, DB, TDLib) | `Platform.kt` | Implementar `ServerPlatform` |
| TDLib por JSON, multi-cliente | `DesktopNative.kt`, `Telegram.kt` | Um cliente por perfil |
| `SqlDatabase` | `Platform.kt` | JDBC no servidor |
| `Library` sem Android | `Library.kt` | Directo, com `profile_id` |
| Login Telegram (código/2FA/QR) | `Telegram.kt` | Expor por API |
| Aquisição, revisão, jobs, `/cancel` | `MusicAcquisition.kt` | Directo |
| Conhecimento, completude, artistas | `Knowledge.kt`, `AlbumCompletion.kt`, `ArtistCatalog.kt` | Directo |

Não se reutiliza: UI Compose, serviços Android, notificações, playback service.

## 2. Trabalho novo (reduzido)

1. **`ServerPlatform`** JVM: `filesDir` por perfil, prefs em ficheiro, `openDatabase` JDBC, `tdlib` via `DesktopTdClient`.
2. **Streaming on-demand com cache temporário em disco**: na primeira reprodução, o ficheiro é obtido do TDLib para um **cache temporário em disco**, partilhado entre perfis, e servido com `Range`. Não é download offline nem biblioteca: tem teto, TTL e é limpo no arranque. Decisão fechada: **sem tmpfs** e **sem cache persistente**.
   - **Single-flight** por `remote.unique_id`; refcount de leitores; TTL de inatividade (10 min); teto configurável (ex. 5 GB).
   - **Prefetch controlado**: o player informa a fila e o servidor antecipa **1–2 faixas seguintes**, canceláveis, subordinadas ao espaço livre e serializadas por conta.
3. **Multi-perfil num processo**: instanciar `AppText`/`Knowledge`/notificações por app ou remover o estado global; `profile_id` em playlists, sources, downloads, settings e jobs (tracks já tem `profile_user`/`profile_position`).
4. **API HTTP** (Ktor): auth, admin, biblioteca, pesquisa, playlists, jobs (SSE), stream, definições.
5. **SPA web**: Entrar, Início, Biblioteca, Procurar, Playlist, Player, Jobs, Administração. Álbum/Artista numa segunda passagem.
6. **Operação**: JAR/systemd (ou Docker), `cloudflared`, backups do SQLite, `/healthz`.

## 3. Modelo de dados

Uma BD SQLite partilhada. `profile_id` em:

- `sources`, `playlists`, `playlist_tracks` (via playlist), `downloads`, `settings`, `jobs`.
- `tracks`: confirmar `profile_user`/`profile_position`; criar `likes(profile_id, track_id)`.
- Globais: `profiles`, `sessions`, `accounts`.

Sem tabela de media, sem refcount, sem pasta de biblioteca no servidor.
Ficheiros importados localmente ficam fora do MVP web.

## 4. Autenticação e administração

- Contas criadas pelo admin; Argon2id; sem auto-registo.
- Sessão: token 256-bit com hash na BD; cookie `HttpOnly; Secure; SameSite=Lax`, 30 dias, rotação.
- CSRF em escritas; rate limit; log de tentativas; papéis `admin`/`listener`.
- `/admin` também atrás de Cloudflare Access (email OTP).
- Estado TDLib cifrado no servidor; nunca exposto ao browser.

## 5. Streaming e prefetch

- `GET /api/stream/{trackId}`: valida sessão → track do perfil → ficheiro do cache (ou obtém do TDLib) → `206` com `Range`.
- Cache temporário em disco: chave `remote.unique_id`, single-flight, refcount de leitores, TTL de 10 min após o último leitor, teto configurável (5 GB por defeito), limpeza no arranque.
- `POST /api/player/queue { currentTrackId, nextTrackIds }`: agenda prefetch das 1–2 faixas seguintes. Cancelar quando a fila muda; nunca prefetch de playlists inteiras.
- Prefetch só com espaço livre e prioridade abaixo da faixa em reprodução; uma transferência por perfil.
- Sem autorização por caminho; nunca aceitar `chatId/messageId` do cliente; o cache não concede acesso.
- Sem transcode: MP3/AAC/FLAC como estão.
- MVP: esperar pelo ficheiro completo antes de responder ao primeiro `Range`; progressivo só se a medição o justificar.

## 6. UI web (MVP)

Entrar · Início · Biblioteca (Faixas/Playlists) · Procurar · Playlist · Player fixo · Jobs · Administração.
Álbuns/Artistas e organização visual ficam para a passagem seguinte.
PWA com manifest e `standalone`; Media Session; skeletons e erros com retry.

## 7. Operação

- CT: 2 vCPU, 2 GB RAM, 8–12 GB de disco.
- `cloudflared` no CT, TLS na Cloudflare, sem portas abertas.
- Backup: `sqlite3 .backup` diário + snapshot do CT.
- Cache temporário limitado (ex. 5 GB) e limpo no arranque.

## 8. Previsão

Dias de trabalho focado, com reutilização.

| Fase | Entrega | Dias |
|---|---|---|
| P0 | `ServerPlatform`, arranque de um perfil, biblioteca, stream com cache temporário | 2–3 |
| P1 | Auth, sessões, admin, `profiles`, `profile_id` na BD | 2–3 |
| P2 | Multi-perfil, dispatcher TDLib partilhado, login Telegram por API, jobs por perfil, cache e prefetch | 4–5 |
| P3 | SPA MVP (login, biblioteca, procura, playlist, player, jobs, admin) | 4–6 |
| P4 | Deploy, Tunnel, backups, testes de autorização, documentação | 1–2 |
| **Total** | MVP familiar | **13–19 dias** |

- **Focado**: 2–3 semanas de calendário.
- **Noites/fins de semana**: 3–5 semanas.
- **Núcleo sem UI polida** (stream + auth + biblioteca numa página simples): **7–10 dias**.

O que pode esticar: streaming com seek sem cache completa (opção B), áudio em background no iOS e scoping na BD partilhada.

## 9. Recursos

| Recurso | Servidor em pé | Por utilizador |
|---|---|---|
| RAM | 350–500 MB | +120–200 MB (TDLib) |
| vCPU | ~0 em repouso | picos em pesquisa/sync |
| Disco | 4–6 GB | 30–80 MB de BD TDLib |
| Cache temporário | 0–5 GB configurável | partilhado |
| Dinheiro | 0 €/mês | 0 € |

Três perfis: **1,1–1,5 GB RAM**, 2 GB recomendados, 8–12 GB de CT.

## 10. Aceitação

- Admin cria/desactiva contas; sem auto-registo; sessão sobrevive a reinício.
- Cada perfil vê apenas as suas origens e playlists; acesso cruzado devolve 403/404.
- Reprodução com seek em Safari/PWA; áudio em background.
- Cancelar obtenção envia `/cancel` e não afecta outros perfis.
- Cache temporário respeita o limite e não expõe media entre perfis.

## 11. Riscos

| Risco | Mitigação |
|---|---|
| Singletons globais | Passar a instância; fallback: um JVM por perfil |
| `libtdjson` em Linux | Reutilizar JNA do desktop; validar no P0 |
| `td_receive` é global por biblioteca | Um dispatcher único por processo; nunca um loop por perfil (ver secção 12) |
| Seek sem cache | Começar com cache temporário; opção B só se necessário |
| iOS em background | Validar cedo em dispositivo real |
| Scope creep | Sem downloads, transcode, sync ou offline no MVP |
## 12. Decisões em aberto

Fechar antes de começar a codificar; as marcadas **BLOQUEIA** alteram a arquitetura.

1. **BLOQUEIA — dispatcher TDLib.** `td_receive` é global por biblioteca. Não pode existir um loop por perfil: é preciso **um único receptor** por processo que leia `@client_id` e encaminhe para o `TdClient` certo. Custo estimado: 1–2 dias.
2. **Quem autentica o Telegram de cada perfil.** Recomendação: o próprio utilizador introduz telefone/código/2FA; o admin só inicia e pode terminar a sessão. O admin nunca deve conhecer códigos de terceiros.
3. **Credenciais `api_id`/`api_hash`.** Uma por instância (recomendado) ou uma por perfil; guardar fora do repositório; no JVM vir de ambiente, não de `BuildConfig`.
4. **Política de sessões web.** Duração (30 dias), “lembrar-me”, rotação, TOTP obrigatório para `admin`, recuperação de password por reset do admin.
5. **Política de passwords.** Parâmetros Argon2id, comprimento mínimo, bloqueio após N tentativas, mensagens sem revelar existência de conta.
6. **Números do cache.** Teto (5 GB), TTL (10 min), profundidade de prefetch (2), limpeza no arranque e comportamento com disco cheio (rejeitar prefetch, servir faixa atual).
7. **Ciclo de vida do TDLib.** Arranque lazy no primeiro acesso e paragem após inatividade (ex. 30 min) para poupar RAM, ou todas as sessões sempre ligadas. Recomendação: lazy + paragem com estado persistido.
8. **Importação de ficheiros locais no servidor.** Fora do MVP? Confirmar; caso contrário, precisa de upload, pasta e indexação próprios.
9. **Quem pode adquirir.** Todos os perfis ou só alguns; limite de pedidos por perfil/dia; destino obrigatório definido pelo próprio perfil.
10. **Histórico e fila.** Guardar fila por perfil (sim, para retomar) e histórico de reprodução (recomendação: não guardar no servidor no MVP). Retenção e exportação.
11. **Backups.** Frequência, retenção e cifra; incluir teste de restauro; cache excluído.
12. **Deploy e atualização.** systemd com JAR vs imagem Docker; como atualizar e reverter; migrações da BD no arranque.
13. **Tunnel e Access.** `cloudflared` com token; `/admin` atrás de Cloudflare Access; HSTS; domínio e DNS.
14. **Validação iOS.** Áudio em background, persistência do cookie na PWA, AirPlay e seek — em dispositivo real, antes de investir na UI completa. Plano B se falhar.
15. **Testes mínimos de isolamento.** Matriz perfil A/B/C × endpoints (biblioteca, track, stream, playlist, jobs) e testes de cache partilhada sem fuga de autorização.
16. **Migração da biblioteca existente.** O owner pode importar a BD Android para o primeiro perfil? Caminhos e `file_id` diferem; decidir se há importador ou se começa vazia.
17. **Privacidade e uso.** Aviso de uso familiar, sem partilha pública, logs sem telefones/tokens; o dono da instância tem acesso administrativo a todos os perfis.
18. **Gatilho do fallback.** Se o dispatcher TDLib ou a refactor dos singletons (`AppText`, `Knowledge`) exceder 3 dias, mudar para **um processo por perfil** atrás de um gateway, aceitando +700–900 MB de RAM.
