# MLib — proposta de produto, UX e interface

**Data:** 20-11-2026  
**Base:** MLib `993eafb`; MCat Player último baseline auditado `a41cf5c`  
**Natureza:** proposta de produto e design, não compromisso de implementação nem cópia do Spotify.

## 1. Opinião directa

Não faria tudo igual ao MCat Player.

Estimativa:

- **~60% faria igual** — protocolo de pesquisa, seleção, conclusão, encaminhamento, deduplicação de gravações e conceito de playlists/álbuns.
- **~30% faria diferente** — destino/onboarding, deduplicação de álbuns, merge de metadata, jobs, capas e biblioteca.
- **~10% não portaria** — servidor Express, GramJS, localStorage, API MCat, proxy de áudio, PWA e player Web.

O MCat Player é uma boa referência de **fluxo**, não de arquitectura nem de interface. O MLib deve assumir-se como uma **biblioteca musical Telegram com assistente de aquisição**, não como um clone do Spotify.

## 2. O que faria igual

| Opção MCat Player | Decisão | Porquê |
|---|---|---|
| Pesquisa por track/album/playlist/artist | Manter | É o núcleo do produto; já está implementado. |
| Filtros Deezer/SoundCloud | Manter | O filtro tem de ser aplicado no bot, não só na UI. |
| Resultados numerados | Manter | O bot responde assim; parser simples e verificável. |
| Seleção múltipla e “selecionar tudo” | Manter, com pré-visualização | Útil, mas deve mostrar estimativa de faixas/tempo antes de iniciar. |
| Conclusão por contagem, sinal, faixa única e silêncio | Manter | Já está fechado em código/testes; é a semântica correta. |
| Encaminhar mensagens em vez de descarregar/re-enviar | Manter | Preserva identidade Telegram, qualidade e rapidez. |
| Matching de faixas por título/artista normalizados | Manter | É o oráculo do MCat Player; os golden tests já o cobrem. |
| Playlist local com ordem e membros únicos | Manter | Já implementado; validar em live. |
| Álbum como entidade separada | Manter | É essencial para “completar álbum”. |
| Completar álbum | Manter o conceito, mudar a UX | Hoje adiciona automaticamente; deve mostrar o que falta e pedir aprovação. |
| Pesquisa de capas online | Manter | iTunes/Deezer são úteis; falta completar o gestor. |
| Retoma de trabalho interrompido | Manter | O journal atual é uma boa base, mas incompleto por opção. |

## 3. O que faria diferente

| Opção MCat Player | O que mudaria no MLib | Razão |
|---|---|---|
| Wizard que cria um canal privado automaticamente | Criar um setup **opcional e explícito**: escolher ou criar arquivo, marcar como fonte/destino, confirmar | Criar canais automaticamente polui a conta e pode surpreender o utilizador. |
| Destino limitado a “GinjMusic” | Selector de qualquer chat/canal com permissão de publicação, com pesquisa e favoritos | O produto não deve depender de um nome específico. |
| Deduplicação depois de processar | Indexar o destino **antes** de comparar e configurar fontes de dedup | Evita duplicados na primeira aquisição para um destino ainda não indexado. |
| Merge fuzzy e destrutivo de álbuns | Sugestões de merge com pré-visualização, edições/anos visíveis e undo | “Greatest Hits” e “Greatest Hits 2” não são o mesmo álbum. |
| Merge de faixas apenas no servidor | Estúdio de merge no cliente: escolher keeper, comparar qualidade, preservar relações | Não perder favoritos, playlists, álbuns nem ficheiros. |
| Jobs só no estado da UI | Estado persistido por opção/faixa, progresso, retry apenas do que falta e cancelamento | Um restart não deve repetir tudo. |
| “Esconder duplicados” como única resposta | Vista de revisão de duplicados com escolha de representante | Esconder não resolve a biblioteca. |
| Capa genérica Unsplash como capa real | Placeholder explícito + capa personalizada/online/upload | Unsplash não é metadata musical. |
| Select-all sem custo | Mostrar “N faixas, ~X min, Y pedidos” e confirmar | Evita aquisições enormes por engano. |
| Uma única conversa de bot sem fila visível | Fila de aquisição com estados, pausa/cancelar e retry | O bot é sequencial; a UI deve refletir isso. |
| UI Web/PWA | UI Android nativa, gestos, offline e Android Auto | O produto é Android-first. |

## 4. O que não portaria

