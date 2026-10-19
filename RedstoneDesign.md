# Redstone-Engine: Design-Dokument

> Status: **Entwurf** (AI-generiert, Review ausstehend). Arbeitsgrundlage, kein Ist-Report.
> Ersetzt den bisherigen Prototyp (`blocks/redstone/RedstoneEngine.kt`, `RedstoneSolver.kt`,
> `RedstoneComponents.kt`, `PistonHandler.kt`).

## 1. Ziel & Scope

Eine Redstone-Engine, die

- Vanilla-nahe Semantik liefert (Strong/Weak Power, gerichtete Komponenten, Delays),
- **Custom-Blöcke** als Redstone-Quelle, -Leiter und -Verbraucher erlaubt,
- von **unserer eigenen Block-Implementierung** (`blocksandstuff-blocks`) genutzt wird, statt
  Vanilla-Komponenten hart zu verdrahten,
- pro Instance **abschaltbar** ist,
- deterministisch simuliert (Bounded-Queue / Event-getrieben) statt per Fixed-Point-Recompute.

**Nicht-Ziele (v1):** bit-genaue Vanilla-Update-Order, Slime-/Honig-Piston-Drag, Block-Entity-Inputs
für Comparator (Container), Quasi-Connectivity, Observerspulse-Timing exakt wie Vanilla.

## 2. Anforderungen

| # | Anforderung |
| --- | --- |
| R1 | Power ist abgeleitet, **nicht** in Blockstates normaler Blöcke gespeichert. |
| R2 | Strong vs. Weak Power explizit unterscheidbar. |
| R3 | Gerichtete Komponenten (Repeater/Comparator/Observer/Piston `facing`). |
| R4 | Tick-/Delay-Modell (Bounded Queue), Clocks möglich. |
| R5 | Custom-Blöcke können als Source/Sink/Conductor registriert werden. |
| R6 | `blocksandstuff-blocks` registriert seine Vanilla-Komponenten über dieselbe API. |
| R7 | Per-Instance toggelbar; globaler Default konfigurierbar. |
| R8 | Deterministisches Ordering. |
| R9 | Engine als eigenes Modul, keine Abhängigkeit `common → redstone`. |

## 3. Warum kein Blockstate als Power-Wahrheit

Der aktuelle Prototyp liest Power aus `lit`/`powered`/`open` zurück und schreibt sie per `setBlock`.
Probleme:

- Normale Blöcke **haben** keinen Power-State; „powered" ist in Vanilla immer eine Query
  (`getSignal` / `getDirectSignal` / `isBlockPowered`).
- Strong/Weak-Unterschied geht verloren → „half powered" vs. „fully powered" stimmt nicht.
- `setBlock`-Feeds erzeugen Reentrancy und brauchen Guards (`ThreadLocal processing`).
- Save/Load, Chunk-Load und `doBlockUpdates=false` brechen die Logik.
- Zustand ist nicht dort, wo Vanilla ihn hält.

**Regel:** State wird nur gespeichert, wo Vanilla ihn speichert. Alles andere ist Query.

## 4. Architektur & Module

```
blocksandstuff-common          (unverändert; InstanceOptions, Tags, Utils)
        ▲
blocksandstuff-redstone        NEU  – Engine, Query, Registry, Simulator (kein Vanilla-Wissen)
        ▲
blocksandstuff-blocks          registriert Vanilla-Komponenten u. nutzt die API
```

- `blocksandstuff-redstone` kennt **nur** Minestom-Grundtypen + `common`. Keine Kenntnis von
  „das ist ein Repeater" – nur die API.
- `blocksandstuff-blocks` (oder ein optionaler `redstone-vanilla`-Registrar darin) implementiert
  die Vanilla-Komponenten über die öffentliche API (R6).
- `testserver` verdrahtet `registerDefault()`.

