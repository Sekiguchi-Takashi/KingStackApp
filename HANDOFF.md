# HANDOFF — KingStackApp

## 現在地

v1.0。仕様書のPhase 1〜3を実装済み。Phase 4はDaily Challengeのみ着手。

## 未実装 / 次にやること

- ドラッグ＆ドロップ移動（現状はタップ選択→タップ移動のみ）
- 配札・完成時のカードアニメーション（現状は選択時のリフトのみ）
- ランキング、実績、ミッション、カスタムカード
- ジョーカー・特殊カード・8列以上ステージ
- 実プレイデータによる `BoardAnalyzer` の重み調整（現状は仕様書の仮重みのまま）

## 触る場所の目安

| やりたいこと | ファイル |
|---|---|
| 列数・完成枚数・キング開放回数 | `core/Model.kt` の定数 |
| 置ける条件・一括移動条件・完成条件 | `core/Rules.kt` |
| スコア配分・配札処理・勝敗判定 | `core/Engine.kt` |
| AIの強さ・評価重み | `ai/BoardAnalyzer.kt`, `ai/DrawAI.kt` |
| 難易度カーブ | `core/Model.kt` の `Difficulty`, `ai/DrawAI.kt` の `DifficultyAI` |
| 盤面の見た目・カード寸法 | `ui/BoardView.kt` |

## 注意

- `Engine.refresh()` は盤面変化後に必ず通す。完成退避・キング開放・勝敗判定がここに集約されている。
- `GameState` は不変。Undoはスナップショットのスタック（最大60）。
- セーブはカードIDのみをSharedPreferencesにJSON保存。`Card(id)` から suit/rank を復元する設計。