- Express, rotas `/api/mcat/*`, `/api/albums/*`, `/api/playlists/*`.
- GramJS/TelegramClient, StringSession e autenticação MTProto do servidor.
- localStorage, configuração Cloud do MCat e `mcatApi`.
- Proxy HTTP de áudio e player Web.
- PWA/offline Web e service worker.
- Catálogo demo, playlists canónicas e blacklists específicas sem decisão explícita.
- Dependência de runtime do MCat/MCat Player.

## 5. Proposta de melhoria do produto

### 5.1 Posicionamento

**MLib = a biblioteca da minha música no Telegram, com um assistente que encontra o que falta.**

Três pilares:

1. **Biblioteca** — organizar, reparar, completar e ouvir.
2. **Aquisição** — pesquisar e obter música sem sair da app.
3. **Confiança** — nada é apagado ou substituído sem revisão; tudo é recuperável.

### 5.2 Melhorias nos fluxos já portados

1. **Destino e fontes**
   - Selector de chats/canais com permissão de publicação.
   - Indexação do destino antes da dedup.
   - Fontes de dedup configuráveis por perfil.
   - Estado visível: “destino indexado / a indexar / incompleto”.

2. **Aquisição**
   - Assistente em passos: tipo → fornecedor → pesquisa → resultados → destino → confirmação.
   - Estimativa de faixas/tempo e aviso de colecções grandes.
   - Fila com estado por opção e retry do que falta.
   - Resultado parcial explicado: “18/20 faixas; 2 falharam”.
   - Nunca mostrar um resultado como completo se ficou parcial.

3. **Deduplicação**
   - Matching automático apenas para candidatos claros.
   - Casos ambíguos vão para revisão.
   - Merge de metadata com escolha de keeper e pré-visualização.
   - Operação reversível/undo.

4. **Álbuns**
   - Cartão de álbum com anel de completude (`18/20`).
   - “Faixas em falta” na página do álbum.
   - Completar álbum com lista de existentes, em falta e ambíguas.
   - Sugestões de merge de edições apenas quando o utilizador abre “Resolver duplicados”.

5. **Playlists**
   - Capa: primeira faixa, pesquisa online, upload, presets, URL.
   - Ordem por drag-and-drop; ações em massa.
   - Descrição e capa sincronizadas; placeholder explícito.

6. **Jobs**
   - Journal por opção/faixa, não apenas por título.
   - Retomar sem repetir opções concluídas.
   - Cancelamento local claro; explicar quando o bot continua.
   - Histórico de pedidos com detalhe e repetição.

### 5.3 Novas features

Prioridade **P1**:

1. **Biblioteca saudável** — dashboard com:
   - faixas em falta em álbuns;
   - grupos de duplicados;
   - ficheiros locais inválidos;
   - capas em falta/genéricas;
   - fontes incompletas;
   - conflitos/estado do CloudSync.
2. **Completar álbum assistido** — lista de faltas + aquisição aprovada.
3. **Estúdio de duplicados** — comparar, escolher keeper, fundir metadata, undo.
4. **Pesquisa unificada** — biblioteca + Telegram + bot, com filtros e histórico.

Prioridade **P2**:

5. **Páginas de artista** — discografia, álbuns, faixas, fontes.
6. **Smart downloads** — favoritos/álbuns/playlists automáticos; limites de storage.
7. **Fila e retoma entre dispositivos** — via CloudSync, só metadata/posição.
8. **Widgets e quick settings** — now playing, continuar a ouvir, saúde da biblioteca.
9. **Android Auto melhorado** — Início, Álbuns, Playlists, Favoritos, Offline; sem menus profundos.
10. **Importar/exportar** — M3U/JSON, backup/restore, relatório da biblioteca.
11. **Privacidade** — bloqueio biométrico, fontes ocultas, modo privado.
12. **Scrobbling opcional** — ListenBrainz/Last.fm, desligado por defeito.
13. **Sleep timer, velocidade, equalizador, crossfade/gapless** — conforme suporte Media3.
14. **Pesquisa por voz / comandos** — “adicionar álbum X ao destino Y”.
15. **Smart playlists** — regras (década, género, favoritos, offline, recentes).

Prioridade **P3**:

16. **Partilha rica** — cartão de álbum/playlist com artwork e link Telegram.
17. **Modo família/perfis** — separação de bibliotecas na mesma conta.
18. **Métricas pessoais** — mais ouvidos, tempo, evolução; opt-in.
19. **Temas** — capa/artwork-driven, alto contraste, AMOLED.
20. **Tablet/foldable** — two-pane; desktop fica congelado.

