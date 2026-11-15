# MLib — matriz de fontes funcionais e portabilidade

**Actualizado:** 15-11-2026

## 1. Papel de cada projecto

| Projecto | Papel | O que MLib aproveita |
| --- | --- | --- |
| Spotygram | Base técnica do MLib | Kotlin/Compose, TDLib, SQLite, Media3, biblioteca, playlists, downloads, CloudSync e integração Android |
| MCat | Referência da lógica original de aquisição | Protocolo de pesquisa no bot, filtros, selecção de resultados, recolha de áudio, encaminhamento e deduplicação |
| MCat Player | Referência funcional principal | UX de pesquisa/seleção, execução sequencial de aquisições, recolha de faixas, deduplicação, playlists/álbuns e lições de produto |
| MLib | Produto independente | Portabilidade nativa da lógica para TDLib e integração na sua biblioteca |

**Não existe dependência operacional entre MLib e MCat/MCat Player.** MLib não utiliza API HTTP do MCat, sessões Telethon/GramJS, bases de dados, jobs ou servidores desses projectos. O código de aquisição é implementado dentro do MLib e usa a sessão TDLib que já existe.

## 2. Matriz de capacidades

| Capacidade | MCat | MCat Player | Direcção MLib |
| --- | --- | --- | --- |
| Pesquisa por faixa | Bot Telegram | UI e fluxo completos | Portar para TDLib |
| Pesquisa por álbum | Bot Telegram | UI e fluxo de aquisição | Pesquisa/acquisition nativa; entidade álbum separada e conclusão de álbum existente |
| Pesquisa por playlist | Bot Telegram | UI e aquisição multi-faixa | Portar, preservar ordem, criar/actualizar playlist configurável |
| Pesquisa por artista | Bot Telegram | Tipo de pesquisa suportado | Portar filtro; opções representam faixas individuais, sem discografia fictícia |
| Fornecedor Deezer | Bot Telegram | Filtro UI e callback | Portar callback TDLib |
| Fornecedor SoundCloud | Bot Telegram | Filtro UI e callback | Portar callback TDLib |
| Resultados numerados | Mensagem Telegram | Parse e selecção múltipla | Parser nativo MLib |
| Selecção de resultado | Botões inline | Callback via GramJS | `getCallbackQueryAnswer` TDLib |
| Recepção de áudio | Conversa bot | Monitorização do histórico | `getChatHistory` TDLib |
| Detecção de conclusão | Mensagens e contagens | Contagem, sinal final e silêncio | Estado de aquisição MLib |
| Encaminhamento | Destino configurado | Destino seleccionável | `forwardMessages` TDLib para GinjMusic ou destino escolhido |
| Deduplicação | Documento e metadata | Destino/fontes e metadata | fileKey + metadata conservadora + candidatos da mesma resposta; nunca destrutiva |
| Biblioteca | Índice operacional MCat | Biblioteca do player | SQLite MLib como catálogo autoritativo |
| Playlist local | Não é o catálogo do player | Criação/actualização automática | Reutilizar `Library.createPlaylist` e `addToPlaylist` |
| Álbum local | Metadados/fluxos do MCat | Gestão e conclusão de álbuns | Modelo de álbum separado, migração SQLite, CRUD, ordenação, CloudSync, detecção local e conclusão em álbum existente |
| Jobs e recuperação | Worker e estado próprios | Estado e progresso UI | Journal de trabalho persistente em preferências MLib; pesquisa reconstruída e repetição idempotente após refresh do destino |
| Reprodução | Fora do foco principal | Player/MediaSession | Reutilizar Media3 e pipeline TDLib MLib |
| CloudSync | Não é o sync MLib | Referências de produto | Protocolo MLib independente; não sincronizar áudio/cache |
| Android Auto | Não é o foco | MediaLibraryService e árvore de navegação | MLib expõe álbuns, playlists, faixas e favoritos pela mesma PlaybackQueue; validação física pendente |

## 3. Regras de portabilidade

- Portar comportamento, não copiar a arquitectura de servidor do MCat Player.
- Usar TDLib já autenticada; não criar segundo cliente ou login.
- Usar callbacks inline do Telegram com os payloads exactos recebidos do TDLib.
- Serializar operações de bot, porque pesquisa e aquisição partilham uma conversa com estado.
- Usar o parser `parseTrack` e o catálogo `Library` existentes.
- Encaminhar a mensagem Telegram recebida; não descarregar e reenviar áudio sem necessidade.
- Indexar o destino antes de deduplicar quando o histórico ainda não está completo.
- Distinguir identidade de mensagem/documento, identidade da gravação e identidade da entrada na biblioteca.
- Não apagar nem substituir automaticamente faixas existentes.
- Preservar sucessos parciais e permitir recuperação/repetição segura.
- Manter o serviço MCat de produção inalterado.

## 4. Estado da primeira fatia MLib

Implementado no branch `feat/mlib-independent-identity`:
- `MusicAcquisition.kt`: pesquisa no bot, filtros inline, parsing de resultados, aquisição sequencial, recolha paginada de áudio, encaminhamento, deduplicação, retoma persistente e integração SQLite.
- `AcquisitionSheet.kt`: interface inicial de pesquisa, selecção e destino.
- `SpotygramApp.kt`: serviço de aquisição ligado à aplicação e parser de faixas disponibilizado internamente.
- `SpotygramUI.kt` / `SettingsSheet.kt`: acesso à funcionalidade.
- Strings em inglês e português de Portugal.

Ainda não validado:
- Build/test CI do último commit (a execução observada anteriormente falhou em dois testes de correspondência de títulos; a correcção está no branch, ainda sem nova execução).
- Comunicação real com o bot e callbacks.
- Aquisição de faixa, playlist e álbum no Telegram.
- Deduplicação em GinjMusic e reprodução.
- Recuperação após interrupção real do processo.
- Conclusão de álbum com dados reais e validação da semântica de artista.
- Navegação de álbuns no Android Auto em unidade principal física.

## 5. Decisões futuras

1. Substituir o journal simples de preferências por um registo transaccional SQLite com estado por opção/faixa, caso os testes live revelem necessidade; o journal de retoma já está implementado.
2. Implementar política de cancelamento local e tratamento de aquisições que continuam no bot.
3. Completar pré-visualização de faixas em falta e aprovação individual antes de completar um álbum; o fluxo actual reutiliza automaticamente faixas existentes e acrescenta as restantes.
4. Validar no bot a semântica de artista (opções de faixas individuais) e os limites de colecções grandes.
5. Adicionar revisão explícita para duplicados ambíguos.
6. Validar FloodWait, reconexão e recuperação em dispositivos reais.
7. Avaliar cache/offline e Android Auto depois de estabilizar a aquisição Android.

Esta matriz descreve direcção e estado observado. Uma capacidade só passa a validada após testes de execução documentados.
