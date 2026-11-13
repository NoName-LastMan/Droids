# Droids

Autonomous clockwork automatons to automate survival chores in Minecraft, starting with the faithful **Farmer Droid**.

Built for **Minecraft 1.21.1** on **NeoForge**.

---

## Features

### 🚜 Farmer Droid (Strictly Mechanical)
* **True Mechanical AI**: No living-creature behaviors! Does not wander aimlessly or look around. Stands motionless in standby mode when no work is pending.
* **Powered / Unpowered Shutdown**: If fuel runs out, the Droid **shuts down completely** (frozen posture, zero movement, dark lens).
* **Configurable Work Boundaries**: Operates strictly within user-designated coordinate bounds `[X1, Z1]` to `[X2, Z2]`.
* **Autonomous Harvesting & Replanting**: Scans farmland inside its boundary, harvests mature crops (Wheat, Carrots, Potatoes, Beetroots, Pumpkins, Melons), and replants seeds instantly.
* **Automated Storage Logistics**: Walks directly to its linked Chest/Barrel to deposit yields while keeping a working reserve of seeds.
* **Dedicated Terminal GUI**: Right-click the Droid to open its control interface:
  * **Fuel Slot**: Solid fuels (Coal, Charcoal, Blaze Rods, Sticks) with real-time countdown.
  * **Upgrade Slot**: Accepts Range Upgrade modules to expand allowed area capacity.
  * **9-Slot Internal Storage**: Direct access to stored seeds and harvested produce.

### 🔧 Droid Wrench
* **Area Bounding Box**:
  * Right-click first block to set **Corner 1**.
  * Right-click second block to set **Corner 2** (computes total width & length).
  * Shift-right-click in air to clear markers.
* **Storage Linking**: Shift-right-click any Chest or Barrel to set the deposit target.
* **Configuration Transfer**: Right-click the Droid with the wrench to upload the boundary box and storage coordinates.

### ⚙️ Range Upgrade
* Placed into the Droid's Upgrade Slot to unlock larger farm dimensions:
  * **Base (0 Upgrades)**: Up to **5x5** area
  * **1x Upgrade**: Up to **9x9** area
  * **2x Upgrades**: Up to **15x15** area
  * **3x Upgrades**: Up to **21x21** area
  * **4x Upgrades**: Up to **31x31** area

---

## Crafting Recipes

* **Farmer Droid**:
  * `1x Clock` (top) + `2x Iron Ingot` (middle sides) + `1x Copper Block` (center) + `1x Redstone Dust` (bottom)
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
