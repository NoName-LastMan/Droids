# Droids

Autonomous clockwork automatons to automate survival chores in Minecraft, starting with the faithful **Farmer Droid**.

Built for **Minecraft 1.21.1** on **NeoForge**.

---

## Features

### 🚜 Farmer Droid
* **Autonomous Crop Harvesting**: Scans nearby farmland, harvests mature crops (Wheat, Carrots, Potatoes, Beetroots, Pumpkins, Melons), and **replants seeds automatically**.
* **Smart Storage Depositing**: Walks to a linked chest or barrel to deposit harvested yields while retaining seed reserves.
* **Solid Fuel Steam Engine**: Refuel by right-clicking with Coal, Charcoal, Blaze Rods, Sticks, or Coal Blocks.
* **Tactile Visuals**: Custom 3D clockwork model with rotating gears, an optical lens eye, and working chimney steam particles.

### 🔧 Droid Wrench
* **Container Linking**: Right-click any storage block (Chest, Barrel) to record its coordinates.
* **Droid Configuration**: Right-click a Droid with the recorded wrench to bind the storage target.
* **Status Inspection**: Shift-right-click a Droid with an empty hand to check remaining fuel time and linked storage.

---

## Crafting Recipes

* **Farmer Droid**:
  * `1x Clock` (top)
  * `2x Iron Ingot` (middle left & right)
  * `1x Copper Block` (center)
  * `1x Redstone Dust` (bottom)
* **Droid Wrench**:
  * `2x Iron Ingot` + `1x Copper Ingot` + `1x Stick`

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
