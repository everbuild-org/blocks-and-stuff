# Block-Behaviours: Ist-Stand, fehlende Blöcke & Migrationsplan

> Deckt **alle Blöcke mit Behaviour** ab – sowohl bereits implementierte (Abschnitt 0) als auch
> fehlende (Abschnitt 1–8) – jeweils mit Einschätzung, ob die Implementierung ausreicht oder auf
> **Twaddle Stacked Behaviour** (`twaddle-stacked-behaviour-block` + `twaddle-random-ticking`)
> umgestellt werden sollte. Diese Datei ist eine Arbeitsgrundlage, kein generierter Statusreport.
> Der automatische Report bleibt `TODO-behavior.md`.

## Grundprinzipien

- **Ein Behaviour gilt pro Block-Familie, nicht pro Variante.** Alle 16 Farben eines Buttons
  teilen sich eine Regel. Rein dekorative Blöcke (Wool, Terrakotta, Concrete, Slabs, Stairs,
  Walls, Polished-Varianten, ...) brauchen **kein** Behaviour und werden hier nicht gelistet.
- **Selektion über Tags/Keys**, nicht über Einzelblöcke (`minecraft:buttons`, `minecraft:crops`, ...).
  Block-Zustände (z. B. `age`, `powered`, `open`) liest die Regel aus dem Kontext.
- **Aktivitäten** (Twaddle): `interact`, `place`, `destroy`, `touch`, `tick`, `randomTick`.
- **Ergebnis** einer Regel: `RuleResult.Next` (weiter), `Consume` (Kette beenden / als behandelt
  markieren), `Cascade` (Kontext für spätere Regeln ändern).

### Legende

| Symbol | Bedeutung |
| --- | --- |
| 🔗 | Kann mit anderen Familien zu **einem** Behaviour zusammengefasst werden |
| 🧩 | Eine Regel deckt die ganze Familie ab (Tag-basiert) |
| ⚠️ | Komplex: eigener BlockEntity-/Ticker-Zustand, evtl. nicht 1:1 als Regel abbildbar |
| ♻️ | Erweitert eine bereits existierende Regel |

Status der bestehenden Implementierungen:

| Symbol | Bedeutung |
| --- | --- |
| ✅ | Ausreichend – behalten wie ist |
| 🛠️ | Ansatz ok, aber **erweitern/korrigieren** (fehlende Varianten, Bugs) |
| 🔄 | Auf **Twaddle Stacked Behaviour** migrieren (lohnt sich) |
| 🧱 | Interact via Twaddle möglich, **Inventar-/BlockEntity-Logik bleibt Minestom-nativ** |

> **Bewertungskriterium:** „ausreichend" = Vanilla-Edge-Cases sind sauber abgedeckt
> (Sneak, Redstone, `waterlogged`, Metall-Varianten, Events/Cancel-Pfad). Twaddle-Migration
> lohnt sich, wenn eine Regel in eine Kette wiederverwendbarer Regeln zerfällt oder
> Random-Tick betroffen ist (dann ersetzt `twaddle-random-ticking` das Eigenbau-System).

---

## 0. Ist-Stand: Implementierte Behaviours & Empfehlung

Alle aktuell registrierten Gruppen aus `VanillaBlockBehaviour`.

