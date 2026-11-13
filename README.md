# Droids

Autonomous clockwork automatons to automate survival chores in Minecraft, featuring the **Farmer Droid** and **Miner Droid**.

Built for **Minecraft 1.21.1** on **NeoForge**.

---

## Features

### 🚜 Farmer Droid (Strictly Mechanical)
* **True Mechanical AI**: No living-creature behaviors! Does not wander aimlessly or look around. Stands motionless in standby mode when no work is pending.
* **Powered / Unpowered Shutdown**: If fuel runs out, the Droid **shuts down completely** (frozen posture, zero movement, dark lens).
* **Configurable Work Boundaries**: Operates strictly within user-designated coordinate bounds `[X1, Z1]` to `[X2, Z2]`.
* **Autonomous Harvesting & Replanting**: Scans farmland inside its boundary, harvests mature crops (Wheat, Carrots, Potatoes, Beetroots, Pumpkins, Melons), and replants seeds instantly.
* **Automated Storage Logistics**: Walks directly to its linked Chest/Barrel to deposit yields while keeping a working reserve of seeds.
* **Dedicated Terminal GUI**: Fuel countdown, Range Upgrade slot, and 9-slot storage.

### ⛏️ Miner Droid (Heavy Excavation)
* **Dual Operation Modes** (Toggled in Terminal GUI):
  * **➔ TUNNEL (Horizontal)**: Bores a 3x3 tunnel straight ahead. Max distance governed by Range Upgrades (32m ➔ 64m ➔ 128m ➔ 256m).
  * **⬇ QUARRY (Vertical)**: Excavates an area layer by layer downwards. Capped at a **strict 20-block absolute depth limit** from the starting surface! Automatically places ladder shafts to ascend/descend.
* **Dynamic Pickaxe Speed**: Mining speed dynamically scales with the equipped Pickaxe tier (Wood ➔ Stone ➔ Iron ➔ Diamond ➔ Netherite) and the *Efficiency* enchantment!
* **Active Hazard Protection**:
  * **Continuous Gravel Clearing**: Continuously mines overhead falling gravel/sand before stepping forward.
  * **Suffocation & Drowning Immunity**: Heavy metallic frame cannot suffocate or drown.
  * **Fluid & Chasm Patching**: Uses mined cobblestone/deepslate to plug lava/water and bridge pit floors.
* **Tool Durability & Emergency Fueling**:
  * Pickaxe loses durability normally. Supports *Fortune* and *Silk Touch*!
  * Emergency auto-refuel: Consumes 1 mined Coal only when fuel is critically low (< 15 seconds). All other ores are stored for deposit.

### 🔧 Droid Wrench
* **Area Bounding Box**:
  * Right-click first block to set **Corner 1**.
  * Right-click second block to set **Corner 2** (computes total width & length).
  * Shift-right-click in air to clear markers.
* **Storage Linking**: Shift-right-click any Chest or Barrel to set the deposit target.
* **Configuration Transfer**: Right-click any Droid (Farmer or Miner) with the wrench to upload the boundary box and storage coordinates.

### ⚙️ Range Upgrade
* Placed into the Droid's Upgrade Slot:
  * **Farmer**: 5x5 ➔ 9x9 ➔ 15x15 ➔ 21x21 ➔ 31x31 area.
  * **Miner**: 32m ➔ 64m ➔ 128m ➔ 256m tunnel distance, and 5x5 ➔ 9x9 ➔ 16x16 (Slime Chunk) quarry dimension.

---

## Crafting Recipes

* **Farmer Droid**:
  * `1x Clock` (top) + `2x Iron Ingot` (middle sides) + `1x Copper Block` (center) + `1x Redstone Dust` (bottom)
* **Miner Droid**:
  * `1x Iron Pickaxe` (top) + `2x Iron Ingot` (middle sides) + `1x Copper Block` (center) + `1x Clock` (bottom)
* **Droid Wrench**:
  * `2x Iron Ingot` + `1x Copper Ingot` + `1x Stick`
* **Range Upgrade**:
  * `2x Copper Ingot` (top & bottom) + `2x Redstone Dust` (sides) + `1x Amethyst Shard` (center)

---

## Development & Building

### Requirements
* Java 21 JDK
* Gradle (provided via `./gradlew`)

### Build JAR
```bash
./gradlew build
```
The compiled mod JAR will be located in `build/libs/droids-<version>.jar`.

---

## License
Licensed under the [MIT License](LICENSE).
