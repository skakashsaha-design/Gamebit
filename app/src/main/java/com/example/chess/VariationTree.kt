package com.example.chess

import java.util.UUID

data class BoardArrow(val from: Square, val to: Square, val colorHex: Long = 0xCC10B981)
data class BoardHighlight(val square: Square, val colorHex: Long = 0xAA38BDF8)

class MoveNode(
    val id: String = UUID.randomUUID().toString(),
    val state: ChessBoardState,
    val move: Move?,
    val san: String,
    val parent: MoveNode? = null,
    val children: MutableList<MoveNode> = mutableListOf(),
    var comment: String = "",
    val nags: MutableList<Int> = mutableListOf(),
    val arrows: MutableList<BoardArrow> = mutableListOf(),
    val highlights: MutableList<BoardHighlight> = mutableListOf(),
    var evalCp: Int? = null,
    var evalMate: Int? = null,
) {
    val ply: Int
        get() = if (parent == null) 0 else parent.ply + 1

    fun addOrGetChild(move: Move, san: String, nextState: ChessBoardState): MoveNode {
        val existing = children.firstOrNull {
            it.move?.from == move.from &&
                it.move.to == move.to &&
                it.move.promotion == move.promotion &&
                it.move.dropPieceType == move.dropPieceType
        }
        if (existing != null) return existing
        val created = MoveNode(
            state = nextState,
            move = move,
            san = san,
            parent = this,
        )
        children.add(created)
        return created
    }

    fun mainLine(): List<MoveNode> {
        val list = mutableListOf<MoveNode>()
        var curr: MoveNode? = this
        while (curr != null && curr.children.isNotEmpty()) {
            val next = curr.children.first()
            list.add(next)
            curr = next
        }
        return list
    }

    fun pathFromRoot(): List<MoveNode> {
        val path = mutableListOf<MoveNode>()
        var curr: MoveNode? = this
        while (curr != null) {
            path.add(curr)
            curr = curr.parent
        }
        return path.reversed()
    }

    fun promoteChild(childId: String) {
        val idx = children.indexOfFirst { it.id == childId }
        if (idx > 0) {
            val item = children.removeAt(idx)
            children.add(0, item)
        }
    }

    fun deleteChild(childId: String) {
        children.removeAll { it.id == childId }
    }
}
