# MLib — matriz de fontes funcionais e portabilidade

**Actualizado:** 14-11-2026

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
| Pesquisa por álbum | Bot Telegram | UI e fluxo de aquisição | Portar pesquisa; criar modelo de álbum próprio numa fase posterior |
| Pesquisa por playlist | Bot Telegram | UI e aquisição multi-faixa | Portar e criar/actualizar playlist SQLite |
| Pesquisa por artista | Bot Telegram | Tipo de pesquisa suportado | Portar filtro; validar semântica de aquisição |
| Fornecedor Deezer | Bot Telegram | Filtro UI e callback | Portar callback TDLib |
| Fornecedor SoundCloud | Bot Telegram | Filtro UI e callback | Portar callback TDLib |
| Resultados numerados | Mensagem Telegram | Parse e selecção múltipla | Parser nativo MLib |
| Selecção de resultado | Botões inline | Callback via GramJS | `getCallbackQueryAnswer` TDLib |
| Recepção de áudio | Conversa bot | Monitorização do histórico | `getChatHistory` TDLib |
| Detecção de conclusão | Mensagens e contagens | Contagem, sinal final e silêncio | Estado de aquisição MLib |
| Encaminhamento | Destino configurado | Destino seleccionável | `forwardMessages` TDLib para GinjMusic ou destino escolhido |
| Deduplicação | Documento e metadata | Destino/fontes e metadata | fileKey + metadata conservadora; nunca destrutiva |
| Biblioteca | Índice operacional MCat | Biblioteca do player | SQLite MLib como catálogo autoritativo |
| Playlist local | Não é o catálogo do player | Criação/actualização automática | Reutilizar `Library.createPlaylist` e `addToPlaylist` |
| Álbum local | Metadados/fluxos do MCat | Gestão e conclusão de álbuns | Modelo de álbum separado ainda por implementar |
| Jobs e recuperação | Worker e estado próprios | Estado e progresso UI | Jobs MLib persistentes são trabalho futuro; primeira fatia usa estado em memória |
| Reprodução | Fora do foco principal | Player/MediaSession | Reutilizar Media3 e pipeline TDLib MLib |
| CloudSync | Não é o sync MLib | Referências de produto | Protocolo MLib independente; não sincronizar áudio/cache |
| Android Auto | Não é o foco | Lições de integração | Avaliar após estabilização Android |

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
- `MusicAcquisition.kt`: pesquisa no bot, filtros inline, parsing de resultados, aquisição sequencial, recolha de áudio, encaminhamento, deduplicação e integração SQLite.
- `AcquisitionSheet.kt`: interface inicial de pesquisa, selecção e destino.
- `SpotygramApp.kt`: serviço de aquisição ligado à aplicação e parser de faixas disponibilizado internamente.
- `SpotygramUI.kt` / `SettingsSheet.kt`: acesso à funcionalidade.
- Strings em inglês e português de Portugal.

Ainda não validado:
- Build CI desta implementação.
- Comunicação real com o bot e callbacks.
- Aquisição de faixa e playlist.
- Deduplicação em GinjMusic e reprodução.
- Recuperação após interrupção.
- Álbum e artista de ponta a ponta.

## 5. Decisões futuras

1. Persistir jobs e estados de aquisição em SQLite, com recuperação segura após reinício.
2. Implementar política de cancelamento local e tratamento de aquisições que continuam no bot.
3. Modelar álbuns e edições/releases, sem confundir álbuns com playlists.
4. Completar gestão de artistas e aquisição em lote apenas depois de validar os resultados reais do bot.
5. Adicionar revisão explícita para duplicados ambíguos.
6. Tornar configurável a criação automática de playlists.
7. Avaliar cache/offline e Android Auto depois de estabilizar a aquisição Android.

Esta matriz descreve direcção e estado observado. Uma capacidade só passa a validada após testes de execução documentados.
