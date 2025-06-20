package kryptonbutterfly.checklist

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kryptonbutterfly.checklist.actions.Action
import kryptonbutterfly.checklist.actions.ActionAdapter

object Constants
{
    const val TEXT_COLUMN = 0
    const val CREATE_TASK = "CREATE TASK"
    const val MOVE_TASK = "MOVE TASK"

    const val MSG_DELETE_ALL_TASKS = "Delete all Tasks?"
    const val TEXT_DELETE = "Delete"
    const val TEXT_CANCEL = "Cancel"

    const val ACTION = "ACTION"
    const val INDEX = "INDEX"
    const val DESCRIPTION = "DESCRIPTION"

    const val SETTINGS = "SETTINGS"

    const val JSON_TYPE = ":type"
    const val JSON_DATA = "data"

    const val HTML_PREFIX = "<p dir=\"ltr\">"
    const val HTML_POSTFIX = "</p>\n"

    val GSON: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Action::class.java, ActionAdapter)
        .create()
}