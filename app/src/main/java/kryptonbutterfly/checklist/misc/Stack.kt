package kryptonbutterfly.checklist.misc

import java.util.*

class Stack<E> {
    var backingList = LinkedList<E>()
    var limit: Int = 1
        set(limit) {
            field = limit
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