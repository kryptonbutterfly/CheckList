package kryptonbutterfly.checklist.persistence

import kryptonbutterfly.checklist.DeletedTask

class Data: IData {
    override val tasks: ArrayList<String> = ArrayList()
    override val deleted: ArrayList<DeletedTask> = ArrayList()
}
