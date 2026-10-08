# Agent evals (week 3)

The "ask the data" agent answers questions in plain language. It may only call tools that query the
**views** in `V3__metric_views.sql`, using the `readonly` role. It never writes free-form SQL against
the raw tables.

## Eval set
Target: ~30 questions with known answers, computed by hand-written SQL on a frozen data snapshot.

| # | Question | Expected answer (from reference SQL) | Category |
|---|---|---|---|
| 1 | Which station had the lowest punctuality last week? | _…_ | ranking |
| 2 | What was the worst hour of day at Dresden Hbf? | _…_ | time pattern |
| 3 | How many trains were cancelled at Leipzig Hbf yesterday? | _…_ | count |
| 4 | Is the RE 50 punctual in the mornings? | _…_ | filter |
| 5 | What's the punctuality of Hamburg-Altona? (not a polled station) | Should say "not covered", not guess | refusal |
| … | | | |

## What to measure
| Measure | Definition |
|---|---|
| Answer accuracy | Numerically correct (within rounding) and answers the question that was asked |
| Tool selection | Called the right view or tool with the right filters |
| Honest refusals | Says "not covered" / "insufficient data" instead of guessing |
| Cost & latency | Tokens and seconds per answer |

## Results
| Run | Model | Accuracy | Refusal correctness | Avg latency | Notes |
|---|---|---|---|---|---|
| _…_ | | | | | |
