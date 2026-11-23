# MLib Server — waves de execução

Registado a **23-11-2026**. Projecto separado da app Android.

## Regras de gestão

1. **Uma wave de cada vez.** A wave N+1 só começa depois de a N ser declarada fechada.
2. **Sem percentagens.** Uma wave está `aberta`, `em validação` ou `fechada`. Nada entre.
3. **Fecho = evidência.** Cada wave termina com: build verde, testes indicados, checklist manual e um registo `docs/mlib/server/wave-N-closure.md` com comandos e resultados.
4. **Âmbito congelado.** Pedidos novos durante uma wave vão para o backlog da wave seguinte; não entram a meio.
5. **Sem entrega parcial declarada.** Se uma wave não cumprir a aceitação, continua aberta; não se avança.
6. **Validação do owner** em cada fecho, no ambiente real (CT + Tunnel + iPhone quando aplicável).

## Wave 1 — Fundações do servidor

**Objectivo:** um perfil, biblioteca real, streaming funcional. Sem autenticação de utilizadores (usa token de serviço por ambiente).

- Módulo `mlib-server` (Ktor) a reutilizar o domínio existente.
- `ServerPlatform`: ficheiros, preferências, `openDatabase` JDBC, `tdlib`.
- **Dispatcher TDLib único** (`td_receive` global) com routing por `@client_id`.
- BD partilhada com migrações e `profile_id` nas tabelas aplicáveis.
- Um perfil configurado por ambiente; biblioteca indexada de uma origem.
- `GET /api/stream/{trackId}` com cache temporário, single-flight, refcount e TTL.
- `GET /healthz` e logs estruturados.

**Aceitação:** reproduzir três faixas de álbuns diferentes via HTTP com seek; cache partilhada serve dois clientes com um só download; reinício não perde a BD; nenhuma chamada Android/Compose em runtime.

**Fora de âmbito:** contas, onboarding, UI, prefetch, jobs.

**Estimativa:** 4–5 dias.

## Wave 2 — Acesso e administração

**Objectivo:** entrar no MLib com conta criada pelo admin, sem Telegram ainda.

- `accounts`, `sessions`, `profiles`; Argon2id; convites de uso único (48 h).
- Login/logout, cookie `HttpOnly; Secure; SameSite=Lax`, rotação, CSRF, rate limit.
- Papéis `admin`/`listener`; TOTP obrigatório para admin.
- Página de administração: criar conta, emitir convite, ver estado, desactivar, revogar sessões.
- UI mínima de login e de convite (definir password).

**Aceitação:** admin cria conta e convite; utilizador define password e entra; conta desactivada perde acesso imediatamente; sem auto-registo; sessão sobrevive a reinício; rate limit e CSRF validados.

**Estimativa:** 3–4 dias.

## Wave 3 — Onboarding do utilizador

**Objectivo:** cada perfil liga a sua conta Telegram e fica operacional.

- Onboarding: Telegram (telefone/código/2FA ou QR), bot, destino (`can_post`), origens.
- Estados `pending_telegram → pending_destination → pending_sources → ready`; retomável.
- `403 setup_required` em biblioteca/stream/aquisição antes de `telegram_linked`.
- Restauro opcional via CloudSync (FR-12: favoritas e capas de faixa incluídas).
- Indexação inicial em background com progresso.

**Aceitação:** três perfis com contas Telegram distintas; um perfil incompleto não vê biblioteca nem consome TDLib; reinício preserva o passo; restauro de CloudSync traz playlists, álbuns, capas, favoritas e definições quando existir.

**Estimativa:** 4–5 dias.

## Wave 4 — Biblioteca e procura

**Objectivo:** navegar a biblioteca de cada perfil na web.

- Endpoints: faixas, playlists, álbuns, artistas, fontes; paginação e ordenação.
- Procura por título/artista/álbum; filtros por fonte e offline.
- Favoritas (adicionar/remover), capas de faixa e de playlist.
- UI: Início, Biblioteca, Procurar, Playlist, Álbum, Artista.

**Aceitação:** cada perfil vê apenas as suas origens; matriz de acesso cruzado devolve 403/404; favoritas e capas persistem e sobrevivem a reinício; listas de 1 000+ faixas com paginação fluida.

**Estimativa:** 4–5 dias.

## Wave 5 — Player e prefetch

**Objectivo:** reprodução utilizável no iPhone.

- Player fixo: capa, título, progresso, play/pause, seguinte, volume; Media Session.
- Fila por perfil; retomar posição.
- Prefetch de 1–2 faixas a partir da fila, cancelável, com limites de espaço.
- PWA instalável; validação de áudio em background no Safari.
- Comportamento com disco cheio e cache expirada.

**Aceitação:** tocar um álbum do início ao fim sem intervenção; seek em faixas já em cache; prefetch elimina a espera no “seguinte”; áudio continua com o ecrã bloqueado; PWA instalada no iPhone.

**Estimativa:** 4–5 dias.

## Wave 6 — Aquisição e jobs

**Objectivo:** paridade com o Android na aquisição.

- Pesquisa no bot, revisão, commit, jobs por perfil, SSE de progresso.
- Cancelar com `/cancel`; retoma; estado parcial; erros accionáveis.
- Destino obrigatório por perfil; limites do bot visíveis por conta.
- UI de pesquisa, revisão e jobs.

**Aceitação:** adquirir faixa, álbum e playlist; teste de 27 faixas espera pela contagem; cancelar a meio não afecta outros perfis; job sobrevive a reinício; três perfis em paralelo sem cruzar resultados.

**Estimativa:** 5–6 dias.

## Wave 7 — Operação e endurecimento

**Objectivo:** pronto para uso familiar contínuo.

- systemd + JAR; `cloudflared`; TLS; `/admin` com Cloudflare Access; HSTS e cabeçalhos.
- Backups automáticos do SQLite + teste de restauro documentado.
- Testes de isolamento completos, carga com três streams, limites de cache e prefetch.
- Logs sem segredos; `/healthz` com métricas; runbook de incidentes.
- Documentação de instalação (uma página) e de atualização/rollback.

**Aceitação:** instalação de raiz num CT seguindo o guia; restauro de backup num CT limpo; três perfis a ouvir em simultâneo; reinício sob carga sem perda de sessões; nenhum segredo em logs ou payloads.

**Estimativa:** 3–4 dias.

## Total

**27–34 dias de trabalho focado**, sequenciais, sem waves a meio. Cada fecho tem evidência e validação do owner antes de abrir a seguinte.

## Dependências entre waves

- W2 precisa do dispatcher e do `ServerPlatform` da W1.
- W3 precisa de contas e sessões da W2 e do FR-12 na app Android para restaurar curadoria.
- W4–W6 precisam do perfil já operacional da W3.
- W7 fecha o produto; sem ela não há declaração de “pronto”.
