package kryptonbutterfly.checklist.persistence.versions

import android.content.ContextWrapper
import kryptonbutterfly.checklist.DeletedTask
import kryptonbutterfly.checklist.persistence.Data
import kryptonbutterfly.checklist.persistence.IData
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.function.Consumer

object V1_3_0: Serializer {
    override fun load(context: ContextWrapper,
        populator: Consumer<IData>,
        iStream: ObjectInputStream)
    {
        val data = Data()
        repeat(iStream.readInt()) {
            data.tasks.add(iStream.readUTF()) }
        repeat(iStream.readInt()) {
            data.deleted.add(DeletedTask(iStream.readInt(), iStream.readUTF()))}
        populator.accept(data)
    }

    override fun save(context: ContextWrapper, populator: Consumer<IData>, oStream: ObjectOutputStream) {
        val data = Data()
        populator.accept(data)
        oStream.writeInt(data.tasks.size)
        data.tasks.forEach { oStream.writeUTF(it) }

        oStream.writeInt(data.deleted.size)
        data.deleted.forEach {
            oStream.writeInt(it.index)
            oStream.writeUTF(it.description.toString())
        }
    }
}
