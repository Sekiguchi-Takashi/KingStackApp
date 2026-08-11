# KING STACK

スパイダーソリティア派生のカードパズル。仕様書 `king_stack_game_spec.md` に基づくAndroid実装。

## 実装済み（Phase 1 + 2 + 3）

- 52枚 / 初期5列 / 1つ大きい数字の上に置くルール
- 同一スート連番のスタック一括移動
- A〜K完成で列を退避、4セットでCLEAR
- キングが列の先頭に来ると6列目・7列目を開放
- 配札（開放列数ぶんを1枚ずつ）とリドロー（配札直後・未移動時に1回）
- デッドロック判定 / GAME OVER
- Undo / Hint / スコア / CHAIN / オートセーブ
- 盤面評価AI・ヒントAI（ビーム探索 深さ2）・配札AI（候補評価＋重み付きランダム）
- 難易度別AI補正と動的難易度（直近10戦の成績で±0.10）
- 戦績 / 設定 / ルール / Daily Challenge（日付シード）

## 仕様書からの判断ポイント

| 項目 | 仕様書の記述 | 実装での解釈 |
|---|---|---|
| キング開放条件 | 「条件を満たしたキングが先頭に来た場合」 | v1.1: 列の先頭（最下段）に来たとき、または列の一番上に露出したとき。同じキングは1回だけ、最大2回 |
| 完成条件 | 「A〜Kの13枚が正しい順序で」 | スート不問。同一スート限定にする場合は `Rules.isCompleted` を1行変更 |
| 配札条件 | 「合法手が存在しない場合」 | 既定は仕様書どおりの厳格モード（合法手がないときだけ配れる）。設定の「配札は手詰まり時のみ」をOFFにするといつでも配札可 |

## 構造

```
core/   Model, Rules, Engine, GameController   ルールとゲーム進行（UI非依存）
ai/     BoardAnalyzer, HintAI, DrawAI, DifficultyAI
data/   SaveStore, Feedback
ui/     Theme, BoardView, GameScreen, Screens
```

ルールはUIに書かず `Rules` と `Engine` に集約。列数・配札枚数・キング条件・スコア重みは
`core/Model.kt` の定数、`core/Engine.kt` のスコア定数、`ai/BoardAnalyzer.kt` の重みだけで調整できる。

## ビルド

main へのpushでGitHub Actionsがdebug APKをビルドし、Releaseに添付する。

## 署名

`app/debug.keystore` を固定鍵としてリポジトリに含めている。これによりビルドのたびに署名が変わらず、
新しいAPKをそのまま上書きインストールできる（アンインストール不要）。
v1.0 を入れている場合のみ、一度アンインストールしてから v1.1 を入れること。
