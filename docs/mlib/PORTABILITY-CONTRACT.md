# MLib — contrato funcional de portabilidade MCat Player
**Versão:** 1.2 (correcção de paridade MCat, 16-11-2026) | **Baseline MLib:** `7231aa08022863a5a3f22138ff881fcf4045c340` | **Baseline MCat Player:** `a41cf5ceeea023d7b86484cf5c7bf5a7ae7b2132`

Este contrato define comportamento observável. As fixtures em `desktop/src/test/resources/portability/` são exemplos sintéticos anonimizados, não capturas live. Os payloads de callback são marcadores opacos fictícios; os testes nunca os enviam ao Telegram.

## Pesquisa e resultados
1. Tipos suportados: `track`, `album`, `playlist`, `artist`.
2. Providers: `deezer`, `soundcloud`; o filtro tem de ser aplicado ao bot, não apenas à UI.
3. Resultados numerados seguem linhas `N. título`; linhas de estado e ruído não são opções.
4. A selecção usa o callback/data exacto recebido do TDLib. Não reconstruir payloads nem aceitar teclado antigo.
5. Uma mensagem de resposta pode ser editada ou substituída por nova mensagem; a correlação usa o cursor/mensagem após a acção.

## Classificação de cardinalidade
- `playlist` e `album`: multi-faixa.
- `track`: pedido simples; termina quando chega a primeira faixa.
- `artist`: só é multi-faixa quando a opção seleccionada indica contagem esperada superior a um.
- A classificação usada pelo worker MCat é: playlist, album ou `expectedTrackCount > 1`.

## Recepção/conclusão (comportamento MCat de referência)
- Recolher apenas mensagens de áudio novas após o cursor capturado imediatamente antes de seleccionar a opção.
- Desduplicar IDs de mensagem e preservar a ordem cronológica.
- Para playlist/álbum/artist multi-faixa, uma contagem esperada positiva conclui a recepção assim que o número de áudios recebidos atinge essa contagem.
- Um sinal textual reconhecido conclui a recepção se já tiver sido recebido pelo menos um áudio, mesmo que a contagem esperada não tenha sido atingida. Inclui `finished`, `download complete`, `completed`, `concluído`, `todas as faixas`, `process complete`, `done!` e `pronto!`; a comparação é case-insensitive.
- Pedido simples: a primeira faixa recebida conclui a recepção; não exige mensagem final adicional.
- Colecção: depois de pelo menos um áudio, 9 segundos sem novas faixas concluem o lote.
- Deadline: 45 s para pedido simples; 240 s para multi-faixa. Sem áudio no deadline, a opção falha. Com áudio, o resultado é parcial.
- A contagem esperada, sinal textual e silêncio são critérios de conclusão herdados do MCat, não apenas indicadores.
- Cada saída do ciclo de recepção — contagem, sinal com áudio, faixa simples, silêncio ou deadline — encerra a opção e permite ao worker MCat avançar para a opção seguinte. O timeout com áudio é parcial; sem áudio é falha. Não existe uma espera adicional por sinal terminal para libertar a sequência.
- As regras de cursor, deduplicação de IDs e ordem cronológica são adaptações técnicas TDLib e devem preservar o resultado observável.

## Encaminhamento e biblioteca
- Encaminhar cada mensagem de áudio individualmente por TDLib `forwardMessages`; não descarregar/re-enviar áudio.
- Na retoma de uma colecção parcial, deduplicar faixa a faixa: reutilizar as já importadas e encaminhar as novas. Uma faixa existente não justifica saltar a colecção inteira.
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


## Exclusividade do turno Telegram
A sequência de opções deve reproduzir o worker MCat: após cada opção, a próxima pode ser processada quando os critérios de conclusão acima forem satisfeitos. Um Mutex local pode impedir concorrência entre chamadas MLib, mas não deve impor um sinal terminal adicional que o MCat não exige. Qualquer endurecimento diferente deve ser explicitamente classificado como alteração funcional, não como portabilidade.
