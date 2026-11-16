# MLib — contrato funcional de portabilidade MCat Player
**Versão:** 1.1 (aditamento 16-11-2026) | **Baseline MLib:** `7231aa08022863a5a3f22138ff881fcf4045c340` | **Baseline MCat Player:** `a41cf5ceeea023d7b86484cf5c7bf5a7ae7b2132`

Este contrato define comportamento observável. As fixtures em `desktop/src/test/resources/portability/` são exemplos sintéticos anonimizados, não capturas live. Os payloads de callback são marcadores opacos fictícios; os testes nunca os enviam ao Telegram.

## Pesquisa e resultados
1. Tipos suportados: `track`, `album`, `playlist`, `artist`.
2. Providers: `deezer`, `soundcloud`; o filtro tem de ser aplicado ao bot, não apenas à UI.
3. Resultados numerados seguem linhas `N. título`; linhas de estado e ruído não são opções.
4. A selecção usa o callback/data exacto recebido do TDLib. Não reconstruir payloads nem aceitar teclado antigo.
5. Uma mensagem de resposta pode ser editada ou substituída por nova mensagem; a correlação usa o cursor/mensagem após a acção.

## Classificação de cardinalidade
- `playlist` e `album`: multi-faixa.
- `track`: pedido simples; a primeira faixa é recolhida, mas o turno só termina com sinal terminal explícito.
- `artist`: não implica discografia nem multi-faixa. Só é multi se a opção seleccionada indicar contagem esperada superior a um.
- Contagem indicada e validada prevalece sobre heurística de tipo.

## Recepção/conclusão (comportamento MCat de referência)
- Recolher apenas mensagens de áudio novas após o cursor capturado imediatamente antes de seleccionar a opção.
- Desduplicar IDs de mensagem e preservar ordem cronológica.
- A contagem esperada positiva é um indicador de progresso, não uma confirmação de que o bot libertou o turno. Atingir 20/20 sem sinal final continua pendente.
- Sinais textuais MCat (case-insensitive): `finish`, `finished`, `download complete`, `completed`, `concluído`, `todas as faixas`, `process complete`, `done!`, `pronto!`.
- Uma colecção 20/18 com sinal final `finish` está concluída com as 18 faixas recebidas; a contagem esperada não bloqueia faixas indisponíveis, incluindo por restrições geográficas.
- Pedido simples: a primeira faixa é recolhida, mas o turno só é libertado com sinal final explícito.
- Silêncio de 9 segundos é apenas um indicador de inactividade; não prova que o bot terminou.
- Deadline: 45 s para simples; 240 s para multi. Sem áudio e sem sinal = falha pendente; com áudio mas sem sinal = resultado parcial pendente, sem libertar o bot.
- Um novo pedido só pode ser enviado depois do sinal terminal da opção anterior. A serialização por Mutex não substitui esta confirmação.
- Em timeout, persistir a conversa, o cursor e as opções pendentes. A retoma espera o sinal terminal depois do cursor antes de reconstruir a pesquisa; contagem, silêncio ou descarte não libertam a conversa.

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


## Exclusividade do turno Telegram (ACQ-05)
A conversa stateful do bot fica reservada desde que a pesquisa devolve opções até à chegada de um sinal terminal explícito. Em timeout, o job conserva chat/cursor/opções e a retoma aguarda o sinal final antes de enviar qualquer nova pesquisa ou selecção. Não se pode avançar para a opção seguinte com base apenas na contagem, no silêncio ou na primeira faixa. Na retoma parcial, a deduplicação é por faixa, reutilizando o que já foi importado e encaminhando apenas o que falta.
