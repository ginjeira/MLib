# Wave 1 — Fundações do servidor

**Estado:** `em validação` (22-11-2026)
**Regra:** só passa a `fechada` depois da validação do owner com uma conta Telegram real.

## Entregue

- Novo módulo Gradle `:server` (JVM 21), a compilar o domínio partilhado.
- `ServerPlatform`: diretórios, preferências JSON, SQLite JDBC, TDLib, chaves e limites por ambiente.
- **Dispatcher TDLib único**: `td_receive` global lido por uma thread e encaminhado por `@client_id`; teste puro de routing.
- Esquema de servidor: tabela `server_profiles` e `profile_id` em `tracks`, `sources`, `playlists`, `downloads` e `albums`, idempotente.
- Fila de inicialização: `MLIB_SOURCE_CHAT` adiciona a origem e indexa após o Telegram ficar `ready`.
- `MediaCache`: cache temporário partilhado, single-flight por faixa, refcount de leitores, TTL de 10 min e teto global (5 GB por omissão).
- HTTP (JDK `HttpServer`, deliberadamente sem framework nesta wave):
  - `GET /healthz`
  - `GET /api/version`
  - `GET /api/tracks` (token de serviço)
  - `GET /api/stream/{trackId}` com HTTP Range e 206/416
- Distribuição `:server:installDist` (script `bin/server`).
- `MLIB_TELEGRAM_ENABLED=0` permite arrancar e validar HTTP/BD sem rede.

## Evidência recolhida

- `:server:test` verde: **9 testes** (5 ranges, 1 esquema/reabertura, 3 routing).
- `:server:installDist` gera distribuição executável.
- Servidor instalado arranca e responde:
  - `GET /healthz` → `200` com `{"status":"empty","telegram":"welcome","tracks":0,...}`
  - `GET /api/tracks` sem token → `401`
  - `GET /api/tracks` com `Bearer` → `200 {"total":0,"tracks":[]}`
  - `GET /api/stream/abc` com token e Telegram não ligado → `503 Telegram is not ready`
- Reabertura da BD não recria nem duplica colunas (teste dedicado).

## Validação pendente do owner (fecha a wave)

Requisitos: JDK 21, `libtdjson` Linux (ou o `MLIB_NATIVE_DIR` do desktop), conta Telegram.

1. Preparar dependências nativas: `python3 tools/desktop-dependencies.py --target linux-x64 --state <estado>`.
2. Arrancar:
   ```bash
   export MLIB_SERVER_DATA=/srv/mlib/data
   export MLIB_NATIVE_DIR=/srv/mlib/native
   export MLIB_STREAM_TOKEN=<token>
   export MLIB_TELEGRAM_PHONE=+...
   export MLIB_SOURCE_CHAT=-100...
   export MLIB_SOURCE_TITLE="GinjMusic"
   bin/server
   ```
   Introduzir código/2FA no prompt (ou `MLIB_TELEGRAM_CODE`/`MLIB_TELEGRAM_PASSWORD`).
3. Confirmar `GET /healthz` com `telegram:"authorizationStateReady"` e `tracks > 0`.
4. `GET /api/tracks` e escolher três faixas de álbuns diferentes.
5. `curl -H "Authorization: Bearer <token>" -H "Range: bytes=0-65535" -o /tmp/a.bin http://host:8080/api/stream/<id>` → `206`, `Content-Range` correto.
6. Pedir a mesma faixa em dois clientes em paralelo; confirmar `cache.entries` sem duplicação e um só download no TDLib.
7. Reiniciar o processo; confirmar biblioteca, `server_profiles` e `profile_id` preservados.
8. Registar os resultados neste documento e mudar o estado para `fechada`.

## Limitações conhecidas (aceites nesta wave)

- Só um perfil está exposto; as colunas `profile_id` existem, mas o filtro por perfil chega na Wave 3.
- Sem contas, convites ou UI (Wave 2).
- Sem prefetch (Wave 5) e sem autenticação de utilizadores finais (Wave 2).
- Camada HTTP em `com.sun.net.httpserver`; a passagem para um framework fica para quando a UI/API crescerem.
- O JAR inclui dependências Compose porque o módulo partilhado contém a UI; não são usadas em runtime.
- Sem `systemd`, Tunnel ou backups (Wave 7).
