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
