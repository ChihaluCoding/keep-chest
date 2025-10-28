# Packed Chest

## 使い方

1. 収納したいチェストや樽の前で `Shift` を押しながら破壊します。中身を保持した「収納済みチェスト」アイテムがインベントリに入るか、その場にドロップします。
2. 回収したアイテムを持って通常どおり設置すると、元の向き・内容物・カスタム名を含めて復元されます。
3. `Shift` を押さずに破壊した場合はバニラと同様に中身が散乱します。

### ダブルチェストをまとめて収納したい場合

`config/packed-chest.json` の `"allowDoubleChestPack"` を `true` に設定します。片側を `Shift` 破壊すると両側の内容が 1 つのアイテムにまとめられます。

### 回収できない時のチェック

- 他プレイヤーが開いている収納は回収できません。
- `blacklistedItems` に含まれるアイテムが入っていると拒否されます。
- 設定した `maxPackedNbtSize` を超える大容量チェストは回収できません。
- サーバー保護プラグイン連携を有効にしている場合、権限のない領域では回収・設置がブロックされます。

### コストやクールダウンを設定する

`config/packed-chest.json` で以下を調整します。

- `"packCostMode"` を `xp` / `item` / `tooldamage` に切り替えると、回収時のコストを徴収できます。
- `"packCooldownTicks"` で再利用までの待機時間を設定します。
- `"allowPackSteal"` を `false` にすると所有者または権限者だけが回収できます。

### ログとイベント

- `"logPackingOperations": true` にすると、回収・設置の履歴がログに残ります。
- Mod からフックする場合は `PackedChestPackEvent` / `PackedChestUnpackEvent` を監視・キャンセルして挙動を制御します。
