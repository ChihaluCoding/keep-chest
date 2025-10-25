# AGENTS.md

## Keep Chest

## 概要

**Keep Chest（キープチェスト）** は、プレイヤーがスニーク（Shift）しながらチェストを破壊した場合に、そのチェストを「中身ごと保持したアイテム（Packed Chest）」として取得できるようにする Minecraft 用の モッド機能です。サバイバルの流れを壊さないよう保護連携・制限（コスト／クールダウン／ブラックリスト）を備え、シングル／マルチどちらでも安全に動作することを目的とします。

---

## 目的（Why）

* 拠点移転や倉庫整理の手間を減らすことで、プレイヤーの作業テンポを向上させる。
* サーバー運営者・管理者が荒らし対策やバランス調整をしやすいよう、設定で細かく制御できること。
* 既存のゲーム性（チェストの保護・ホッパー接続等）を破壊しない堅実な実装。

---

## ユーザー向け動作（High-level）

* プレイヤーが **Shift 押しながらチェスト破壊** → 通常の中身散乱を行わず、`packed_chest` アイテムが生成される。
* `packed_chest` を設置するとチェストが復元され、中身も復帰する。
* 通常破壊（Shift なし）は従来どおり（中身散乱）を維持。
* ダブルチェストは片側をShift破壊すると**両側まとめて**PackedChestに保存（設定で切替可）。

---

## 主要機能一覧

1. Shift+破壊 → PackedChest 作成（NBTに中身を保存）
2. 置くと復元（BlockEntityTag を復元）
3. ダブルチェスト同時保存（設定で ON/OFF）
4. 保護プラグイン連携（WorldGuard / GriefPrevention 等へのフック）
5. ホッパー / 自動搬送連動時の安全チェック
6. ブラックリスト（特定アイテムを含む場合は pack 禁止）
7. コスト・クールダウン設定（XP／指定アイテム／ツール耐久 等）
8. オプションで所有者ロック（他人が設置できない）
9. イベント API（Pack / Unpack を外部でキャンセル/監視可能）
10. NBT サイズ上限チェック（爆発的データ肥大を防止）

---

## 挙動詳細（フロー）

### A. チェスト破壊時（Shift）

1. サーバー側で `BlockBreakEvent` を受け取る。
2. 条件チェック（下記）を行う：

   * `player.isSneaking()` が true
   * `enableShiftPacking` が true
   * チェストを別プレイヤーが開いていない
   * チェストが保護されていない（保護プラグイン連携）
   * ホッパー等で現在アイテム搬送中でない（設定で緩和可）
   * 中身に `blacklistedItems` が含まれていない
   * NBT サイズが `maxPackedNbtSize` 以下である
3. 条件を満たしたら、BlockEntity の Inventory を読み出し、PackedChest アイテム（`ItemStack`）を生成。NBT に `BlockEntityTag`（Items, ChestType, CustomName, Owner, PackedAt）を埋める。
4. Double chest 対応：接続相方の BlockEntity も読み取り、1 つにマージして保存（設定 `allowDoubleChestPack` による）。
5. PackedChest をプレイヤーインベントリへ追加、空きが無ければ地面へドロップ。
6. チェストブロック（両側）が安全に削除される（破壊サウンドは出すがアイテム散乱はキャンセル）。
7. ログ/イベントを発火：`KeepChestPackEvent` を発行（Cancelable）。

### B. PackedChest の設置（Unpack）

1. `ItemUse`（右クリック設置）で PackedChest を設置する際、NBT の `BlockEntityTag` を読み込み BlockEntity を再構築。
2. ChestType（single/left/right/trapped）を考慮して適切なブロックを設置。
3. 近隣の BlockState との整合性を確認（例えば既にチェストがあって double になってしまう場合の対処）。
4. イベント `KeepChestUnpackEvent` を発行（Cancelable）。
5. 設置成功後は PackedChest アイテムを消費（通常の ItemStack 操作）。

---

## 条件・チェック（実装上の必須）

* **保護連携**: `respectProtectionPlugins` が有効なら、WorldGuard / GP などの API を呼び、権限があるか確認。
* **開放中チェック**: 同時編集や開いているプレイヤーがいる場合は pack を拒否するか待機させる。
* **接続装置チェック**: ホッパー/ドロッパー等が接続されている場合、設定で pack を拒否または警告あり。
* **NBT サイズ上限**: ネットワーク負荷を避けるため `maxPackedNbtSize` を超えたら pack を拒否してエラーメッセージ。
* **ブラックリスト**: modded special items（エンティティコンポーネントを含む等）を config で除外可能。
* **同時操作ロック**: ブロックごとの mutex ロックを設け、複数pack操作で複製が出ないようにする。

---

## コンフィグ（推奨キーとデフォルト例）

```json
{
  "enableShiftPacking": true,
  "requireSneak": true,
  "allowDoubleChestPack": true,
  "disallowEnderChestPacking": true,
  "packCostMode": "none", // none | xp | item | tooldamage
  "packCostItem": { "item": "minecraft:ender_pearl", "count": 1 },
  "packCooldownTicks": 20,
  "respectProtectionPlugins": true,
  "blacklistedItems": [],
  "packedItemStackable": false,
  "allowPackSteal": false,
  "logPackingOperations": false,
  "maxPackedNbtSize": 65536
}
```

---

## データ構造（NBT / アイテム定義）