| Familie | Datei | Zustand | Empfehlung |
| --- | --- | --- | --- |
| Crafting Table, Anvil, Brewing Stand, Loom, Grindstone, Smithing, Cartography, Stonecutter, Enchanting | `GenericWorkStationRule` | Inventar via `BlockInventoryHandler` (common), funktioniert | 🧱 Interact-Teil kann Twaddle-Rule werden, Inventar-Logik behalten |
| Wooden Trapdoors | `WoodenTrapDoorOpenRule` | nur Holz, kein Redstone/`waterlogged`, kein Metall | 🔄 → generisches `ToggleBooleanBehaviour` (Abschnitt 1) |
| Fence Gates | `GateOpenRule` | ok, aber ohne Redstone & Ausrichtungsfälle | 🔄 → generisches Toggle (Abschnitt 1) |
| Wooden Doors | `DoorOpenRule` | Holz ja; Eisen/Kupfer + Redstone fehlen | 🔄 → generisches Toggle (Abschnitt 1/2) |
| Copper Oxidation | `CopperOxidationRule` | 21 Blöcke, Custom-RandomTick | 🛠️ Blockliste erweitern (bars/chains/lanterns/doors/trapdoors/golem/lightning_rod/bulb), 🔄 RandomTick auf Twaddle |
| Signs | `SignEditRule` | öffnet Editor; `// TODO: Write back updated signs` | 🛠️ Rückweg (Text speichern) fehlt – korrigieren, Interact ggf. Twaddle |
| Cake | `CakeEatRule` | inkl. Kerzen-Guard, Event | ✅ (optional als Twaddle-Interact-Rule) |
| Candle Cakes | `CandleCakeRule` | ok | ✅ |
| Strippable Wood | `StrippingBehaviorRule` | hartcodierte Map, Axt-Logik | 🔄 → generische Werkzeug-Mapping-Regel (Beispiel D) |
| Concrete Powder | `ConcretePowderRule` | `tick()` + `isTickable()` | 🔄 auf Twaddle `tick`/`randomTick` |
| Farmland | `FarmlandRule` | Custom-RandomTick, Bewässerung | 🔄 RandomTick auf Twaddle, Logik behalten |
| Coral | `CoralRule` | Custom-RandomTick, tot-Varianten | 🔄 RandomTick auf Twaddle |
| Sweet Berry Bush | `SweetBerryBushRule` | RandomTick/Interact/Touch; kleines Problem: `onRandomTick` gibt immer einen Block zurück (unnötiges `setBlock`) | 🛠️ Rückgabe `null` wenn unverändert, 🔄 RandomTick auf Twaddle |
| Player Head | `SkullDisplayRule` | nur BlockEntity-Tags (`profile`, Sound, Name) | ✅ |

> **Fazit:** Kein bestehendes Behaviour ist grundsätzlich falsch, aber die **Interact-Familien
> (Türen/Trapdoors/Gates/Buttons)** gehören in ein gemeinsames Toggle-Behaviour, die
> **Werkzeug-Transformation** in eine Mapping-Regel, und **alle Random-Tick-Blöcke** auf
> `twaddle-random-ticking`. Reine BlockEntity-Fälle (Köpfe, Anvil-Workstations) bleiben
> Minestom-nativ, nur ihr Interact-Pfad kann eine Twaddle-Regel werden.

---

## 1. Interaktions-/Click-Toggles (Rechtsklick)

**Kernidee:** Button, Lever, Druckplatte, Tür, Trapdoor, Fence Gate, Kerze, Kupfer-Bulb,
Lagerfeuer und Redstone-Lampe teilen **dasselbe Muster**: beim Interagieren wird ein
**boolesches Property** umgeschaltet (`powered` / `open` / `lit`) und ein Redstone-Update
ausgelöst. Deshalb gehören sie **alle** in EIN gemeinsames
`ToggleBooleanBehaviour(property, ...)` – es unterscheiden sich nur der Property-Name,
die Auslöse-Aktivität (Interact vs. Touch vs. Redstone) und kleine Sonderfälle.

| Behaviour | Blöcke / Tag | Property | Auslöser | Sonderfall |
| --- | --- | --- | --- | --- |
| Button | `minecraft:buttons` (`wooden_`/`stone_`) | `powered` | `interact` | Timer ~1 s → zurück |
| Lever | `minecraft:levers` | `powered` | `interact` | – |
| Druckplatte | `minecraft:pressure_plates` (`wooden_`/`stone_`) | `powered` | `touch`+`tick` | Gewichtsschwelle |
| Tür | `minecraft:doors` | `open` | `interact`/redstone | Eisen/Kupfer nur Redstone |
| Trapdoor | `minecraft:trapdoors` | `open` | `interact`/redstone | Metall nur Redstone |
| Fence Gate | `minecraft:fence_gates` | `open` | `interact`/redstone | Ausrichtungslogik |
| Kupfer-Bulb | `*_copper_bulb` | `lit` | `interact`/redstone | – |
| Redstone-Lampe | `minecraft:redstone_lamp` | `lit` | redstone | – |
| Kerze / Candle Cake | `minecraft:candles`, `candle_cakes` | `lit` | `interact` | Kerzenanzahl |
| Lagerfeuer | campfire / soul_campfire | `lit` | `interact` | Cooking, Wasser |
| Glocke | `minecraft:bell` | – | `interact` | ⚠️ Sonderlogik |
| Notenblock | `minecraft:note_block` | – | `interact` | ⚠️ Instrument/Note |

