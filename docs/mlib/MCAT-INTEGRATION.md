# MLib — portabilidade da lógica de aquisição MCat / MCat Player

**Actualizado:** 14-11-2026  
**Âmbito:** auditoria estática dos repositórios `ginjeira/MCat` e `ginjeira/MCat-Player`, mais implementação inicial no MLib.  
**Decisão vinculativa:** MLib incorpora a lógica funcional; não utiliza o serviço, a API, a sessão ou a infraestrutura do MCat.

## 1. Decisão de arquitectura

Os três projectos têm papéis diferentes:

| Projecto | Papel |
| --- | --- |
| MCat | Referência da lógica original de pesquisa, selecção, obtenção e encaminhamento de áudio através do bot Telegram |
| MCat Player | Referência funcional da experiência de pesquisa, selecção, aquisição, deduplicação, playlists/álbuns e integração com a biblioteca |
| MLib | Implementação independente dessa lógica, usando o seu próprio código, catálogo SQLite e sessão TDLib |

MLib **não**:
- chama endpoints HTTP do MCat;
- autentica com credenciais Web do MCat;
- utiliza a sessão Telethon/GramJS do MCat ou do MCat Player;
- depende de um servidor MCat em execução;
- partilha jobs, estado, base de dados ou credenciais com esses projectos.

MLib reutiliza a sessão TDLib que já possui. O motor de aquisição é código nativo do MLib e comunica directamente com o bot Telegram.

## 2. Auditoria do MCat Player

O MCat Player foi a referência funcional principal. Os pontos de entrada auditados incluem:

- `src/components/views/SearchView.tsx`: interface de pesquisa, filtros, resultados numerados, selecção múltipla, destino Telegram e estado dos pedidos.
- `server.ts`, `getMcatBotEntity`: resolução do bot `@MusicsHuntersbot`.
- `server.ts`, `parseBotSearchResults`: extracção de resultados numerados.
- `server.ts`, `clickButtonByKeyword`: selecção dos filtros através de botões inline.
- `server.ts`, `processMcatJob`: selecção de resultados, recolha de mensagens de áudio, detecção de conclusão, deduplicação e encaminhamento.
- `server.ts`, rotas `/api/mcat/search` e `/api/mcat/jobs`: lógica de pesquisa e criação/execução de pedidos na aplicação original.

O MCat Player usa uma sessão Telegram própria através de GramJS/TelegramClient. Essa implementação não deve ser copiada como dependência: MLib já tem TDLib e deve traduzir as operações para a API TDLib.

### Comportamento funcional a preservar

1. Pesquisar um termo no bot.
2. Seleccionar tipo de conteúdo: track, album, playlist ou artist.
3. Seleccionar fornecedor: Deezer ou SoundCloud.
4. Interpretar os resultados numerados devolvidos pelo bot.
5. Permitir seleccionar uma ou várias opções.
6. Seleccionar o destino Telegram.
7. Para cada opção, seleccionar o botão correspondente no resultado.
8. Recolher as mensagens de áudio emitidas pelo bot.
9. Distinguir track individual de aquisição multi-faixa; usar contagem esperada quando disponível, sinal de conclusão e período de silêncio como fallback.
10. Deduplicar antes de encaminhar, preservando a identidade do objecto Telegram e a identidade da gravação.
11. Encaminhar as mensagens para o destino.
12. Actualizar a biblioteca e, quando o resultado é uma playlist, criar/actualizar a playlist local.

O código do MCat Player usa um limite de espera de 45 segundos para uma faixa e 240 segundos para colecções; consulta o histórico em intervalos de 2 segundos e usa 9 segundos sem novas faixas como fallback para terminar uma colecção sem contagem/sinal explícito. Estes valores são referências de comportamento, não garantias do bot.

## 3. Tradução técnica para TDLib no MLib

A implementação MLib deve usar:

| Operação | TDLib |
| --- | --- |
| Resolver o bot | `searchPublicChat` |
| Enviar termo/número | `sendMessage` + `inputMessageText` |
| Ler conversa e cursor | `getChatHistory` |
| Obter mensagem actualizada | `getMessage` |
| Clicar botão inline callback | `getCallbackQueryAnswer` com `callbackQueryPayloadData` |
| Encaminhar áudio | `forwardMessages` |
| Interpretar áudio | `SpotygramApp.parseTrack` (parser existente) |
| Actualizar catálogo | `Library.source`, `Library.upsert`, `Library.reload` |
| Criar playlist | `Library.createPlaylist` / `Library.addToPlaylist` |

Os dados `bytes` dos callbacks são transportados pelo JSON TDLib em Base64. MLib deve usar os dados exactos do botão devolvido pelo Telegram; não deve reconstruir nem adivinhar payloads.

A autenticação continua a ser a autenticação Telegram já existente no MLib. Não existe um segundo login nem uma sessão separada para aquisição.

