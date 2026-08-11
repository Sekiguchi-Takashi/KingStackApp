# KING STACK — ゲーム仕様書

## 1. 概要

スパイダーソリティアをベースにしたスマートフォン向けカードパズルゲーム。

プレイヤーは複数の縦列にカードを積み、A〜Kの13枚を完成させて退避させる。
初期盤面は5列で、キングの出現条件を満たすと6列目、さらに7列目が開放される。

ゲームの特徴は以下。

- 初期5列・各列1枚から開始
- 「1つ大きい数字」のカードへ積む
- 同一スートの連続列はまとめて移動可能
- A〜K完成で列を完成エリアへ退避
- キングによって最大7列まで盤面拡張
- 合法手がなくなったら全開放列へ1枚ずつ配札
- 配札直後、カードを動かす前なら1回だけリドロー可能
- AIがヒント、盤面評価、配札バランス調整を担当
- 4セット完成でクリア

---

# 2. オントロジー

```text
Game
├── CardDeck
│   ├── Card
│   ├── Suit
│   └── Rank
├── Board
│   ├── StackSlot × 5〜7
│   ├── CompletedArea
│   └── EmptySlot
├── DrawSystem
│   ├── Draw
│   └── Redraw
├── PlayerAction
│   ├── MoveCard
│   ├── MoveStack
│   ├── Undo
│   └── Hint
├── Rule
│   ├── PlacementRule
│   ├── CompletionRule
│   ├── KingExpansionRule
│   ├── DrawRule
│   └── RedrawRule
├── AI
│   ├── BoardAnalyzer
│   ├── HintAI
│   ├── DrawAI
│   └── DifficultyAI
└── Result
    ├── Clear
    └── GameOver
```

---

# 3. カード仕様

## Rank

| 表記 | 数値 |
|---|---:|
| A | 1 |
| 2 | 2 |
| 3 | 3 |
| 4 | 4 |
| 5 | 5 |
| 6 | 6 |
| 7 | 7 |
| 8 | 8 |
| 9 | 9 |
| 10 | 10 |
| J | 11 |
| Q | 12 |
| K | 13 |

Suit:

- ♠
- ♥
- ♦
- ♣

52枚を使用する。

---

# 4. 盤面仕様

初期状態は5列。

```text
① ② ③ ④ ⑤
```

6列目、7列目はロック状態。

キングの条件を満たすと順次開放する。

```text
5 → 6 → 7
```

最大7列。

各列は0枚以上のカードを保持する。

---

# 5. カード配置ルール

基本ルール:

> カードは、数字が1つ大きいカードの上に置く。

例:

```text
7
└ 6
   └ 5
      └ 4
         └ 3
            └ 2
               └ A
```

空列には任意のカードまたは合法なスタックを置ける。

---

# 6. スタック移動

以下の条件をすべて満たす連続列は一括移動可能。

- 数字が連続している
- 同一スート
- 表向き
- 正しい順序で積まれている

例:

```text
♠9
♠8
♠7
♠6
```

は一括移動可能。

異なるスートが混在する連続列は、原則として1枚ずつ移動する。

---

# 7. 完成ルール

A〜Kの13枚が正しい順序で完成したら、完成列として盤面から退避する。

```text
K
Q
J
10
...
2
A
```

52枚なので、完成セットは最大4セット。

```text
CompletedCount == 4
```

でCLEAR。

---

# 8. キングによる列開放

キングは通常の13として扱うほか、盤面拡張キーとして機能する。

## 1回目

条件を満たしたキングが先頭に来た場合:

```text
5列 → 6列
```

## 2回目

別のキングが同条件を満たした場合:

```text
6列 → 7列
```

7列目開放後は追加開放なし。

---

# 9. 配札ルール

合法手が存在しない場合、「カードを配る」を使用できる。

現在の開放列数と同じ枚数を1枚ずつ配る。

```text
5列 → 5枚
6列 → 6枚
7列 → 7枚
```

配札は全開放列へ1枚ずつ行う。

---

# 10. リドロールール

配札直後で、まだカードを1枚も移動していない場合のみ1回だけリドロー可能。

状態:

```text
Draw
 ↓
RedrawAvailable = true
 ↓
カードを動かす
 ↓
RedrawAvailable = false
```

または:

```text
Draw
 ↓
Redraw
 ↓
RedrawAvailable = false
 ↓
カード操作
```

リドロー後の再リドローは禁止。

---

# 11. デッドロック

以下をすべて満たす場合、デッドロック。

- 合法なカード移動がない
- 利用可能な配札がある場合は配札へ移行
- 残りデッキがなく、完成していないカードが残っている場合はGAME OVER

---

# 12. GameState

```text
GameState
├── cards
├── slots
├── activeSlotCount
├── drawPile
├── completedStacks
├── score
├── combo
├── moveCount
├── drawCount
├── redrawAvailable
├── hasMovedAfterDraw
├── kingCount
├── difficulty
└── gameStatus
```

