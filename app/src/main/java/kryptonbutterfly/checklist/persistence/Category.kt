package kryptonbutterfly.checklist.persistence

import com.google.gson.annotations.Expose
import java.io.Serializable

data class Category(
	@Expose val id: Long,
	@Expose var name: String,
	@Expose var icon: String?): Serializable {
	constructor(data: Data, name: String, icon: String?) : this(data.genCategoryID(), name, icon)
}
