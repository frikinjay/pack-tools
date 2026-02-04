# Pack Tools 

PackTools adds features, utilities and fixes that are commonly used by most modpacks. Most of these are QOL features and can be disabled in real time through the config screen, the features should be compatible with most mods but with the amount of features ensuring compatibility is an ongoing process.

## Configuration Guide

This guide covers all available settings for PackTools. You can modify these via the in-game config menu or by editing the config file directly.

---

## Values
Core gameplay variables.

* **Mending Repair XP Ratio**: Sets how much durability is restored per XP point. (Range: 1–100, Default: 2)
* **Piglin Trade Duration**: How many ticks a Piglin takes to examine an item before bartering. (Range: 1–200, Default: 34)

---

## Features
Toggleable mechanics and quality-of-life tweaks.

### World & Gameplay
* **Dragon Egg Respawner**: Allows the Dragon Egg to spawn every time the dragon is killed.
* **XP Clumping**: Groups experience orbs together to reduce lag.
* **Easy Harvesting**: Right click harvesting for crops.
* **Jump Over**: Lets you hop over fences and walls without carpet or potions.
* **Double Doors**: Automatically opens or closes both sides of a double door at once, works for fences as well.
* **No Trample**: Prevents players and mobs from destroying farmland by jumping on it.
* **(Experimental)** **Elytra Replenisher**: Ensures End Ships are re-stocked with Elytra and Dragon head for every new player that enters an end ship.

### Combat & Movement
* **Swing Through**: Allows you to attack mobs through grass, flowers, and other non-solid foliage.
* **Quick Zoom**: A dedicated zoom function.
* **Reacher**: Adds "reach-around" block placement (similar to Quark).
* **Stack Maxxer**: Increases certain item stack limits.

### Utilities & Notifications
* **Startup Ping**: Plays a notification sound when the game finishes loading.
* **Global Sound Suppressor**: Only plays global sounds (like the Wither spawn) for nearby players.
* **Experimental Suppressor**: Hides the warning screen when loading experimental worlds.
* **Toast Suppressor**: Disables tutorial and recipe "toast" popups in the corner.
* **Rapid Piglin Trading**: Speeds up the bartering process.
* **Restocker**: Automatically moves items from your inventory to your hotbar when a stack runs out.
* **Mouse and Key Tweaks**: Various input tweaks and shortcuts using the scroll wheel and quick stacking.

---

## Tooltips
Additional info added to item hover text.

* **Mod Name Display**: Shows which mod an item belongs to.
* **Durability Tooltip**: Shows exact durability numbers on tools and armor.
* **Tag Tooltips**: Displays item tags (requires F3+H to be active).
* **Food Information**: Shows hunger and saturation values.
* **Effect Information**: Details the potion effects a consumable item provides.

---

## HUD
Visual overlays for your main display.

* **Status Effects**: Lists active potion effects on the HUD.
* **Armor Value**: Displays your current armor level on the HUD.
* **Armor Toughness**: Displays the toughness stat above the armor bar on the HUD.
* **Health Overlay**: Changes how hearts are rendered when the health bar overflows for better visibility.
* **Restoration Preview**: Shows a pulsing preview of how much health or food you’ll gain when holding a consumable.

## Block tags and Item tags for customization
You can add blocks and items to tags to add them to certain features or blacklist them.
All of these tags are under the `packtools` namespace.

* `harvest_blacklist`: Blacklists crops from being harvested through right clicks.
* `tall_harvestable`: Adds additional cactus or sugarcane like crops to be harvestable through right clicks.
* `jumpable`: Adds blocks the player can jump over like walls, fences etc.
* `swingthrough_blacklist`: Blacklists blocks players shouldn't be able to swing through.
* `restocker_blacklist`: Blacklists items from being restocked onto the players hand.
* `stackmaxxer_blacklist`: Blacklists items from having their stack size increased like shulker boxes etc.
* `end_city`: Structures the elytra replenisher will try to replenish elytra and dragon head in.

### I will be adding further features, blocks and items that are commonly required in a modpack, if you find any bugs or have any suggestions for features or tweaks in the existing features you can create an issue on github or reach out to me through discord.
