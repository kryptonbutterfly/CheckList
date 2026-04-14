package kryptonbutterfly.checklist.actions

import com.google.gson.*
import com.google.gson.annotations.Expose
import kryptonbutterfly.checklist.Constants.JSON_DATA
import kryptonbutterfly.checklist.Constants.JSON_TYPE
import java.io.Serializable
import java.lang.reflect.Type
import java.util.*

sealed class Action<I>: Serializable {
    abstract fun inverse(): I?

    override fun toString(): String {
        return "${this::class.simpleName}=(${
            Arrays.stream(this::class.java.declaredFields)
                .filter { f -> f.isAnnotationPresent(Expose::class.java) }
                .map { f ->
                    val value = f.get(this)
                    val name = f.name
                    return@map if (CharSequence::class.java.isAssignableFrom(f.type))
                        "$name='$value'"
                    else
                        "$name=$value"
                }
                .reduce { a, b -> "$a, $b" }
        })"
    }
}

data class CreateTask(
    @Expose val description: String,
    @Expose val listName: String,
    @Expose val category: Long,
    @Expose val index: Int) :
    Action<DeleteTask>() {
    override fun inverse(): DeleteTask {
        return DeleteTask(description, listName, category, index)
    }
}

data class ChangeTask(
    @Expose val descOld: String,
    @Expose val catOld: Long,
    @Expose val indOld: Int,
    @Expose val descNew: String,
    @Expose val catNew: Long,
    @Expose val indNew: Int,
    @Expose val listName: String) :
    Action<ChangeTask>() {
    override fun inverse(): ChangeTask {
        return ChangeTask(descNew, catNew, indNew, descOld, catOld, indOld, listName)
    }
}

data class MoveTask(
    @Expose val old: Int,
    @Expose val new: Int,
    @Expose val description: String,
    @Expose val categoryID: Long,
    @Expose val listName: String) :
    Action<MoveTask>() {
    override fun inverse(): MoveTask {
        return MoveTask(new, old, description, categoryID, listName)
    }
}

data class DeleteTask(
    @Expose val description: String,
    @Expose val listName: String,
    @Expose val category: Long,
    @Expose val index: Int) :
    Action<CreateTask>() {
    override fun inverse(): CreateTask {
        return CreateTask(description, listName, category, index)
    }
}

data class DeleteAll(
    @Expose val count: Int) :
    Action<Unit>() {
    override fun inverse() {}
}

object ActionAdapter: JsonSerializer<Action<*>>, JsonDeserializer<Action<*>> {
    override fun serialize(
        src: Action<*>?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        if (src == null)
            throw IllegalStateException("src was unexpectedly 'null'")
        if (context == null)
            throw IllegalStateException("context was unexpectedly 'null'")
        val jsonObject = JsonObject()
        jsonObject.addProperty(JSON_TYPE, src::class.java.canonicalName)
        jsonObject.add(JSON_DATA, context.serialize(src))
        return jsonObject
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Action<*> {
        if (json == null)
            throw IllegalStateException("json was unexpectedly 'null'")
        if (context == null)
            throw IllegalStateException("context was unexpectedly 'null'")
        val jsonObject = json.asJsonObject
        val type = jsonObject.get(JSON_TYPE).asString
        return context.deserialize(jsonObject.get(JSON_DATA), Class.forName(type))
    }
}
