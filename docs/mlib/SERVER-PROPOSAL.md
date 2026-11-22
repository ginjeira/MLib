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
2. **Streaming on-demand**: o servidor obtém os bytes do TDLib e responde a `Range`. Duas opções:
   - **A (recomendada para o MVP)**: na primeira reprodução, descarregar o ficheiro para um **cache temporário** (LRU, fora da biblioteca) e servir daí. Não é “download offline”; é plumbing de leitura.
   - **B**: pedir ao TDLib apenas os offsets necessários (`downloadFile` com offset/limit) e ir servindo à medida. Suporta seek sem guardar o ficheiro inteiro, mas é mais código e mais frágil.
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

## 5. Streaming

- `GET /api/stream/{trackId}`: valida sessão → track do perfil → bytes do TDLib → `206` com `Range`.
- Cache temporário por ficheiro (opção A): apagar por LRU e no arranque, com limite de disco configurável.
- Sem autorização por caminho; nunca aceitar `chatId/messageId` do cliente.
- Sem transcode: MP3/AAC/FLAC como estão.

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
| P2 | Multi-perfil, TDLib por conta, login Telegram por API, jobs por perfil | 3–4 |
| P3 | SPA MVP (login, biblioteca, procura, playlist, player, jobs, admin) | 4–6 |
| P4 | Deploy, Tunnel, backups, testes de autorização, documentação | 1–2 |
| **Total** | MVP familiar | **12–18 dias** |

- **Focado**: 2–3 semanas de calendário.
- **Noites/fins de semana**: 3–5 semanas.
- **Núcleo sem UI polida** (stream + auth + biblioteca numa página simples): **6–9 dias**.

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
| Seek sem cache | Começar com cache temporário; opção B só se necessário |
| iOS em background | Validar cedo em dispositivo real |
| Scope creep | Sem downloads, transcode, sync ou offline no MVP |