## 6. Proposta de interface — “MLib Signal”

### 6.1 Princípios

1. **Conteúdo primeiro** — capa, título e artista antes de efeitos.
2. **Escuro por defeito, claro completo** — ambos com contraste real.
3. **Uma cor por contexto** — acento derivado da capa; o espectro MLib é assinatura, não arco-íris.
4. **Movimento com propósito** — 180–240 ms; shared element na capa; sem blur gratuito.
5. **Aquisição é primeira classe** — não escondida em Settings.
6. **Nada destrutivo sem pré-visualização.**
7. **Inspirar no Spotify, não copiar** — informação e hábitos, não marca, verde ou layout pixel-perfect.

### 6.2 Sistema visual

**Cores (dark):**

| Token | Hex | Uso |
|---|---|---|
| `bg` | `#0B0D10` | Fundo |
| `surface` | `#14171D` | Cartões/sheets |
| `surfaceElevated` | `#1C2028` | Mini-player, diálogos |
| `text` | `#F4F7F5` | Texto principal |
| `textMuted` | `#A7B0BA` | Secundário |
| `outline` | `#2C333D` | Bordas |
| `accent` | dinâmico da capa | CTA/seleção |
| `spectrum` | 7 cores MLib | Categorias/estados, com moderação |

**Cores (light):** fundo `#F7F8FA`, superfície `#FFFFFF`, texto `#162025`, muted `#5E6A76`, outline `#DDE3E9`.

**Tipografia:** `Manrope`/`Sora` para títulos e `Inter` para corpo (ou system font com escala equivalente). Escala: 32/36 display, 24/28 title, 20/24 section, 16/22 body, 12/16 caption.

**Formas:** raio 12 (inputs), 20 (cards), 28 (sheets/player); capas 16–24; ícones 24 dp; alvos 48 dp.

**Elevação:** sombras suaves; vidro apenas em navegação/mini-player; listas usam superfícies sólidas para performance.

### 6.3 Navegação

Atual: 5 tabs `Music, Favorites, Playlists, Albums, Sources` + Settings em sheet.

Proposta: 4 destinos principais + mini-player persistente.

```
┌─────────────────────────────┐
│  Início   Procurar   Biblioteca   Definições │
└─────────────────────────────┘
        mini-player persistente acima
```

- **Início** — continuar a ouvir, recentes, álbuns por completar, saúde da biblioteca, adquirir.
- **Procurar** — pesquisa unificada (biblioteca/Telegram/bot), filtros, resultados, aquisição.
- **Biblioteca** — chips `Faixas | Álbuns | Playlists | Artistas | Fontes`; favoritos e offline como filtros.
- **Definições** — conta, sync, aparência, armazenamento, updates, privacidade.
- **Adquirir** — botão central/FAB contextual; não enterrado.
- **Tablet** — navigation rail + two-pane.

### 6.4 Ecrãs

#### Início

```
Bom dia, Miguel                         [search] [avatar]
────────────────────────────────────────────────────────
[ Continuar a ouvir — capa grande, progresso, play ]
────────────────────────────────────────────────────────
Álbuns por completar            Ver todos
[ capa 18/20 ] [ capa 09/12 ] [ capa 04/04 ]
────────────────────────────────────────────────────────
Adicionados recentemente
[ capa ] [ capa ] [ capa ] [ capa ]
────────────────────────────────────────────────────────
A tua biblioteca
18 faixas em falta · 6 duplicados · 3 capas em falta
[ Rever ]
```

#### Procurar

- Campo com scope `Biblioteca | Telegram | Bot`.
- Chips de tipo e fornecedor.
- Resultados com checkbox; barra de seleção fixa: `3 selecionadas · ~18 faixas · ~12 min`.
- Destino no topo, com estado `indexado`.
- Botão `Adquirir` com confirmação e estimativa.
- Histórico de pesquisas e pedidos recentes.

#### Biblioteca

- Chips de tipo e filtros (offline, favoritos, duplicados, incompletos).
- Grid/list toggle; ordenação; seleção múltipla.
- Cartões de álbum com anel de completude.
- Playlists com colagem 2x2; artistas com foto; fontes com estado.

#### Álbum

- Hero com capa, gradiente da capa, título/artista/ano.
- `Reproduzir`, `Aleatório`, `Download`, overflow.
- Barra `18/20 faixas` + `Completar álbum`.
- Lista de faixas: número, título, duração, qualidade/offline, favorito.
- Secção “Em falta” com CTA para adquirir.
- “Resolver duplicados” se aplicável.

