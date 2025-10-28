# Packed Chest

Pack up any vanilla chest (and compatible storage blocks) with its contents intact, move it wherever you like, then drop it back down without spilling a single item.

![Packed Chest icon](assets/packed-chest/icon.png)

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Installation](#installation)
- [Gameplay](#gameplay)
- [Configuration](#configuration)
- [Integrations and API](#integrations-and-api)
- [Compatibility & Limitations](#compatibility--limitations)
- [Roadmap & Testing](#roadmap--testing)
- [Localization](#localization)
- [Development](#development)
- [License](#license)

---

## Overview

**Packed Chest** is a Fabric mod for Minecraft 1.21.10 that lets players pick up chests — contents included — simply by breaking them while sneaking. It keeps multiplayer servers safe by integrating with protection plugins, offering configurable costs, cooldowns, and blacklists, and exposing events for further automation or moderation.

- **Mod ID**: `packed-chest`
- **Supported Loader**: Fabric Loader `>= 0.17.3`
- **Minecraft**: `~1.21.10`
- **Java**: `>= 21`
- **Source**: [GitHub – ChihaluCoding/packed-chest](https://github.com/ChihaluCoding/packed-chest)

---

## Key Features

- **Shift-break packing** – Sneak, break, and receive a `packed_chest` item with full inventory NBT preserved.
- **Instant restore** – Place the packed item to deploy the original chest (single, trapped, or double), including names, locks, owner data, and contents.
- **Double chest handling** – Optionally captures both halves into a single packed item.
- **Protection-aware** – Honors WorldGuard, GriefPrevention, and other registered protection hooks before allowing pack/unpack.
- **Hopper safety** – Detects active item transport and can block or delay packing to avoid dupes.
- **Blacklist support** – Prevent packing if restricted items are present.
- **Configurable costs & cooldowns** – Charge XP, consume items, damage tools, or enforce cooldowns per operation.
- **Ownership lock** – Prevent others from unpacking someone else’s chest when enabled.
- **Event API** – `PackedChestPackEvent` and `PackedChestUnpackEvent` let other mods cancel, audit, or extend the behaviour.
- **NBT guardrails** – Rejects oversized payloads to prevent lag spikes and exploits.

---

## Installation

1. **Install Fabric Loader** for Minecraft 1.21.10 if you have not already.
2. **Install Fabric API** that matches your loader and game version.
3. Drop the latest `packed-chest-<version>.jar` into your `mods/` folder.
4. Launch the game. The mod is available on both server and client (`environment: "*"`) and will sync seamlessly in multiplayer.

### Building From Source

```sh
./gradlew build
```

The compiled JARs are produced in `build/libs/`. Fabric Loom is configured to generate both remapped runtime and sources artifacts.

---

## Gameplay

| Action | Result |
| ------ | ------ |
| Sneak (`Shift`) + break chest | Chest is removed, no items spill, and a `packed_chest` item drops or enters inventory. |
| Place `packed_chest` item | Restores original block state, including contents and metadata. |
| Break without sneaking | Vanilla behaviour; items scatter as usual. |
| Attempt to pack protected or in-use chest | Operation is denied with an informative message. |

Additional notes:

- Double chests can be packed as a single item if `allowDoubleChestPack` is enabled (default `true`).
- Ender Chests are disallowed by default (`disallowEnderChestPacking: true`) to preserve balance.
- Packed items are **not stackable** by default to avoid accidental duplication.

---

## Configuration

The configuration is stored as JSON (default path: `config/packed-chest.json`). A representative set of keys is shown below:

```json
{
  "enableShiftPacking": true,
  "requireSneak": true,
  "allowDoubleChestPack": true,
  "disallowEnderChestPacking": true,
  "packCostMode": "none",
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

Key toggles:

- **Costs** – Set `packCostMode` to `xp`, `item`, or `tooldamage` to charge players for packing actions.
- **Protection** – Keep `respectProtectionPlugins` enabled so region ownership is checked before packing/unpacking.
- **Blacklist** – Populate `blacklistedItems` with item IDs that should block packing (`minecraft:dragon_egg`, modded items, etc.).
- **NBT Size** – Tighten `maxPackedNbtSize` to control serialized payload limits in high-load servers.

Changes can be hot-reloaded if your mod loader supports configuration listeners; otherwise restart the server/client after editing the file.

---

## Integrations and API

- **Protection Hooks** – Implement the `IProtectionChecker` interface to bridge your preferred protection plugin. Built-in hooks are planned for WorldGuard and GriefPrevention.
- **Events**:
  - `PackedChestPackEvent(Player, BlockPos, PackedItemStack, Cause)` – Cancel to prevent packing.
  - `PackedChestUnpackEvent(Player, BlockPos, PackedItemStack)` – Cancel to prevent unpacking.
- **Ownership Lock** – When enabled, packed items store `OwnerUUID` and validate it on unpack.
- **Logging** – Enable `logPackingOperations` to record pack/unpack attempts for audit trails.

---

## Compatibility & Limitations

- Vanilla chests, trapped chests, and barrels are fully supported. Additional storage blocks must implement standard inventory interfaces.
- Ender Chests remain disabled by default to protect gameplay balance.
- Extremely large NBT payloads are rejected; consider adjusting `maxPackedNbtSize` for heavily modded servers.
- While mixins aim for broad compatibility, exotic modded containers may require explicit whitelist entries or adapter code.

---

## Roadmap & Testing

**Current focus**

- MVP complete: single chest packing/unpacking, basic UI tooltips, fundamental NBT preservation.

**Upcoming milestones**

1. Double chest parity, blacklist enforcement, NBT size checks (in progress).
2. Protection plugin hooks, configurable costs, hot-reloadable settings.
3. Documentation polish, expanded localization, automated CI coverage, performance profiling.

**Test Coverage Targets**

- Single and double chests, trapped chests, named chests.
- Failure paths: blacklist, hopper interaction, protection denial, oversized NBT.
- Multiplayer stress testing: concurrent packing attempts, server restart persistence.

---

## Localization

- English (`en_us`) and Japanese (`ja_jp`) are maintained in `assets/packed_chest/lang/`.
- Fifteen total languages are planned; contributions are welcome via PRs with corresponding `lang` JSON files.

---

## Development

- Java 21 is required for compilation.
- Fabric Loom splits client and common source sets (`src/main` and `src/client`).
- Mixins are defined in `packed-chest.mixins.json` and `packed-chest.client.mixins.json`.
- Run `./gradlew runClient` or `./gradlew runServer` for local testing using the included `fabric-loom` run configs.

Contributions and issue reports are welcome. Please accompany feature work with tests where applicable and document any configuration changes.

---

## License

This project is licensed under the [MIT License](LICENSE). Feel free to use and adapt the code in accordance with that license.

---

### 日本語の概要 (Japanese Summary)

- **Packed Chest（パックドチェスト）** は、スニークしながらチェストを破壊すると中身を保持したままアイテム化し、別の場所で設置し直せる Fabric 用モッドです。
- 保護プラグイン連携、ブラックリスト、コスト／クールダウン、イベント API などを備え、サバイバルとマルチプレイでも安全に運用できます。
- 設定ファイルは `config/packed-chest.json`。ホッパー稼働中やブラックリスト該当時はパックを拒否し、NBT サイズ制限もあります。
- ダブルチェストやトラップチェストにも対応し、所有者ロックやログ出力も利用可能です。

