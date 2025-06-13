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

data class CreateTask(@Expose val description: String, @Expose val index: Int) :
    Action<DeleteTask>() {
    override fun inverse(): DeleteTask {
        return DeleteTask(description, index)
    }
}

data class RenameTask(@Expose val old: String, @Expose val new: String, @Expose val index: Int) :
    Action<RenameTask>() {
    override fun inverse(): RenameTask {
        return RenameTask(new, old, index)
    }
}

data class MoveTask(@Expose val old: Int, @Expose val new: Int, @Expose val description: String) :
    Action<MoveTask>() {
    override fun inverse(): MoveTask {
        return MoveTask(new, old, description)
    }
}

data class DeleteTask(@Expose val description: String, @Expose val index: Int) :
    Action<CreateTask>() {
    override fun inverse(): CreateTask {
        return CreateTask(description, index)
    }
}

data class DeleteAll(@Expose val count: Int) : Action<Unit>() {
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
