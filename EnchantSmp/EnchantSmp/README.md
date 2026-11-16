# EnchantSmp 0.2.0

Enchain SMP-inspired Paper plugin: **any enchantment on any item**, plus random enchantment rolling.

- Server: Paper 26.2 · Java 25 · Gradle · package `com.enchantsmp`

## Commands (all need OP / the listed permission)

| Command | Permission | What it does |
|---|---|---|
| `/enchant start` | `enchantsmp.start` | Every N minutes (default 10) every player gets an independent random roll |
| `/enchant stop` | `enchantsmp.stop` | Stops automatic rolling |
| `/enchant roll` | `enchantsmp.roll` | Rolls for yourself |
| `/enchant roll <player>` | `enchantsmp.roll.others` | Rolls for that player |
| `/enchant rollall` | `enchantsmp.rollall` | Independent roll for every online player |
| `/enchant reload` | `enchantsmp.reload` | Reloads config.yml |

## Rolling rules
- Rolls apply to the item in the **main hand** only.
- Existing enchantments are never removed or changed, and an enchantment can only go **up**.
- An enchantment already at its maximum level (vanilla or `enchantments.max-levels` in config) is not eligible.
- Curses are excluded by default (`enchantments.excluded`).

## Effects on unusual items (Stage 3, in progress)
Works while the item is in your **main hand**:

| Enchantment | Behaviour |
|---|---|
| Protection, Fire/Blast/Projectile Protection, Feather Falling | Damage reduction (same maths as armor). **No extra hearts.** |
| Blast Protection on a thrown **trident** | Explosion where it lands |
| Riptide (non-trident item) | Right-click: launch forward, no water needed |
| Lunge | Left-click in air: dash forward |
| Efficiency (non-mining items) | +10% attack speed per level |
| Thorns, Respiration, Depth Strider, Swift Sneak, Aqua Affinity, Frost Walker | Work from the hand |

Sharpness, Smite, Fortune, Unbreaking, Looting, Fire Aspect, etc. already work on any item in vanilla.
Not done yet: Soul Speed, Binding/Vanishing curse behaviour, Riptide spin-attack damage, Loyalty/Channeling on non-tridents.

## Building on GitHub (no installs needed)
1. Create a repository on github.com and upload everything in this folder (keep `.github`).
2. Open the **Actions** tab → *Build EnchantSmp* → wait for the green tick.
3. Open the finished run and download the **EnchantSmp-jar** artifact (a ZIP containing the plugin `.jar`).
4. Put the `.jar` in your server's `plugins` folder and restart.