> 🔗 **Alle Zeilen außer Glocke/Notenblock = EIN generisches Click-Behaviour.**
> Buttons etc. laufen dort mit, weil sie genau das „toggle boolean property on click"-
> Verhalten haben. Getrennt bleiben nur echte Sonderlogik (Timer, Kerzenanzahl, Metall-
> Türen ohne Interact, Cooking) – und die werden als Guard/Parameter in dieselbe Regel
> gesteckt, nicht als eigene Klasse.

## 2. Nicht-hölzerne Türen / Trapdoors / Gates

| Behaviour | Blöcke / Tag | Kombinierbar | Activity |
| --- | --- | --- | --- |
| Eisentür (nur Redstone) | `Block.IRON_DOOR` | 🔗 mit Tür-Regel | neighbor |
| Kupfertür (Interact + Redstone) | `minecraft:copper_doors` | 🔗 mit Tür-Regel | `interact` + neighbor |
| Eisen-/Kupfer-Trapdoor | `minecraft:trapdoors` minus `minecraft:wooden_trapdoors` | 🔗 mit Trapdoor-Regel | neighbor |
| Fence Gate per Redstone | `minecraft:fence_gates` | ♻️ bestehende Gate-Regel | neighbor |

> Existiert die hölzerne Variante bereits (`DoorOpenRule`, `WoodenTrapDoorOpenRule`,
> `GateOpenRule`). Die Metall-Varianten lassen sich mit **einer parametrisierten Regel**
> abdecken: `interactable: Boolean`, `redstoneDriven: Boolean`.

## 3. Redstone-Komponenten

| Behaviour | Blöcke | Kombinierbar | Activity |
| --- | --- | --- | --- |
| Redstone-Lampe leuchtet | `minecraft:redstone_lamp` | 🧩 | neighbor |
| Redstone-Wire-Power | `minecraft:redstone_wire` | ⚠️ teuer (Netzberechnung) | neighbor |
| Repeater Delay/Mode | `minecraft:repeater` | ⚠️ | `interact` + neighbor |
| Comparator Mode | `minecraft:comparator` | ⚠️ | `interact` + neighbor |
| Observer-Puls | `minecraft:observer` | ⚠️ | neighbor |
| Tageslichtsensor | `minecraft:daylight_detector` | 🧩 (`interact` invertiert) | `tick` |
| Zielblock (Target) | `minecraft:target` | 🧩 | projectile |
| Blitzableiter | `minecraft:lightning_rod` | 🧩 | lightning |
| Sculk-Sensor | `minecraft:sculk_sensor`, `calibrated_sculk_sensor` | ⚠️ Vibrationen | `interact`+tick |
| Tripwire / Haken | `minecraft:tripwire`, `minecraft:tripwire_hook` | 🔗 = 1 Regel | `touch` |
| Schienen-Typen | powered/activator/detector rail | 🔗 | neighbor |
| Redstone-Fackel/-Block | torch/block | 🧩 (selten eigener Zustand) | — |

## 4. Container & BlockEntity-UI

| Behaviour | Blöcke | Kombinierbar | Activity |
| --- | --- | --- | --- |
| Inventar öffnen | Chest, Trapped Chest, Barrel, Shulker Box, Ender Chest | 🔗 „open inventory" = 1 Regel (Größe je Archetyp) | `interact` |
| Ofen schmelzen | Furnace, Blast Furnace, Smoker | ♻️ existiert (`FurnaceArchetype`) | `interact`+tick |
| Dispenser/Dropper | `minecraft:dispenser`, `minecraft:dropper` | 🔗 = 1 Regel | redstone |
| Hopper-Transfer | `minecraft:hopper` | ⚠️ Twaddle `inventory-transfer` | tick |
| Crafter | `minecraft:crafter` | ⚠️ | redstone |
| Lektüre | `minecraft:lectern` | ⚠️ BlockEntity | `interact` |
| Jukebox | `minecraft:jukebox` | 🧩 | `interact` |
| Blumentopf | `minecraft:flower_pot` (+potted_*) | 🧩 | `interact` |
| Composter | `minecraft:composter` | 🧩 (`level` + Redstone) | `interact` |
| Bienenstock/-nest | `minecraft:bee_nest`, `beehive` | 🔗 = 1 Regel (`honey_level`) | `interact` |
| Lagerfeuer | `minecraft:campfire`, `soul_campfire` | 🔗 = 1 Regel | `interact`+tick |
| Glocke | `minecraft:bell` | ⚠️ | `interact` |
| Respawn-Anker | `minecraft:respawn_anchor` | ⚠️ (`charges`) | `interact` |
| Beacon / Conduit | `minecraft:beacon`, `conduit` | ⚠️ | `interact` |
| Verzierter Topf | `minecraft:decorated_pot` | ⚠️ BlockEntity | — |
| Lodestone | `minecraft:lodestone` | 🧩 (Kompass-Bindung) | — |

