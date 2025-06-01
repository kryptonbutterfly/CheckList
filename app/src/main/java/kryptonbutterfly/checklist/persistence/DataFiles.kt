package kryptonbutterfly.checklist.persistence

import android.content.Context
import android.content.ContextWrapper
import kryptonbutterfly.checklist.persistence.versions.Serializer
import kryptonbutterfly.checklist.persistence.versions.V1_3_0
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList
import kotlin.streams.toList

enum class DataFiles(val fileName: String, val serializers: Serializers) {
    Undefined("TasksListData.txt", kryptonbutterfly.checklist.persistence.versions.Undefined),
    TasksListData_bin("TasksListData.bin", V1_3_0);

    constructor(fileName: String, vararg versions: Serializer) :
            this(fileName, newSerializers(versions))

    fun getLatest(): Pair<Int, Serializer> {
        return serializers.serializers.last()
    }
}

private var version: Int = 0
private fun nextVersion(): Int {
    return version++
}

fun save(context: ContextWrapper, populator: Consumer<IData>) {
    val dataFiles = DataFiles.values().last()
    ObjectOutputStream(context.openFileOutput(dataFiles.fileName, Context.MODE_PRIVATE)).use {
        val latest = dataFiles.getLatest()
        if (dataFiles.serializers.requireVersionNumber)
            it.writeInt(latest.first)
        latest.second.save(context, populator, it)
    }
}

fun load(context: ContextWrapper, populator: Consumer<IData>) {
    val dataFiles = files(context) ?: return
    ObjectInputStream(context.openFileInput(dataFiles.fileName)).use {
        val serializer: Serializer? = if (dataFiles.serializers.requireVersionNumber)
            dataFiles.serializers.getVersion(it.readInt())
        else dataFiles.serializers.get()
        serializer?.load(context, populator, it)
    }
}

private fun files(context: ContextWrapper): DataFiles? {
    val matches = ArrayList<File>()
    var latest: DataFiles? = null

    DataFiles.values().forEach {
        val file = File(context.filesDir, it.fileName)
        if (file.exists()) {
            latest = it
            matches.add(file)
        }
    }

    if (matches.isNotEmpty())
        matches.removeLast()
    matches.forEach(File::delete)

    return latest
}

class Serializers(
    val serializers: Array<Pair<Int, Serializer>>,
    val requireVersionNumber: Boolean) {

    fun getVersion(version: Int): Serializer? {
        serializers.forEach {
            if (it.first == version)
                return it.second
        }
        return null
    }

    fun get(): Serializer? {
        return if (serializers.isEmpty()) null else serializers.last().second
    }
}

private fun newSerializers(serializers: Array<out Serializer>): Serializers {
    val version = nextVersion()
    val req = version != 0 || serializers.size > 1
    val versions = Arrays.stream(serializers)
        .map { Pair(version, it)}
        .toList()
    return Serializers(versions.toTypedArray(), req)
}
