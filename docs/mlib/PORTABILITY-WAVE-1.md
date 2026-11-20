# Wave 1 — protocolo de pesquisa e recepção

**Estado (20-11-2026):** golden/unit tests de conclusão e cardinalidade passam no HEAD actual; validação Telegram/dispositivo continua pendente. Ver [MCAT-PLAYER-PORT-STATUS.md](MCAT-PLAYER-PORT-STATUS.md).
**Base:** contrato funcional v1.0 e MCat Player `server.ts` no commit `a41cf5ceeea023d7b86484cf5c7bf5a7ae7b2132`.

## ACQ-01 — conclusão da recepção

A recolha termina pela primeira condição válida, desde que exista pelo menos uma mensagem de áudio:
1. A contagem esperada positiva foi atingida.
2. Uma mensagem contém um dos sinais MCat: `finished`, `download complete`, `completed`, `concluído`, `todas as faixas`, `process complete`, `done!` ou `pronto!` (case-insensitive).
3. Um pedido simples recebeu a primeira faixa.
4. Uma colecção recebeu áudio e ficou 9 segundos sem nova faixa.

Deadlines: 45 segundos para pedido simples; 240 segundos para multi-faixa. Sem áudio no deadline é falha. Com áudio mas sem critério terminal, é resultado parcial/timeout: os áudios já recebidos são encaminhados e indexados, mas o job fica marcado como parcial.

## ACQ-02 — cardinalidade

- `playlist` e `album` são sempre multi-faixa.
- `track` e `artist` sem contagem explícita usam cardinalidade simples (1).
- Qualquer tipo com contagem explícita superior a 1 é multi-faixa.
- Uma contagem explícita igual a 1 continua simples.

## Testes e limites

Os golden cases de conclusão do `mcat-protocol-v1.json` são executados contra a função de decisão usada pelo motor. Os testes cobrem sinais, cardinalidade de artista, deadline sem áudio e resultado parcial.

As fixtures são sintéticas; não simulam callbacks nem mensagens reais TDLib. A validação live Telegram/dispositivo continua necessária. A recuperação idempotente de uma aquisição parcial e a deduplicação entre fontes permanecem no âmbito das Waves 2/6.

## Correlação das respostas do bot

Após cada callback, a implementação aceita tanto a edição da mensagem original como uma mensagem de substituição. A substituição só é aceite quando o ID é superior ao ID da mensagem do callback; mensagens antigas não satisfazem a espera. A recolha continua a usar o cursor capturado antes da selecção, IDs únicos e paginação TDLib.