## 5. Schlafen & Spawn

| Behaviour | Blöcke / Tag | Kombinierbar | Activity |
| --- | --- | --- | --- |
| Bett: schlafen/Spawn setzen | `minecraft:beds` | 🔗 alle Betten = 1 Regel | `interact` |

## 6. Werkzeug-Transformation (generisches Muster)

**Alle folgenden können zu EINER Mapping-Regel zusammengefasst werden** (🔗):
Input `(itemInHand, block)` → Output-Block. Das ist der größte Abkürzungshebel.

| Werkzeug | Wirkung | Beispiel-Blöcke |
| --- | --- | --- |
| Axt | entrinden + wachsen | `minecraft:logs`/`wood` (♻️ existiert) |
| Axt | Kupfer wachsen | alle `waxed_*` ♻️ Kupfer |
| Schere | Kürbis schnitzen | Pumpkin → Carved Pumpkin |
| Schere | Bienenstock ernten | Bee Nest/Hive (`honey_level=5`) |
| Schere | Ranken/Kelp ernten | Vine, Cave Vines, Kelp |
| Schaufel | Lagerfeuer löschen / Gras → Trampelpfad | Campfire, Grass Block |
| Flint & Steel | Lagerfeuer entzünden / TNT zünden | Campfire, TNT |
| Honigwabe | Kupfer wachsen/konservieren | alle Copper-Blöcke ♻️ |
| Eimer | Kessel füllen/leeren | Cauldron-Varianten ⚠️ |

> Diese Eine-Regel ersetzt `StrippingBehaviorRule` und deckt Schere/Schaufel/Flint ab.
> Mapping-Tabelle + Guard `if (tool !in mapping) Next`.

## 7. Random-Tick: Wachstum & Ausbreitung

| Behaviour | Blöcke / Tag | Kombinierbar | Activity |
| --- | --- | --- | --- |
| Nutzpflanzen wachsen | `minecraft:crops`, `nether_wart`, `torchflower_crop`, `pitcher_crop` | 🔗 generische `age`-Regel | `randomTick` |
| Stängel wachsen | `pumpkin_stem`, `melon_stem` | 🔗 mit Crops | `randomTick` |
| Setzlinge → Baum | `minecraft:saplings`, `mangrove_propagule` | 🔗 = 1 Regel | `randomTick` |
| Zuckerrohr/Kaktus/Bambus | sugar_cane, cactus, bamboo | 🔗 „vertikal wachsen" | `randomTick` |
| Seegras/Kelp | kelp, kelp_plant, seagrass | 🔗 | `randomTick` |
| Chorus-Blüte | `minecraft:chorus_flower` | 🧩 | `randomTick` |
| Beeren/Ranken | cave_vines, glow_berries, twisting/weeping_vines | 🔗 | `randomTick` |
| Tropfstein/Tropfen | `minecraft:pointed_dripstone` | 🧩 | `randomTick` |
| Amethyst-Knospen | amethyst cluster/buds | 🧩 | `randomTick` |
| Rasen/Myzel/Podzol/Nylium-Ausbreitung | grass_block, mycelium, podzol, *_nylium | 🔗 „spreading block" | `randomTick` |
| Laub-Zerfall | `minecraft:leaves` | 🧩 (`distance`-Property) | `randomTick` |
| Feuer-Ausbreitung/Erlöschen | `minecraft:fire`, `soul_fire` | ⚠️ | `randomTick` |
| Eis/Schnee schmelzen | ice, frosted_ice, snow | 🔗 „melt/freeze" | `randomTick` |
| Korallen absterben | ♻️ existiert (`CoralRule`) | — | `randomTick` |
| Kupfer oxidieren | ♻️ bestehende Regel erweitern | — | `randomTick` |

