package app.spotygram

data class ChangelogEntry(
    val version: String,
    val english: String,
    val portuguese: String,
)

object Changelog {
    val entries =
        listOf(
            ChangelogEntry(
                version = "0.1.31",
                english =
                    "CloudSync now carries track covers between devices; favourites already travelled. Secrets such as sessions, API hashes, passwords and TOTP are never included, and the new sync format still reads older packets.",
                portuguese =
                    "O CloudSync passa a transportar as capas de faixa entre dispositivos; as favoritas já viajavam. Segredos como sessões, hashes de API, passwords e TOTP nunca são incluídos, e o novo formato continua a ler pacotes antigos.",
            ),
            ChangelogEntry(
                version = "0.1.30",
                english =
                    "Fixed playlist acquisition stopping after a short pause, which could open the review with one track of a 27-track playlist. Cancelling a search no longer shows an error, and an acquisition can now be cancelled from the progress block, sending /cancel to the bot to stop its download. The artist page fits all actions, its information follows the app language, search opens the acquisition dialog prefilled instead of searching immediately, and offline downloads ask for confirmation.",
                portuguese =
                    "Corrigida a obtenção de playlists que parava após uma pausa curta e podia abrir a revisão com uma faixa de 27. Cancelar uma pesquisa já não mostra erro, e uma obtenção pode agora ser cancelada no bloco de progresso, enviando /cancel ao bot para parar o download. A página de artista comporta todas as ações, a informação segue a língua da app, a pesquisa abre o diálogo de aquisição pré-preenchido em vez de pesquisar logo, e os downloads offline pedem confirmação.",
            ),
            ChangelogEntry(
                version = "0.1.29",
                english =
                    "New navigation with Home, Library, Search and Settings. Home gathers what you were listening to, recent tracks, favourites, playlists and albums, marking the albums whose missing tracks are already known. Artist pages group the library and show albums, tracks, sources and information from MusicBrainz and Wikipedia. Source management moved to Settings, and the player now has an artwork gradient with solid surfaces on the bar and mini-player.",
                portuguese =
                    "Nova navegação com Início, Biblioteca, Procurar e Definições. O Início reúne o que estavas a ouvir, faixas recentes, favoritos, playlists e álbuns, marcando os álbuns cujas faixas em falta já são conhecidas. As páginas de artista agrupam a biblioteca e mostram álbuns, faixas, fontes e informação do MusicBrainz e da Wikipédia. A gestão de fontes passou para Definições, e o player tem agora um gradiente da arte com superfícies sólidas na barra e no mini-player.",
            ),
            ChangelogEntry(
                version = "0.1.28",
                english =
                    "Android Auto now has a Home screen with recent, favourites, offline and playlists, and the assistant can play anything already in the library by voice. Covers picked on the phone also appear in the car. Acquisition stays on the phone because Android Auto only allows browsing and playback.",
                portuguese =
                    "O Android Auto tem agora um Início com recentes, favoritos, offline e playlists, e o Assistente consegue tocar por voz o que já está na biblioteca. As capas escolhidas no telemóvel também aparecem no carro. A aquisição fica no telemóvel porque o Android Auto só permite navegação e reprodução.",
            ),
            ChangelogEntry(
                version = "0.1.27",
                english =
                    "Completing an album now opens the dialog and lists the tracks you already have and the ones missing, with a completion bar. Acquired tracks can be reviewed before they are written to the library. MLib can be locked with the device screen lock. Album details show sourced information from MusicBrainz and Wikipedia. Album and playlist covers can be picked from the device, and AMOLED and high-contrast themes were added.",
                portuguese =
                    "Completar um álbum abre agora o diálogo e lista as faixas que já tens e as que faltam, com uma barra de completude. As faixas adquiridas podem ser revistas antes de entrarem na biblioteca. O MLib pode ser bloqueado com o bloqueio do dispositivo. O detalhe do álbum mostra informação com fonte do MusicBrainz e da Wikipédia. As capas de álbum e playlist podem ser escolhidas do dispositivo, e foram adicionados temas AMOLED e de alto contraste.",
            ),
            ChangelogEntry(
                version = "0.1.26",
                english =
                    "Completing an album now closes the album detail, opens the acquisition screen immediately and keeps the correct album context. The version label now opens a natural-language changelog. Automatic releases were disabled after this point.",
                portuguese =
                    "Completar um álbum fecha agora o detalhe, abre imediatamente o ecrã de aquisição e mantém o contexto correto do álbum. A etiqueta da versão abre um changelog em linguagem natural. As releases automáticas foram desativadas a partir daqui.",
            ),
            ChangelogEntry(
                version = "0.1.25",
                english =
                    "Telegram connection failures now offer Reconnect and stop waiting after ten seconds. The album completion action still needs a separate fix.",
                portuguese =
                    "As falhas de ligação ao Telegram passam a oferecer Reconectar e deixam de esperar após dez segundos. A ação de completar álbum ainda necessita de uma correção separada.",
            ),
            ChangelogEntry(
                version = "0.1.24",
                english =
                    "Added the Reconnect action and a bounded wait for Telegram. Automatic releases were used through this version.",
                portuguese =
                    "Adicionada a ação Reconectar e uma espera limitada pela ligação ao Telegram. As releases automáticas foram usadas até esta versão.",
            ),
            ChangelogEntry(
                version = "0.1.23",
                english =
                    "Album completion starts its search inside the acquisition screen. Searches first try a credible result from the bot chat history before asking the bot again. The library JSON can be exported as a shareable file.",
                portuguese =
                    "Completar álbum inicia a pesquisa dentro do ecrã de aquisição. As pesquisas tentam primeiro um resultado credível do histórico do bot antes de voltar a pedir. O JSON da biblioteca pode ser exportado como ficheiro partilhável.",
            ),
            ChangelogEntry(
                version = "0.1.22",
                english =
                    "Past acquisition jobs can be searched again, and album completion previews the tracks already in the album. Duplicate review and acquisition history are persisted.",
                portuguese =
                    "Os pedidos antigos podem ser pesquisados novamente, e completar álbum pré-visualiza as faixas já existentes. A revisão de duplicados e o histórico de aquisição ficam guardados.",
            ),
            ChangelogEntry(
                version = "0.1.21",
                english =
                    "Owned groups and channels are detected correctly when adding a source, GinjMusic is available again, updates are checked when the app opens, and the library search can search Telegram directly.",
                portuguese =
                    "Os grupos e canais próprios são detetados corretamente ao adicionar uma origem, o GinjMusic volta a estar disponível, as atualizações são verificadas ao abrir a app e a pesquisa da biblioteca pode pesquisar diretamente no Telegram.",
            ),
            ChangelogEntry(
                version = "0.1.20",
                english =
                    "The app checks updates only when it is opened, the connecting banner no longer flashes, and channels remain visible in the source picker.",
                portuguese =
                    "A app verifica atualizações apenas ao abrir, o aviso de ligação deixou de oscilar e os canais continuam visíveis no seletor de origens.",
            ),
        )

    fun forLanguage(language: String): List<Pair<String, String>> =
        entries.map { entry ->
            entry.version to
                if (language.lowercase().startsWith("pt")) entry.portuguese
                else entry.english
        }
}
