package kryptonbutterfly.checklist.persistence

import kryptonbutterfly.checklist.DeletedTask

interface IData {
    val tasks: ArrayList<String>
    val deleted: ArrayList<DeletedTask>
}
