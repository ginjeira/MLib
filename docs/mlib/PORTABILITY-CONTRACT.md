# MLib — contrato funcional de portabilidade MCat Player
**Versão:** 1.0 | **Baseline MLib:** `7231aa08022863a5a3f22138ff881fcf4045c340` | **Baseline MCat Player:** `a41cf5ceeea023d7b86484cf5c7bf5a7ae7b2132`

Este contrato define comportamento observável. As fixtures em `desktop/src/test/resources/portability/` são exemplos sintéticos anonimizados, não capturas live. Os payloads de callback são marcadores opacos fictícios; os testes nunca os enviam ao Telegram.

## Pesquisa e resultados
1. Tipos suportados: `track`, `album`, `playlist`, `artist`.
2. Providers: `deezer`, `soundcloud`; o filtro tem de ser aplicado ao bot, não apenas à UI.
3. Resultados numerados seguem linhas `N. título`; linhas de estado e ruído não são opções.
4. A selecção usa o callback/data exacto recebido do TDLib. Não reconstruir payloads nem aceitar teclado antigo.
5. Uma mensagem de resposta pode ser editada ou substituída por nova mensagem; a correlação usa o cursor/mensagem após a acção.

## Classificação de cardinalidade
- `playlist` e `album`: multi-faixa.
- `track`: pedido simples, termina após a primeira faixa válida.
- `artist`: não implica discografia nem multi-faixa. Só é multi se a opção seleccionada indicar contagem esperada superior a um.
- Contagem indicada e validada prevalece sobre heurística de tipo.

## Recepção/conclusão (comportamento MCat de referência)
- Recolher apenas mensagens de áudio novas após o cursor capturado imediatamente antes de seleccionar a opção.
- Desduplicar IDs de mensagem e preservar ordem cronológica.
- Contagem esperada positiva: terminar quando a contagem for atingida.
- Sinais textuais MCat (case-insensitive): `finished`, `download complete`, `completed`, `concluído`, `todas as faixas`, `process complete`, `done!`, `pronto!`; só concluir com pelo menos um áudio.
- Pedido simples: terminar após a primeira faixa válida.
- Colecção sem contagem/sinal: terminar após 9 segundos sem nova faixa, desde que tenha chegado pelo menos uma.
- Deadline MCat: 45 s para simples; 240 s para multi. Sem áudio = falha; com áudio mas sem critério = resultado parcial/timeout, não sucesso integral.
- MLib deverá registar o critério terminal; a implementação actual que exige exactamente `Finished.` não cumpre este contrato (ACQ-01).

## Encaminhamento e biblioteca
- Encaminhar cada mensagem de áudio individualmente por TDLib `forwardMessages`; não descarregar/re-enviar áudio.
- Preservar title/performer/duration e a identidade Telegram; actualizar/indexar o destino antes de deduplicar/retry.
- Uma opção pode ter sucessos parciais e falhas; nunca eliminar os sucessos já encaminhados.
- Pesquisa artist importa opções seleccionadas individualmente; não inventa álbum/playlist.
- Playlist mantém ordem e membros únicos; álbum mantém identidade e membership próprios.

## Regras de nomes e duplicados
- As funções MCat de limpeza/comparação são a referência dos golden cases.
- A comparação de gravação MCat é baseada em título/artista normalizados, não no file ID Telegram. A identidade Telegram é uma camada separada.
- O merge MCat de tracks preserva o registo primário, escolhe capa/artista/álbum melhores, FLAC sobre formato inferior, maior bitrate/tamanho, duração válida, favorito e documentId.
- O merge MCat de álbuns combina IDs/faixas sem repetição, metadata útil, capa não genérica, ano/género e createdAt mais antigo. A paridade deste merge não está implementada no MLib e pertence à Wave 2/4.
- `cleanPlaylistName` equivale ao `cleanCollectionTitle` MLib para os casos cobertos; blacklist/canonical MCat não é importada.

## Arquitectura e exclusões
TDLib já autenticada, Kotlin, SQLite e CloudSync MLib são os mecanismos. GramJS, Express, localStorage, API MCat, sessão MCat, proxy HTTP de áudio, bot credentials e catálogo demo/canónico são excluídos. Toda a adaptação deve preservar semântica sem criar dependência operacional entre projectos.

## Estados de prova
- **Código presente:** há implementação no HEAD.
- **Paridade de fonte:** golden cases demonstram o mesmo resultado da função MCat de referência.
- **Teste automatizado:** teste executado e aprovado no commit indicado.
- **Teste live aprovado:** evidência real Telegram/dispositivo, com data e execução. Build CI não substitui teste live.