gameStatus:

- PLAYING
- PAUSED
- CLEAR
- GAME_OVER

---

# 13. 機能設計

## ゲーム管理

- 新規ゲーム
- ゲーム開始
- ゲーム中断
- ゲーム再開
- ゲーム保存
- ゲーム終了
- CLEAR判定
- GAME OVER判定

## カード操作

- カード選択
- ドラッグ＆ドロップ
- タップ移動
- 移動可能先表示
- 1枚移動
- スタック移動
- 空列への移動

## 盤面

- 5列生成
- 6列目開放
- 7列目開放
- 列のカード管理
- 空列管理
- 完成列管理

## 配札

- 配札
- 配札アニメーション
- 配札回数管理
- リドロー
- リドロー状態管理

## 補助

- Undo
- Hint
- サウンド
- 振動
- オートセーブ

---

# 14. 画面設計

## S01 ホーム

```text
KING STACK

[ PLAY GAME ]

[ DAILY CHALLENGE ]

[ 戦績 ] [ ルール ]

[ 設定 ]
```

## S02 難易度

- EASY
- NORMAL
- HARD
- EXPERT

## S03 ゲーム

```text
SCORE              MENU

完成:
○ ○ ○ ○

列:
● ● ● ● ● ○ ○

①  ②  ③  ④  ⑤  ⑥  ⑦
[ ] [ ] [ ] [ ] [ ] ...

CHAIN × 4

[ Undo ] [ Hint ]

[ カードを配る ]
```

## S04 ポーズ

- 続ける
- リスタート
- ルール
- ホーム

## S05 ヒント

推奨移動と簡潔な理由を表示。

## S06 CLEAR

- スコア
- 完成数
- 移動数
- 配札数
- リドロー数
- 最大CHAIN
- もう一度
- ホーム

## S07 GAME OVER

- スコア
- 残りカード
- リトライ
- ホーム

## S08 戦績

- プレイ回数
- クリア回数
- クリア率
- BEST SCORE
- BEST CHAIN
- 最少配札
- 最少リドロー

## S09 ルール

チュートリアル形式で基本ルールを説明。

## S10 設定

- サウンド
- 振動
- アニメーション
- カードデザイン
- 左利きモード
- データリセット

---

# 15. AI推論ルール

AIは「答えを出すAI」ではなく「ゲームマスターAI」として設計する。

```text
Player
 ↓
カード操作
 ↓
GameState更新
 ↓
AI Board Analysis
 ↓
次の盤面・配札を評価
 ↓
Playerが再判断
```

---

# 16. AIの基本評価関数

概念:

```text
MoveValue =
    CompletionValue
  + EmptySlotValue
  + StackValue
  + KingUnlockValue
  + FutureMoveValue
  - DeadlockRisk
```

開始時の仮重み:

```text
Completion       +1000
KingUnlock       +300
EmptySlot        +150
StackLength      +20 × 長さ
FutureMoves      +10 × 手数
ChainPotential   +5
DeadlockRisk     -300
StackBreak       -100
```

実際のゲームテストで調整する。

---

# 17. AIが評価する特徴量

- 合法手数
- 完成までの距離
- A〜K完成可能性
- 同一スート連続列
- スタック長
- 空列数
- キングによる列開放可能性
- 孤立カード数
- 次の配札での選択肢
- デッドロック確率
- コンボ継続可能性

---

# 18. ヒントAI

```text
FindLegalMoves()
 ↓
EvaluateMoves()
 ↓
数手先まで探索
 ↓
BestMove
 ↓
プレイヤーへ提示
```

優先順位:

1. 完成につながる手
2. キングによる列開放
3. 空列を作る手
4. スタックを作る手
5. 合法手を増やす手
6. デッドロック回避

---

# 19. 先読み

MVPでは1手先。

完成版では3〜5手先を推奨。

探索方法:

```text
Beam Search
```

を基本候補とする。

全探索による組み合わせ爆発を避けるため、評価上位候補だけを次段へ進める。

---

# 20. 配札AI

完全ランダムではなく、

```text
ランダム候補生成
 ↓
各候補を仮配置
 ↓
合法手分析
 ↓
完成可能性評価
 ↓
デッドロック評価
 ↓
重み付きランダム選択
```

とする。

配札評価:

```text
DrawScore =
    ImmediateMoves
  + StackCreation
  + CompletionPotential
  + KingPotential
  + EmptySlotUtilization
  - DeadlockRisk
```

---

# 21. 重み付きランダム

最良候補を常に選ばない。

例:

```text
候補A 35%
候補B 30%
候補C 20%
候補D 10%
候補E  5%
```

のように、有利な候補ほど出やすくする。

