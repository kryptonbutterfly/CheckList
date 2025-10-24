package kryptonbutterfly.checklist.misc

fun <T> ArrayList<T>.swap(i: Int, k: Int): ArrayList<T> {
	if (i == k)
		return this
	val listRange = IntRange(0, this.size)
	if (i !in listRange)
		throw ArrayIndexOutOfBoundsException("i = '$i' is out of bounds $listRange")
	if (k !in listRange)
		throw ArrayIndexOutOfBoundsException("k = '$k' is out of bounds $listRange")
	val tmp = this[i]
	this[i] = this[k]
	this[k] = tmp
	return this
}
