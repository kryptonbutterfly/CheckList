package kryptonbutterfly.checklist.persistence.versions

import android.content.ContextWrapper
import kryptonbutterfly.checklist.persistence.IData
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.function.Consumer

interface Serializer {
    fun load(context: ContextWrapper, populator: Consumer<IData>, iStream: ObjectInputStream)
    fun save(context: ContextWrapper, populator: Consumer<IData>, oStream: ObjectOutputStream)
}