> Die generische **`age`-Crop-Regel** (Parameter: `maxAge`, Lichtanforderung, Chance) deckt
> Weizen, Karotten, Kartoffeln, Rüben, Netherwarze, Torchflower/Pitcher und Stängel ab —
> ein einziger parametrisierter Code-Pfad.

## 8. Physik & Sonstiges

| Behaviour | Blöcke | Kombinierbar | Activity |
| --- | --- | --- | --- |
| Fallende Blöcke | sand, red_sand, gravel, suspicious_sand/gravel, anvil/chipped/damaged | 🔗 = 1 Regel (`FallingBlock`-Util existiert) | neighbor/`tick` |
| Betonpulver aushärten | ♻️ `ConcretePowderRule` (existiert) | — | `place`/`tick` |
| TNT zünden | `minecraft:tnt` | 🧩 | `interact`/redstone |
| Schwamm saugt Wasser | sponge, wet_sponge | 🧩 | neighbor |
| Dracheneier-Teleport | `minecraft:dragon_egg` | 🧩 | `interact` |
| Gerüst (Scaffolding) | `minecraft:scaffolding` | 🧩 | `interact`/`touch` |
| Schneeschicht | `minecraft:snow` | 🔗 mit melt | `place`/neighbor |

---

## Implementierungsbeispiele (Twaddle)

