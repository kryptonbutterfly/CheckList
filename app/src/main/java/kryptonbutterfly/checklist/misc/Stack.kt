package kryptonbutterfly.checklist.misc

import java.util.*

class Stack<E>(var backingList: LinkedList<E>, limit: Int = 1) {
    var limit: Int = limit
        set(value) {
            require(value >= 0) { "limit must be >= 0" }
            field = value
            enforceLimit()
        }

    fun add(e: E) {
        backingList.addFirst(e)
        enforceLimit()
    }

    fun remove(): E? {
        return if (backingList.isEmpty()) null
        else backingList.removeFirst()
    }

    fun clear() {
        backingList.clear()
    }

    private fun enforceLimit() {
        while (backingList.size > limit) {
            backingList.removeLast()
        }
    }

    fun isEmpty(): Boolean {
        return backingList.isEmpty()
    }
}