#### Player

- Capa grande, fundo gradiente derivado da capa.
- Título/artista, favorito, fonte.
- Seek, tempos, controlos grandes.
- Sheets: fila, velocidade, sleep timer, fonte, letra (quando existir).
- Mini-player com progresso, play/pause, next; swipe para expandir.

#### Aquisição

Passos visíveis:

1. Tipo (`track/album/playlist/artist`)
2. Fornecedor (`Deezer/SoundCloud`)
3. Pesquisa
4. Resultados (seleção múltipla)
5. Destino (chat/canal, indexação)
6. Confirmação (estimativa)
7. Progresso por opção
8. Resultado (completo/parcial/falhado) + retry

Nunca voltar a esconder o estado num texto único.

#### Definições

Secções colapsáveis:

- Conta Telegram
- Biblioteca e fontes
- Sincronização e chave de recuperação
- Aparência e idioma
- Armazenamento e offline
- Atualizações
- Privacidade e segurança
- Acerca/licenças

### 6.5 Identidade MLib

- Marca `M`/waveform MLib; wordmark curto.
- Espectro de 7 cores como assinatura, usado em categorias/estados e não em todos os botões.
- Capas reais mandam na cor; o espectro aparece quando não há capa.
- Não usar verde Spotify, tipografia Spotify ou componentes copiados.

### 6.6 Acessibilidade e performance

- Contraste mínimo 4.5:1; nunca cor como único indicador.
- Dynamic type, TalkBack, descrições em todas as capas/controles.
- Alvos ≥48 dp; navegação por teclado/switch.
- Vidro/blur limitado; testar jank em dispositivos médios.
- Listas grandes com chaves estáveis, paginação e imagens com tamanho adequado.
- Estado offline/erro sempre visível, sem depender de snackbar efémero.

## 7. Roadmap sugerido

### Fase 0 — antes de v0.1.10

- Testar actualização in-app.
- Substituir screenshots antigas (`docs/screenshots` ainda mostram Spotygram/russo).
- Corrigir `AcquisitionSheet.kt:355` (`"Estado: …"` hardcoded).
- Validar live uma faixa e uma playlist pequena.

### Fase 1 — 0.2

- Navegação de 4 destinos + mini-player.
- Início com “Continuar a ouvir”, recentes e álbuns por completar.
- Página de álbum com anel de completude e faixas em falta.
- SRC-01 (destino e indexação).

### Fase 2 — 0.3

- Estúdio de duplicados/merge (DED-02/DUP-01).
- Completar álbum com pré-visualização (ALB-03).
- Biblioteca saudável.
- Gestor de capas (COV-01/PL-03).

### Fase 3 — 0.4

- Setup do bot/arquivo (SET-01).
- Enriquecimento de álbuns (ALB-01).
- Jobs por opção (JOB-01/02).
- Widgets/Android Auto/tablet.

### Fase 4 — futuro

- Scrobbling, smart playlists, import/export, perfis, temas.

## 8. Métricas de sucesso

- Tempo até adquirir uma faixa/álbum.
- Taxa de duplicados após aquisição.
- Percentagem de álbuns completos.
- Sucesso de retry/recuperação.
- Crashes e ANRs.
- Jank/startup em dispositivos médios.
- Acessibilidade: TalkBack, contraste, dynamic type.
- Percepção: “sei o que a app está a fazer” e “não perdi nada”.

## 9. Riscos e guardrails

- **Não copiar o Spotify** visual ou legalmente; inspiração de informação e hábitos.
- **Não automatizar destruição** de álbuns/duplicados.
- **Não esconder o estado do bot**; aquisição é assíncrona e falível.
- **Não prometer paridade MCat Player** sem validação live.
- **Não sobrecarregar a UI com vidro**; performance e legibilidade primeiro.
- **Não misturar** canal CloudSync, arquivo musical e conversa do bot.

## 10. Decisão recomendada

1. Lançar `v0.1.10` para testar actualização e aquisição real.
2. Fechar SRC-01, DED-02/DUP-01 e ALB-03 antes de chamar o MLib “paridade MCat Player”.
3. Redesenhar a navegação e o player na fase 0.2, mantendo a identidade MLib e inspiração Spotify apenas na estrutura.
4. Medir tudo com telemetria local/opt-in; sem SDK de analytics.