* **Item ID**: `keep_chest:packed_chest`（modId=`keep_chest` 推奨）
* **NBT (Item)**:

  * `BlockEntityTag`（Vanilla Shulker 同様の形式）

    * `Items`: List of compound { Slot, id, Count, tag? }
    * `ChestType`: "single" | "left" | "right" | "trapped"
    * `CustomName`: string (optional)
    * `Lock`: (optional)
    * `OwnerUUID`: string (optional; 所有者ロック用)
    * `PackedAt`: long (world time)
    * `KeepChestMeta`（optional）:
    * `PackedVersion`: int (将来の互換性)
    * `PackedSize`: int (bytes) — 便利ログ用

---

## API / イベント（外部フック）

* `KeepChestPackEvent` (server-side)

  * Fields: `Player`, `BlockPos`, `PackedItemStack`, `Cause`
  * Cancelable: true
* `KeepChestUnpackEvent`

  * Fields: `Player`, `BlockPos`, `PackedItemStack`
  * Cancelable: true
* Protection hook interface: `IProtectionChecker` を用意（実装は WorldGuardHook, GPHook）

  * メソッド: `boolean canPack(Player, BlockPos)`, `boolean canUnpack(Player, BlockPos)`
* 設定リスナー: config 変更時にキャッシュを更新するコールバック

---

## UI / メッセージ

* 操作ツールチップ（アイテム）:

  * `Shift + 破壊でチェストを詰めて回収します。`
  * `保護されている場合は無効。`
* アクションメッセージ例:

  * 成功: `Packed Chest を取得しました。` / `Chest packed: <name>`
  * 失敗: `このチェストは保護されています。` / `Packing failed: protected` / `Packing failed: contains restricted items` / `Packing failed: NBT size limit exceeded`
* オプションでログ（admin 用）

---

## 互換性

* Ender Chest: デフォルトで除外（設定で許可可能だが安全上推奨しない）
* Modded Chest / Storage: `IInventory` / `BlockEntity` 実装を検出して互換リストを作る。config でホワイトリスト/ブラックリスト運用。
* ShulkerBox と同様のNBT構造を用いるため、Vanilla のツールやバックアップと概ね整合。
* サーバープラグイン（保護系）対応はプラグイン API を使う hook 実装で拡張可能。

---

## セキュリティ / サーバー運用上の注意

* `allowPackSteal = false`（デフォルト）だと pack はチェストの所有者か権限者のみ可能。
* `packCostMode` でコストを必須化すれば無差別移動の抑止になる。
* 大量データ保存（巨大NBT）を許すと同期遅延やクラッシュを招くため `maxPackedNbtSize` を厳密に設定推奨。
* ログ（`logPackingOperations`）を有効にしておくと不正利用の追跡がしやすい。

---

## テストケース（必ず自動＋手動で通す）

### 単体

1. 単一チェスト pack → unpack（内容一致）
2. トラップチェスト pack → unpack（トラップ機能の復元）
3. ダブルチェスト（左右結合） pack → unpack（両側復元）
4. 空チェスト pack → unpack
5. カスタム名付きチェスト pack → name 維持

### 境界／異常

6. 大量アイテム（NBT が大きい） pack は拒否されるか圧縮失敗するか
7. ブラックリストアイテム含有 → pack 拒否
8. ホッパー稼働中 pack → pack 拒否（設定次第）
9. 他プレイヤー同時 pack 試行 → 複製が発生しない（ロック有効）
10. 保護プラグイン連携で pack 禁止の検証

### マルチプレイヤー

11. サーバー再起動後の PackedChest の保持確認（保存永続性）
12. ネットワーク遅延時の race condition テスト（複数クライアント同時操作）

---

## アセット一覧（最低限）

* `assets/keep_chest/lang/en_us.json`（ローカライズ）
* `assets/keep_chest/lang/ja_jp.json`
* `assets/keep_chest/textures/item/packed_chest.png`（アイコン）
* `assets/keep_chest/textures/gui/tooltip.png`（必要なら）
* `mod.toml`（modId=keep_chest 推奨）

---

## 実装ノート（優先度付き）

1. 最小実装プロトタイプ（MVP）

   * Single chest の pack/unpack（保護／コスト無し）
   * Item NBT に BlockEntityTag を保存・復元するだけの流れ
   * Basic UI ツールチップ
2. 拡張（段階2）

   * Double chest 対応
   * Blacklist / NBT-size-check
   * pack/unpack イベント API
3. 運用向け（段階3）

   * 保護プラグインフック実装（WorldGuard, GriefPrevention）
   * packCostMode の導入（item/xp/tooldamage）
   * 設定のホットリロードと管理 UI（デバッグ）
4. 仕上げ（段階4）

   * ドキュメント整備・ローカライズ全言語
   * 自動テスト & CI（NBT 境界・競合テスト）
   * パフォーマンスチューニング（NBT圧縮やストリーム処理）

---

## 既知の制約 / 注意点

* 非 Vanilla の BlockEntity の完璧な互換性は保証できない。対応は個別に追加要件。
* NBT が非常に大きいとパフォーマンス問題を引き起こすため、実運用では `maxPackedNbtSize` の調整が必須。
* Ender Chest を許可するとゲーム性が崩れる可能性があるためデフォルトは除外。

---

## ローカライズキー（例）

```json
{
  "item.keep_chest.packed": "Keep Chest (Packed)",
  "item.keep_chest.tooltip_1": "Shift + 破壊でチェストを中身のまま回収します。",
  "message.keep_chest.packed_success": "チェストを詰めました。"
}
```

---

## テスト/QA の担当案

* 開発 → 単体テスト + 自動化スクリプト（NBT上限・同時pack）
* QA → マルチサーバーで保護プラグイン併用テスト（WorldGuard / GP）
* 運用 → `logPackingOperations` を有効にして数サーバーで稼働させる（不正検知）

---

## langファイル
- 15ヵ国語対応（英語・日本語必須）
