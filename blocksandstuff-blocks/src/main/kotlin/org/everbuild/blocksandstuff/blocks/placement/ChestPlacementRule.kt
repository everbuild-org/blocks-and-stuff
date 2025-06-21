package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import kotlin.math.cos
import kotlin.math.sin

class ChestPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockUpdate(updateState: UpdateState): Block {
        val currentBlock = updateState.currentBlock
        val instance = updateState.instance
        val position = updateState.blockPosition
        if (instance !is Instance) return currentBlock
        
        // Erst alle Nachbarn in einem größeren Radius aktualisieren
        updateAllNearbyChests(instance, position)
        
        // Dann die aktuelle Truhe aktualisieren
        return updateSingleChest(instance, position, currentBlock)
    }

    private fun updateAllNearbyChests(instance: Instance, centerPosition: Point) {
        val visited = mutableSetOf<Point>()
        val toUpdate = mutableSetOf<Point>()
        
        // Sammle alle Truhen in einem 3x3 Bereich
        for (x in -1..1) {
            for (z in -1..1) {
                val checkPos = centerPosition.add(x.toDouble(), 0.0, z.toDouble())
                if (isChest(instance.getBlock(checkPos))) {
                    toUpdate.add(checkPos)
                }
            }
        }
        
        // Aktualisiere alle gefundenen Truhen
        for (pos in toUpdate) {
            if (pos !in visited) {
                val block = instance.getBlock(pos)
                val updatedBlock = updateSingleChest(instance, pos, block)
                if (updatedBlock != block) {
                    instance.setBlock(pos, updatedBlock)
                }
                visited.add(pos)
            }
        }
    }

    private fun updateSingleChest(instance: Instance, position: Point, currentBlock: Block): Block {
        if (!isChest(currentBlock)) return currentBlock
        
        val facing = Direction.valueOf(currentBlock.getProperty("facing")?.uppercase() ?: return currentBlock)
        val currentType = currentBlock.getProperty("type") ?: "single"
        
        // Wenn die Truhe Teil einer Doppeltruhe ist, prüfe ob der Partner noch existiert
        if (currentType in listOf("left", "right")) {
            val partnerDirection = if (currentType == "left") {
                facing.rotateYClockwise()
            } else {
                facing.rotateYCounterClockwise()
            }
            
            val partnerPos = position.add(partnerDirection.vec())
            val partnerBlock = instance.getBlock(partnerPos)
            
            // Detaillierte Partnerprüfung
            val hasValidPartner = isChest(partnerBlock) &&
                    partnerBlock.name() == currentBlock.name() &&
                    Direction.valueOf(partnerBlock.getProperty("facing")?.uppercase() ?: "") == facing &&
                    partnerBlock.getProperty("type") == (if (currentType == "left") "right" else "left")
            
            if (!hasValidPartner) {
                // Partner ungültig - zu Einzeltruhe konvertieren
                return currentBlock.withProperty("type", "single")
            }
        }
        
        // Wenn es eine Einzeltruhe ist, NICHT automatisch verbinden
        return currentBlock
    }

    private fun tryFormDoubleChest(
        instance: Block.Getter,
        position: Point,
        chestBlock: Block
    ): Block? {
        val facing = Direction.valueOf(chestBlock.getProperty("facing")?.uppercase() ?: return null)
        
        // Prüfe beide möglichen Richtungen für Doppeltruhen
        for (direction in listOf(facing.rotateYClockwise(), facing.rotateYCounterClockwise())) {
            val neighborPos = position.add(direction.vec())
            val neighborBlock = instance.getBlock(neighborPos)
            
            // Detaillierte Kompatibilitätsprüfung
            if (isChest(neighborBlock) && 
                neighborBlock.name() == chestBlock.name() &&
                neighborBlock.getProperty("type") == "single") {
                
                val neighborFacing = Direction.valueOf(
                    neighborBlock.getProperty("facing")?.uppercase() ?: continue
                )
                
                // Truhen müssen in die gleiche Richtung schauen
                if (neighborFacing == facing) {
                    val isLeft = isLeftChest(facing, direction)
                    val chestType = if (isLeft) "left" else "right"
                    val neighborChestType = if (isLeft) "right" else "left"
                    
                    // Aktualisiere die benachbarte Truhe
                    if (instance is Instance) {
                        instance.setBlock(
                            neighborPos,
                            neighborBlock.withProperty("type", neighborChestType)
                        )
                    }
                    
                    return chestBlock.withProperty("type", chestType)
                }
            }
        }
        
        return null
    }

    override fun blockPlace(placementState: PlacementState): Block? {
        val facing = placementState.getNearestHorizontalLookingDirection()
        var resultBlock = placementState.block.withProperty("facing", facing.name.lowercase())
        
        val isWaterlogged = isWaterAt(placementState.instance, placementState.placePosition)
        resultBlock = resultBlock.withProperty("waterlogged", isWaterlogged.toString())

        val shouldTryConnection = if (placementState.isPlayerShifting) {
            isLookingAtChest(placementState)
        } else {
            true
        }

        if (shouldTryConnection) {
            val doubleChestResult = tryFormDoubleChest(
                placementState.instance,
                placementState.placePosition,
                resultBlock
            )
            return doubleChestResult ?: resultBlock.withProperty("type", "single")
        }
        
        return resultBlock.withProperty("type", "single")
    }

    private fun isLookingAtChest(placementState: PlacementState): Boolean {
        val playerPos = placementState.playerPosition() ?: return false
        val pitch = Math.toRadians(playerPos.pitch().toDouble())
        val yaw = Math.toRadians(playerPos.yaw().toDouble())
        val dirX = -sin(yaw) * cos(pitch)
        val dirY = -sin(pitch)
        val dirZ = cos(yaw) * cos(pitch)

        val startPos = playerPos.add(0.0, 1.6, 0.0) // Standard-Augenhöhe
        val maxDistance = 6.0
        val steps = 60

        for (i in 1..steps) {
            val t = (i.toDouble() / steps) * maxDistance
            val checkPos = startPos.add(dirX * t, dirY * t, dirZ * t)
            val block = placementState.instance().getBlock(checkPos)
            if (isChest(block)) {
                return true
            }

            if (!block.registry().isAir && !isTransparent(block)) {
                break
            }
        }
        return false
    }

    private fun isTransparent(block: Block): Boolean {
        return when (block.name()) {
            "minecraft:air",
            "minecraft:water",
            "minecraft:glass",
            "minecraft:glass_pane" -> true
            else -> block.name().contains("glass") ||
                    block.name().contains("pane") ||
                    block.registry().isAir
        }
    }

    private fun isCompatibleChest(chest1: Block, chest2: Block): Boolean {
        if (!isChest(chest1) || !isChest(chest2)) return false
        return chest1.name() == chest2.name()
    }

    private fun isChest(block: Block): Boolean {
        return block.name() in listOf("minecraft:chest", "minecraft:trapped_chest")
    }

    private fun isLeftChest(facing: Direction, neighborDirection: Direction): Boolean {
        return when (facing) {
            Direction.NORTH -> neighborDirection == Direction.EAST
            Direction.EAST -> neighborDirection == Direction.SOUTH
            Direction.SOUTH -> neighborDirection == Direction.WEST
            Direction.WEST -> neighborDirection == Direction.NORTH  // Hier war der Fehler
            else -> false
        }
    }

    private fun isWaterAt(instance: Block.Getter, position: Point): Boolean {
        val block = instance.getBlock(position)
        return block.name() == "minecraft:water"
    }

    private fun Direction.rotateYClockwise(): Direction {
        return when (this) {
            Direction.NORTH -> Direction.EAST
            Direction.EAST -> Direction.SOUTH
            Direction.SOUTH -> Direction.WEST
            Direction.WEST -> Direction.NORTH
            else -> this
        }
    }

    private fun Direction.rotateYCounterClockwise(): Direction {
        return when (this) {
            Direction.NORTH -> Direction.WEST
            Direction.WEST -> Direction.SOUTH
            Direction.SOUTH -> Direction.EAST
            Direction.EAST -> Direction.NORTH
            else -> this
        }
    }
}