**Vorteil:** Backend (Interpretiert vs. später „kompiliert"/MCHPRS-artig) austauschbar; Tests ohne
Minestom möglich.

## 5. Kernmodell

### 5.1 Power-Queries

Zwei Achsen: **Strong/Direct** vs. **Weak/Indirect**, plus **gerichtet** vs. **omnidirektional**.

```kotlin
interface PowerQuery {
    /** Was emittiert der Block bei [from] durch die Seite [face] (Weak). */
    fun weakSignal(from: Point, face: BlockFace): Int
    /** Strong/Direct-Signal (Torch→Attachment, Repeater/Comparator→Front). */
    fun directSignal(from: Point, face: BlockFace): Int
    /** Irgendein anliegendes Signal an [pos] (weak oder direct). */
    fun isPowered(pos: Point): Boolean
    /** Nur direkt/strong gepowert (z. B. für Torch-Inversion). */
    fun isIndirectlyPowered(pos: Point): Boolean
}
```

- Wire: `weakSignal = power` (0–15), `directSignal = 0`.
- Torch/Redstone-Block/Lever/Plate: `weakSignal = directSignal = 15` (aktiv).
- Repeater/Comparator: nur `face == facing` → Front, `directSignal = output`.
- Block „strongly powered" (z. B. durch Torch auf Block): leitet `directSignal` an alle Seiten weiter.

Der Simulator hält pro emittierendem Knoten die berechnete Ausgabe; die Query ist ein dünner,
gecachter Blick darauf (kein Blockstate-Roundtrip).

### 5.2 State-Ownership

| Konzept | Speicherort |
| --- | --- |
| Wire-Power 0–15 | Blockstate `power` |
| Torch an/aus | Blockstate `lit` |
| Repeater `powered`/`delay`/`locked` | Blockstates |
| Comparator `powered` | Blockstate `powered` (nur Sichtbarkeit) |
| Comparator Analogwert (0–15) | **abgeleitet**, nur In-Memory-Component-State |
| Piston `extended` | Blockstate |
| „Position ist powered" | **abgeleitet** |
| Strong/Weak Power | **abgeleitet** |

Custom-Komponenten definieren ihre eigene Persistenz (Property **oder** Tag **oder** Block-Entity)
über die API – sie werden **nicht** auf ein bestimmtes Schema gezwungen.

**Entscheidung – analoger Comparator-Wert:** Der Analogwert ist reine abgeleitete Ausgangsleistung
(`emit`), nicht persistiert. Er lebt im In-Memory-Component-State und wird bei Bedarf neu berechnet
(Vanilla speichert ihn ebenfalls nicht; `getOutputSignal` liest Rear/Sides zur Laufzeit). Das ist
die performanteste Variante – kein `setBlock`, kein NBT-Write bei jedem Signalwechsel – und über
`ComponentState`/`Emit` nativ erweiterbar. Nur das `powered`-Blockstate wird zur Client-Sichtbarkeit
gesetzt; beim Chunk-Load wird re-evaluiert (siehe §8).

## 6. Komponenten-API & Erweiterbarkeit (R5)

### 6.1 Registrierung

```kotlin
package org.everbuild.blocksandstuff.redstone

class RedstoneRegistry {
    fun register(selector: ComponentSelector, factory: ComponentFactory)
    fun source(block: Block, source: PowerSource)        // Convenience
    fun consumer(block: Block, consumer: PowerConsumer)  // Convenience
}

sealed interface ComponentSelector {
    data class ByBlock(val block: Block) : ComponentSelector
    data class ByTag(val tag: String) : ComponentSelector
}

typealias ComponentFactory = (Block) -> RedstoneComponent
```

- `ByTag`: für Familien (z. B. `minecraft:doors`, `minecraft:trapdoors`) und Custom-Tags.
- `ByBlock`: Einzelblöcke.
- `source`/`consumer` sind Kurzformen für einfache Fälle (Custom-Blöcke ohne Sonderlogik).

### 6.2 Komponenten-Interface

```kotlin
interface RedstoneComponent {
    /** Liest den eigenen internen Zustand aus dem Block (Properties/Tags/Block-Entity). */
    fun readState(world: ReadContext): ComponentState

    /** Kombinatorisch: gewünschter Zustand aus aktuellen Eingaben. Sequenzielle Komponenten
     *  dürfen hier nur den *Zielzustand* liefern (Delay kommt über onScheduledTick). */
    fun evaluate(ctx: UpdateContext): ComponentEvaluation

    /** Was emittiert dieser Block durch [face]? (Richtungsabhängig.) */
    fun emit(ctx: ReadContext, face: BlockFace): Emit

    /** Von welchen Seiten wird Input konsumiert? Default: alle. */
    fun consumes(ctx: ReadContext): Set<BlockFace> = ALL_FACES

    /** Delay in Ticks, falls sequenziell (Repeater: 2*delay, Torch: 1, Piston: 2). */
    fun delayTicks(ctx: ReadContext): Int = 0
}

data class Emit(val weak: Int, val direct: Int = 0)

sealed interface ComponentEvaluation {
    object None : ComponentEvaluation
    data class SetState(val block: Block, val scheduleTick: Boolean = false) : ComponentEvaluation
    data class Trigger(val effect: SideEffect) : ComponentEvaluation // Piston, TNT, Dispenser
}
```

`ReadContext` bietet `block(pos)`, `power(pos)`, `facing()` und Zugriff auf den eigenen Block.
`UpdateContext` zusätzlich `setState(pos, block)` (gebatcht), `scheduleTick(pos, delay)` und
`markDirty(pos)`.

### 6.3 Einfache Quellen/Verbraucher

```kotlin
interface PowerSource {
    fun strength(block: Block): Int                 // konstant, z. B. Redstone-Block = 15
    fun directional(block: Block): Boolean = false  // true → nur facing-Front
}

interface PowerConsumer {
    /** Wird aufgerufen, wenn sich der anliegende Pegel ändert. */
    fun onPowerChanged(ctx: UpdateContext, powered: Boolean, strength: Int)
}
```

Custom-Blöcke, die nur „an/aus" reagieren (Custom-Lampe, Custom-Tür), implementieren `PowerConsumer`
und werden intern in einen `GenericConsumerComponent` gewrappt.

### 6.4 Custom-State

`ComponentState` ist bewusst opak: die Komponente entscheidet, ob sie Properties
(`block.withProperty`), NBT-Tags (`block.withTag`) oder eine Block-Entity nutzt. Der Simulator
schreibt nur das von `evaluate` gelieferte Ersatz-Block über `setState`.

## 7. Simulation & Tick-Ordering (R4, R8)

### 7.1 Bounded Queue + Delta-Cycles

Pro Instance ein `RedstoneSimulator`:

1. **Tick-Queue** (Prioritätsqueue nach absoluter Tickzeit) für sequenzielle Knoten und
   Nachbar-Änderungen.
2. Pro Tick: alle fälligen Updates abarbeiten.
3. Innerhalb eines Ticks: **Delta-Cycle** – eine FIFO „dirty positions"-Queue, die abgearbeitet wird,
   bis sie leer ist (kombinatorisches Settling: Wire breitet sich im selben Tick aus).
4. Sequenzielle Knoten planen sich mit `delayTicks` erneut in die Tick-Queue ein.

```
tick():
  due = tickQueue.pollDue(now)
  delta = ArrayDeque(due)
  guard = 0
  while (delta.isNotEmpty()) {
     if (++guard > DELTA_LIMIT) { reportNonConvergent(); break }   // Zyklus-Schutz
     val pos = delta.poll()
     val comp = componentAt(pos) ?: continue
     val eval = comp.evaluate(ctx(pos))
     when (eval) {
        is SetState -> { if (writeState(pos, eval)) enqueueNeighboursOf(pos, delta) }
        is Trigger  -> applyEffect(eval.effect)
        None        -> {}
     }
     if (comp.delayTicks(ctx) > 0) scheduleTick(pos, delay)
  }
```

Writes innerhalb eines Ticks nutzen `setBlock(pos, block, doBlockUpdates = false)`, um
Rückkopplung über Minestom-Neighbor-Updates zu vermeiden; das „Aufwecken" der Nachbarn läuft
explizit über `enqueueNeighboursOf` (kein Event-Reentrancy-Hack mehr).

### 7.2 Ordering

**Entscheidung: deterministisch.**

- Eingang in den Delta-Cycle nach **(x, y, z)** (bzw. Queue-Insertion), Faces in fester Reihenfolge.
- **Bewusst nicht** Vanilla-Order-kompatibel; das wird dokumentiert. Wer Vanilla-Contraption-Order
  braucht, muss sie separat modellieren.

### 7.3 Zyklen & Clocks

- `DELTA_LIMIT` verhindert Endlosschleifen im Delta-Cycle.
- Sequenzielle Komponenten (Torch 1 Tick, Repeater `2*delay`, Piston 2 Ticks) haben echte Delays →
  Torch-/Repeater-Clocks entstehen **zwischen** Ticks und terminieren den Delta-Cycle.
- Oszillierende Delta-Cycles (rein kombinatorische Rückkopplung, in Vanilla quasi nicht existent)
  werden als Fehler gemeldet/loggt.

## 8. Lifecycle & Events

| Event | Aktion |
| --- | --- |
| Block placed (`Placement`) | `simulator.onBlockPlaced(pos)` → Komponente auflösen, Nachbarn dirty |
| Block removed/destroyed | `simulator.enqueueNeighboursOf(pos)` |
| Block changed (`InstanceBlockUpdateEvent`) | Filter auf „redstone-relevant"; sonst `enqueueNeighboursOf` |
| Chunk geladen | Redstone-Knoten im Chunk markieren (löst R-Scan / „Initial-Scan"-Problem) |
| Instance tick | `simulator.tick()` |
| Instance unload | Simulator-State verwerfen |

Der Simulator ist **einziger Schreiber** von Redstone-State; nach `SetState` werden Nachbarn gezielt
neu bewertet. Non-Redstone-Effekte (Piston-Bewegung, Tür-Animation, TNT) laufen über normale
`setBlock`-Pfade.

## 9. Integration in `blocksandstuff-blocks` (R6)

`blocksandstuff-blocks` registriert die Vanilla-Komponenten **über die Redstone-API** (kein
hartkodiertes Redstone-Wissen mehr im Behavior-Modul):

```kotlin
// blocksandstuff-blocks/.../redstone/VanillaRedstoneComponents.kt
fun RedstoneRegistry.registerVanilla() {
    register(ByTag("minecraft:doors")) { DoorComponent(it) }
    register(ByBlock(Block.REDSTONE_WIRE)) { WireComponent }
    register(ByBlock(Block.REPEATER)) { RepeaterComponent }
    register(ByBlock(Block.COMPARATOR)) { ComparatorComponent }
    register(ByBlock(Block.REDSTONE_TORCH)) { TorchComponent }
    register(ByTag("minecraft:buttons")) { ButtonComponent }
    // ...
    register(ByBlock(Block.PISTON)) { PistonComponent(sticky = false) }
    register(ByBlock(Block.STICKY_PISTON)) { PistonComponent(sticky = true) }
}
```

- Die bestehenden Placement-/Behavior-Regeln bleiben für Form/Interaktion zuständig; die
  *Power-Logik* wandert in die Redstone-Komponenten.
- Der alte `RedstoneEngine`/`RedstoneSolver`/`PistonHandler` in `blocks/redstone/` wird entfernt.

## 10. Toggle & Per-Instance (R7)

- `InstanceOptions` (bestehender `InstanceOptionsProvider`) bekommt `redstoneEnabled: Boolean`.
- Registrierung: `RedstoneRegistrations.enable(instance)` bzw. automatisch für Instanzen mit
  aktivierter Option.
- Kein globaler `IdentityHashMap`-State mehr; der Simulator ist an die Instance gebunden und wird
  bei Unload freigegeben.
- Globaler Default über Server-Config; einzelne Instanzen können es deaktivieren (z. B. Kreativ-Welten).

## 11. Custom-Block-Beispiel

```kotlin
// Ein Custom-Block, der als Lampe wirkt und auf Weak Power hört.
val registry: RedstoneRegistry = ...

registry.consumer(Block.fromKey("my:glow_lamp")!!) { block ->
    PowerConsumer { ctx, powered, _ ->
        val updated = ctx.block().withProperty("lit", powered.toString())
        ctx.setState(ctx.pos(), updated)
    }
}

// Ein Custom-Block, der als gerichtete Quelle 15 ausgibt.
registry.source(Block.fromKey("my:emitter")!!) { block -> MyEmitter(block) }

class MyEmitter(private val block: Block) : RedstoneComponent {
    override fun emit(ctx: ReadContext, face: BlockFace): Emit {
        val facing = RedstoneComponents.facing(block)
        return if (face == facing) Emit(weak = 15, direct = 15) else Emit(0)
    }
    // evaluate/readState je nach Bedarf
}
```

Custom-Blöcke können eigene Blockstates nutzen; die Engine erzwingt kein Schema.

## 12. Testing

- **Unit (ohne Minestom):** `TestWorld` implementiert die Read-Schnittstellen + sammelt Writes.
  Golden-Tests für: Inverter, T-Flip-Flop, Repeater-Delay 1–4, Comparator compare/subtract,
  Piston extend/retract, Torch-Clock (Tick-Trace).
- **Integration (Minestom `InstanceContainer`):** Platzieren/Ersetzen/Entfernen in laufenden
  Schaltungen; Chunk-Load-Scan; Per-Instance-Toggle.
- **Regression:** die bereits existierenden Tests (`replacingWireKeepsLampLit`,
  `repeaterCircuitSurvivesWireReplacement`, `RedstoneWirePlacementRuleTest`) bleiben und laufen
  gegen die neue Engine.

## 13. Performance

- Event-getrieben statt Voll-Recompute: nur geänderte Knoten + Nachbarn.
- Delta-Cycle-Limit als Schutz.
- Optional später: Dirty-Chunk-Marking, kompilierter Graph (MCHPRS-artig) als Backend hinter
  derselben API.
- Ziel-Budget: Simulator-Kosten skalieren mit **Änderungen**, nicht mit Weltgröße.

## 14. Migrationsplan

1. **Modul** `blocksandstuff-redstone` anlegen (API, Registry, Query-Interfaces, Simulator-Skelett).
2. **Kern**: Wire, Torch (Inversion), Lever/Button/Plate, Lampe, Türen/Trapdoors/Gates migrieren.
3. **Sequenziell**: Repeater (Delay/Lock), Comparator (compare/subtract).
4. **Mechanik**: Piston/Sticky (direktes Schieben), Observer, TNT-Zündung, Dispenser/Dropper.
5. **Blocks-Anbindung** (`registerVanilla`) + alte `blocks/redstone/`-Klassen entfernen.
6. **Toggle** per Instance-Option + Testserver-Verdrahtung.
7. **Tests/Golden-Traces** ergänzen.

## 15. Entscheidungen & offene Punkte

### Entschieden

- **Ordering:** deterministisch (§7.2), bewusst nicht Vanilla-Order-kompatibel.
- **Backend:** interpretierter Event-Simulator für v1; kompiliert (MCHPRS-artig) nur bei Bedarf
  hinter derselben API (§13).
- **Comparator-Analogwert:** abgeleitet, In-Memory-Component-State, nicht persistiert (§5.2).
- **Comparator-Container-Inputs (Block-Entity):** **nicht in v1.** Wird als spätere API-Erweiterung
  vorgesehen (`PowerQuery`/`ReadContext` bekommen eine optionale `containerSignal(pos)`-Erweiterung),
  ohne die Kern-API zu brechen.
- **Slime-/Honig-Piston-Drag:** eigener Folge-Entwurf (`PistonDragDesign`), nicht Teil der v1-Engine;
  die Piston-Komponente wird so geschnitten, dass ein Drag-Resolver später ergänzt werden kann.

### Offen (nicht blockierend)

- Exakter Umfang der Chunk-Load-Re-Evaluierung (ganzer Chunk vs. lazy bei Zugriff) – v1: ganzer
  Chunk-Scan der Redstone-Knoten.
- Debug-/Diagnose-Hooks (Tick-Trace, non-convergence-Report) – v1: Logging, Ausbau später.
