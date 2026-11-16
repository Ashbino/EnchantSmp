# EnchantSmp

A Paper plugin project for universal enchantments and individual random enchantment rolls.

## Current starter features

- `/enchant start` starts automatic rolls every 10 minutes.
- Each online player receives an independent roll.
- Rolls are applied to the player's currently held item.
- `/enchant stop` stops automatic rolls.
- `/enchant roll [player]` manually rolls for one player.
- `/enchant rollall` rolls independently for all online players.
- `/enchant reload` reloads the configuration.
- Enchantments are applied unsafely so normally incompatible items can receive them.
- Existing enchantments are preserved. A repeated enchantment can only upgrade to a strictly higher level.

## Build on GitHub

1. Create a GitHub repository and upload the contents of this folder.
2. Push the files to the repository.
3. Open the **Actions** tab.
4. The workflow builds the plugin and uploads `EnchantSmp.jar` as an artifact.

## Important

This is the initial project foundation. The custom gameplay effects (such as Blast Protection explosions on thrown tridents, Riptide movement on incompatible items, Lunge dashes, and Protection effects while holding non-armour items) are planned for subsequent updates.

The Paper API dependency is set to `26.2-R0.1-SNAPSHOT`. If the exact API build published for your Paper 26.2 server uses a different version suffix, update the dependency in `build.gradle`.