これにより「AIに答えを操作されている」感覚を抑える。

---

# 22. 難易度別AI補正

| 難易度 | AI補正 |
|---|---:|
| EASY | 90% |
| NORMAL | 65% |
| HARD | 35% |
| EXPERT | 10% |

AI補正とは、配札候補の中から有利な候補を選択する確率。

---

# 23. プレイヤースキル推定

以下を記録。

```text
PlayerModel
├── ClearRate
├── AverageScore
├── AverageMoves
├── AverageDraw
├── AverageRedraw
├── HintUsage
├── UndoUsage
└── RecentPerformance
```

直近の成績から内部的なSkillLevelを推定する。

---

# 24. 動的難易度

連勝・高スコアが続く場合:

```text
配札AI補正を少し下げる
```

連敗が続く場合:

```text
配札AI補正を少し上げる
```

ただし、必ずクリアできる状態にはしない。

プレイヤーには動的調整を原則として表示しない。

---

# 25. バランス目標

初期目標値:

| 難易度 | クリア率目標 |
|---|---:|
| EASY | 80〜95% |
| NORMAL | 55〜75% |
| HARD | 30〜55% |
| EXPERT | 10〜30% |

実際のプレイデータを収集し、配札AIの重みを調整する。

---

# 26. スコア

基本スコア例:

| 行動 | スコア |
|---|---:|
| カード移動 | +10 |
| 連続移動 | +20 |
| A〜K完成 | +1,000 |
| キング列開放 | +300 |
| 5CHAIN | +100 |
| 配札 | -50 |
| リドロー | -100 |
| ヒント | -50 |

Undoについては、通常プレイでは使用可能とし、ランキングでは使用回数を別記録する方式を推奨。

---

# 27. コンボ

連続して有効な操作をするとCHAINを増加。

```text
CHAIN × 2
CHAIN × 3
CHAIN × 4
...
```

完成列を作った場合は大きなCHAIN BONUSを付与する。

---

# 28. 保存データ

```text
GameSave
├── gameId
├── timestamp
├── board
├── deck
├── completed
├── activeSlots
├── score
├── combo
├── moveCount
├── drawCount
├── redrawState
└── difficulty
```

アプリ終了時にも現在ゲームを復元可能にする。

---

# 29. 開発フェーズ

## Phase 1 — MVP

- 52枚
- 5列
- カード移動
- スタック
- A〜K完成
- 完成列退避
- 配札
- リドロー
- キングによる6・7列開放
- CLEAR
- GAME OVER

## Phase 2 — ゲーム性

- Undo
- Hint
- スコア
- CHAIN
- セーブ
- アニメーション
- サウンド
- 振動

## Phase 3 — AI

- 盤面評価
- ヒントAI
- 配札AI
- デッドロック予測
- 難易度補正
- プレイヤーSkill推定

## Phase 4 — 継続プレイ

- Daily Challenge
- ランキング
- プレイ統計
- 実績
- カスタムカード
- 追加ゲームモード

---

# 30. 実装上の基本アーキテクチャ

```text
UI Layer
   ↓
Game Controller
   ↓
GameState
   ↓
┌─────────────────────────┐
│ CardManager              │
│ BoardManager             │
│ MoveManager              │
│ StackManager             │
│ CompletionManager        │
│ KingManager              │
│ DrawManager              │
│ RedrawManager            │
│ ScoreManager             │
│ ComboManager             │
└────────────┬────────────┘
             ↓
          AI Engine
             ↓
┌─────────────────────────┐
│ BoardAnalyzer            │
│ HintAI                   │
│ DrawAI                   │
│ DifficultyAI             │
└─────────────────────────┘
```

**重要:** ルールをUI側に直接書かず、`GameState` と各Managerに分離する。これにより、後から「キングの条件」「列数」「配札枚数」「リドロー回数」などを変更してもゲーム画面を作り直さずに済む。

---

# 31. 将来拡張

この設計から以下を追加できる。

- ジョーカーカード
- 特殊カード
- 8列以上の特殊ステージ
- 時間制限モード
- 1枚制限モード
- デイリーチャレンジ
- ランキング
- スキン
- 実績
- ミッション
- AI対戦的なチャレンジ
- ストーリーモード

基本ゲームエンジンは共通化する。

---

# 32. 設計上の最重要原則

このゲームでは、

> **「カードをどう動かすか」だけでなく「次にどんなカードが来ると困るか」を考えさせる**

ことをゲームデザインの中心とする。

したがってAIも、

```text
良い手を出す
```

だけではなく、

```text
プレイヤーが考える余地を残した
適度に有利・不利な盤面を生成する
```

ことを目的とする。

これにより、単純なスパイダーソリティアの派生ではなく、

**「盤面整理 → キングによる拡張 → 配札 → リドロー → 再整理」**

という独自のゲームループを成立させる。
