package kryptonbutterfly.checklist.persistence.versions

import android.content.ContextWrapper
import kryptonbutterfly.checklist.DeletedTask
import kryptonbutterfly.checklist.persistence.Data
import kryptonbutterfly.checklist.persistence.IData
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.function.Consumer

object Undefined: Serializer {
    override fun load(
        context: ContextWrapper,
        populator: Consumer<IData>,
        iStream: ObjectInputStream)
    {
        val data = Data()
        if (iStream.available() <= 0)
            return

        repeat(iStream.readInt()) { data.tasks.add(iStream.readUTF())}
        if (iStream.available() <= 0 || !iStream.readBoolean())
            return
        data.deleted.add(DeletedTask(iStream.readInt(), iStream.readUTF()))
        populator.accept(data)
    }

    override fun save(context: ContextWrapper, populator: Consumer<IData>, oStream: ObjectOutputStream) {
        val data = Data()
        populator.accept(data)
        oStream.writeInt(data.tasks.size)
        data.tasks.forEach { oStream.writeUTF(it) }

        val notEmpty = data.deleted.isNotEmpty()
        oStream.writeBoolean(notEmpty)
        if (notEmpty) {
            val first = data.deleted.last()
            oStream.writeInt(first.index)
            oStream.writeUTF(first.description.toString())
        }
    }
}