Alle Beispiele: Regel-IDs sind namenspacen, Handler werden nach `MinecraftServer.init`
kompiliert und per Block-ID registriert (siehe Twaddle-Docs „Attach handlers").

### Beispiel A — Generisches Click-Behaviour (Buttons, Türen, Kerzen, ...)

EIN Behaviour für alle „toggle boolean on click"-Blöcke. Button läuft hier mit rein.

```kotlin
class ToggleBooleanBehaviour(
    private val property: String,          // "powered" | "open" | "lit"
    private val interactable: Boolean = true,
    private val resetAfterTicks: Int? = null, // z. B. Button
)

fun ToggleBooleanBehaviour.rule(id: String) = Rule<InteractionActivityContext>("blocksandstuff:$id") { ctx ->
    if (!interactable) return@Rule RuleResult.Next
    if (ctx.player.isSneaking && !ctx.player.itemInMainHand.isAir) return@Rule RuleResult.Next

    val current = ctx.block.getProperty(property) == "true"
    ctx.replaceBlock(ctx.block.withProperty(property, (!current).toString()))
    // resetAfterTicks != null  -> Minestom-Scheduler plant das Zurücksetzen
    RuleResult.Consume
}

val buttons  = ToggleBooleanBehaviour("powered", resetAfterTicks = 20).rule("button")
val doors    = ToggleBooleanBehaviour("open").rule("door")
val trapdoors = ToggleBooleanBehaviour("open").rule("trapdoor")
val candles  = ToggleBooleanBehaviour("lit").rule("candle")

val ruleset = BlockRuleset.build {
    // Twaddle selektiert per Block-ID -> Tags vorher zu Block-Listen auflösen.
    fun family(tag: String, rule: Rule<InteractionActivityContext>) =
        tagBlocks(tag).forEach { addBlock(it) { interact.append { install(rule) } } }

    family("minecraft:buttons", buttons)
    family("minecraft:doors", doors)
    family("minecraft:trapdoors", trapdoors)
    family("minecraft:candles", candles)
}
```

> Sonderfall-Handling (Metall-Türen nur Redstone, Kerzenanzahl, Button-Timer) läuft über
> die Parameter/Guards **derselben** Regel – keine eigenen Klassen nötig.

### Beispiel B — Generische Crop-Regel (RandomTick, mehrfach genutzt)

Eine Regel, viele Nutzpflanzen. Wiederverwendbar dank Parameter.

```kotlin
class CropBehaviour(private val maxAge: Int, private val growthChance: Int) {
    fun asRule(id: String) = Rule<RandomTickActivityContext>("blocksandstuff:$id") { ctx ->
        val age = ctx.block.getProperty("age")?.toIntOrNull() ?: return@Rule RuleResult.Next
        if (age >= maxAge) return@Rule RuleResult.Next
        if (Random.nextInt(growthChance) != 0) return@Rule RuleResult.Next

        // randomTick: replaceBlock staged newBlock für den Tick-Caller
        ctx.replaceBlock(ctx.block.withProperty("age", (age + 1).toString()))
        RuleResult.Consume
    }
}

val wheat = CropBehaviour(maxAge = 7, growthChance = 8).asRule("wheat")
val carrots = CropBehaviour(maxAge = 7, growthChance = 8).asRule("carrots")

val ruleset = BlockRuleset.build {
    addBlock(Block.WHEAT)  { randomTick.append { install(wheat) } }
    addBlock(Block.CARROTS){ randomTick.append { install(carrots) } }
}
```

### Beispiel C — Parametrisierte Tür (Holz + Metall in einer Regel)

Statt Holztür und Eisentür getrennt: ein Verhalten mit Flags.

```kotlin
class DoorBehaviour(
    private val interactable: Boolean,   // Holz/Kupfer: true, Eisen: false
    private val redstoneDriven: Boolean  // Metall: true
)

fun DoorBehaviour.rule(block: Block) = Rule<InteractionActivityContext>("blocksandstuff:door") { ctx ->
    val open = ctx.block.getProperty("open") == "true"
    if (!interactable) return@Rule RuleResult.Next
    if (ctx.player.isSneaking && !ctx.player.itemInMainHand.isAir) return@Rule RuleResult.Next

    ctx.replaceBlock(ctx.block.withProperty("open", (!open).toString()))
    RuleResult.Consume
}
// Redstone-Teil als separate neighbor-Regel, wiederverwendet für Türen/Trapdoors/Gates.
```

### Beispiel D — Werkzeug-Mapping (eine Regel für alle Tools)

Ersetzt `StrippingBehaviorRule` und deckt Schere/Schaufel/Flint mit ab.

```kotlin
data class ToolTransform(val from: Block, val tool: Material, val to: Block)

val TRANSFORMS = listOf(
    ToolTransform(Block.PUMPKIN, Material.SHEARS, Block.CARVED_PUMPKIN),
    ToolTransform(Block.STONE, Material.WOODEN_PICKAXE, Block.AIR), // Beispiel
)

val UseTool = Rule<InteractionActivityContext>("blocksandstuff:use_tool") { ctx ->
    val match = TRANSFORMS.firstOrNull {
        ctx.block.key() == it.from.key() && ctx.player.itemInMainHand.material() == it.tool
    } ?: return@Rule RuleResult.Next

    ctx.replaceBlock(match.to)
    RuleResult.Consume
}

val ruleset = BlockRuleset.build {
    TRANSFORMS.map { it.from }.distinct().forEach { block ->
        addBlock(block) { interact.append { install(UseTool) } }
    }
}
```

### Beispiel E — Fallender Block (Physik, geteilt)

```kotlin
val Falling = Rule<InteractionActivityContext>("blocksandstuff:falling") { ctx ->
    // Nachbarprüfung + ggf. Bewegung über das FallingBlock-Util ausführen
    RuleResult.Next
}
// Ein und dieselbe Regel für sand, gravel, concrete_powder, anvil, ...
```

---

## Umsetzungsreihenfolge (Vorschlag)

1. **Phase 0/1**: Twaddle-Dependencies verdrahten, eine Regel end-to-end (Trapdoor) verifizieren.
2. **Werkzeug-Mapping** (Beispiel D) – ersetzt `StrippingBehaviorRule` und deckt Kupfer-Wax,
   Schere, Schaufel, Flint ab.
3. **Türen/Trapdoors/Gates** konsolidieren (Beispiel C) inkl. Metall-Varianten + Redstone
   → ersetzt `DoorOpenRule`, `WoodenTrapDoorOpenRule`, `GateOpenRule`.
4. **Buttons/Levers/Plates** (Beispiel A).
5. **Bestands-Fixes**: `SignEditRule` Rückweg (Text speichern), `SweetBerryBushRule`
   `null` bei unverändert.
6. **Crops** (Beispiel B) + Saplings/Zuckerrohr als generische Wachstumsregeln.
7. **Random-Tick auf `twaddle-random-ticking`**: `FarmlandRule`, `CoralRule`,
   `SweetBerryBushRule`, `CopperOxidationRule`, `ConcretePowderRule`; eigenes
   `randomticking/`-Paket entfernen.
8. **Copper-Oxidation erweitern** (bars/chains/lanterns/doors/trapdoors/golem/lightning_rod).
9. **Falling Blocks**, Container, restliche Einzelfälle.

> **Hinweis zu Tags:** Vanilla-Tags wie `minecraft:buttons`, `minecraft:crops`,
> `minecraft:beds` decken die Familien ab. Nicht vorhandene Tags ggf. in
> `blocksandstuff/blocks/block/*.json` (vgl. `bamboo.json`, `glass_panes.json`) ergänzen.
> **Twaddle-Selektion läuft über Block-IDs** – ein Tag wird beim Ruleset-Bau zu einer
> Block-Liste aufgelöst (`addBlock` je Block), nicht als Tag übergeben.

---

## Checkliste (Abhaken)

> Automatisch generiert und abgehakt durch `kotlin blocksandstuff_util.main.kts` (Block-Keys als Detektor).
> Den Bereich zwischen den Markern nicht manuell bearbeiten.

<!-- BEHAVIOUR_CHECKLIST_START -->
### Phase 0/1 – Grundgerüst
- [x] Twaddle-Dependencies in blocksandstuff-blocks/build.gradle.kts
- [x] Eine Regel end-to-end verifizieren (Trapdoor)

### Bestand migrieren / korrigieren
- [x] StrippingRule.kt
- [x] WaxingRule.kt
- [x] Türen (Holz) → gemeinsames Toggle-Behaviour
- [x] Trapdoors → gemeinsames Toggle-Behaviour
- [x] Fence Gates → gemeinsames Toggle-Behaviour
- [x] SignEditRule Rückweg (Text speichern)
- [x] SweetBerryBushRule → null bei unverändert
- [x] Copper-Oxidation Blockliste erweitern
- [x] Random-Tick auf twaddle-random-ticking

### Interaktions-/Click-Toggles
- [ ] Button (minecraft:buttons)
- [ ] Lever (minecraft:levers)
- [ ] Druckplatten
- [ ] Kerze (minecraft:candles)
- [x] Kupfer-Bulb
- [ ] Redstone-Lampe
- [ ] Lagerfeuer
- [ ] Glocke
- [ ] Notenblock

### Türen / Trapdoors / Gates
- [ ] Eisentür (nur Redstone)
- [x] Kupfertüren
- [x] Eisen-/Kupfer-Trapdoors
- [x] Fence Gates per Redstone

### Redstone-Komponenten
- [ ] Redstone-Wire
- [ ] Redstone-Torch
- [ ] Repeater
- [ ] Comparator
- [ ] Observer
- [ ] Tageslichtsensor
- [ ] Zielblock (Target)
- [x] Blitzableiter
- [ ] Sculk-Sensor
- [ ] Tripwire + Haken
- [ ] Schienen

### Container & BlockEntity
- [ ] Chest / Trapped Chest / Ender Chest
- [ ] Barrel
- [ ] Shulker Box
- [ ] Ofen / Blast Furnace / Smoker
- [ ] Dispenser / Dropper
- [ ] Hopper
- [ ] Crafter
- [ ] Lectern
- [ ] Jukebox
- [ ] Flower Pot
- [ ] Composter
- [ ] Bee Nest / Beehive
- [ ] Respawn Anker
- [ ] Beacon / Conduit
- [ ] Decorated Pot
- [ ] Lodestone

### Betten
- [ ] Beds (schlafen/Spawn)

### Random-Tick: Wachstum & Ausbreitung
- [ ] Crops
- [ ] Saplings
- [ ] Sugar Cane / Cactus / Bamboo
- [ ] Kelp / Seagrass
- [ ] Chorus Flower
- [ ] Gras-Ausbreitung
- [ ] Leaves – Zerfall
- [ ] Pointed Dripstone
- [ ] Amethyst

### Physik & Sonstiges
- [ ] Falling Blocks
- [ ] TNT zünden
- [ ] Sponge / Wet Sponge
- [ ] Dragon Egg Teleport
- [ ] Scaffolding
- [ ] Snow Layer
<!-- BEHAVIOUR_CHECKLIST_END -->