## 4. Fluxo implementado no MLib

Ficheiros:
- `shared/src/main/kotlin/app/spotygram/MusicAcquisition.kt`
- `shared/src/main/kotlin/app/spotygram/AcquisitionSheet.kt`
- integração em `SpotygramApp.kt`, `SpotygramUI.kt` e `SettingsSheet.kt`

Fluxo actual:
1. O utilizador abre **Descobrir música** nas definições.
2. MLib resolve `@MusicsHuntersbot` através da conta Telegram autenticada.
3. Envia a pesquisa e selecciona os filtros por callbacks TDLib.
4. Apresenta os resultados numerados para selecção.
5. O utilizador escolhe o destino Telegram; por defeito, MLib procura o grupo GinjMusic.
6. Antes de adquirir, MLib adiciona o destino às fontes e tenta indexar o seu histórico completo para que a deduplicação use o catálogo existente.
7. MLib selecciona sequencialmente cada opção, recolhe mensagens de áudio novas e encaminha-as para o destino.
8. As mensagens encaminhadas são interpretadas pelo parser existente e gravadas no catálogo SQLite.
9. Para resultados do tipo playlist, MLib cria ou actualiza uma playlist local com as faixas obtidas.

A conversa com o bot é serializada por um `Mutex` único. Isto evita que pesquisas e aquisições iniciadas no MLib misturem callbacks e respostas. As pesquisas mantêm validade local de 20 minutos.

## 5. Destino e biblioteca

O utilizador confirmou que a conta Telegram autenticada no MLib tem acesso ao grupo `GinjMusic`.

O destino de aquisição é distinto do canal privado usado pelo CloudSync MLib. Não confundir:
- `GinjMusic`: destino de áudio;
- canal `mlib-sync`: transporte privado de sincronização de metadata;
- conversa privada com o bot: origem temporária dos resultados de aquisição.

O MLib encaminha mensagens Telegram; não descarrega o áudio para voltar a enviá-lo como ficheiro local. O catálogo guarda a identidade da mensagem no destino e o player continua a usar o mecanismo TDLib/Media3 já existente.

## 6. Deduplicação e segurança dos dados

A identidade do objecto Telegram e a identidade da gravação são diferentes.

A implementação inicial compara:
- `fileKey` TDLib quando disponível;
- título normalizado;
- artista normalizado quando ambos os lados o têm;
- duração, com tolerância curta.

A indexação do destino é feita antes da aquisição quando ainda não está completa. A deduplicação é conservadora: não elimina mensagens nem gravações e não substitui metadata de forma destrutiva. Casos ambíguos devem ser encaminhados para revisão numa fase posterior.

Para playlists:
- a ordenação das faixas recebidas é preservada;
- faixas já existentes podem ser reutilizadas;
- a playlist local é criada/actualizada sem duplicar IDs;
- falhas parciais não apagam as faixas já obtidas.

## 7. Limitações conhecidas da primeira implementação

- Estado das pesquisas e jobs ainda reside em memória; uma terminação do processo exige nova pesquisa e não retoma automaticamente uma aquisição interrompida.
- Não existe cancelamento remoto do processamento iniciado no bot.
- A serialização protege operações iniciadas pelo MLib, mas uma interacção manual simultânea com o mesmo bot/conversa pode interferir.
- O protocolo do bot é inferido a partir de texto, botões e mensagens Telegram; alterações do bot podem exigir ajustes.
- A criação automática de playlists está implementada no fluxo inicial; o modelo SQLite actual do MLib não tem ainda uma entidade de álbum separada.
- Pesquisa por artista está disponível como filtro do bot, mas a semântica de aquisição de um artista inteiro precisa de validação funcional.
- A indexação completa de GinjMusic pode demorar, dependendo do histórico existente.
- Não foi ainda realizado um teste live de pesquisa, callback, aquisição, encaminhamento, deduplicação e reprodução.

## 8. Critérios de validação

A integração só deve ser considerada funcional depois de todos estes testes:

1. Pesquisar uma faixa com Deezer e SoundCloud.
2. Confirmar que os filtros são aplicados no bot, não apenas apresentados na interface.
3. Seleccionar um resultado e receber áudio.
4. Confirmar encaminhamento para GinjMusic.
5. Confirmar que a mensagem encaminhada aparece na biblioteca e reproduz.
6. Repetir a aquisição e confirmar que não é criada uma segunda entrada para a mesma gravação.
7. Adquirir uma playlist pequena e confirmar contagem, ordem, importação e playlist local.
8. Testar uma aquisição parcialmente falhada e confirmar que as faixas válidas permanecem.
9. Testar expiração de pesquisa, timeout, FloodWait e perda de ligação.
10. Confirmar que o canal CloudSync MLib e os dados do Spotygram não são afectados.

**Estado:** implementação inicial de código submetida à branch em 14-11-2026; CI e testes Telegram reais ainda não validados.
