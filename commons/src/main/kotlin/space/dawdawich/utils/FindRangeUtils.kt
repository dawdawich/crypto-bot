package space.dawdawich.utils


fun findLowestRange(list: List<Double>): List<Double> = findRange(list.reversed())
fun findLargestRange(list: List<Double>): List<Double> = findRange(list)

private fun createTree(groups: MutableList<Group>) {
    val stack = mutableListOf<Group>()
    for (i in 0 until groups.size) {
        val current = groups[i]
        if (stack.isEmpty()) {
            stack.add(current)
        }
        var index = 0
        var value = Int.MAX_VALUE
        for (j in stack.indices) {
            if (stack[j].depth < value) {
                value = stack[j].depth
                index = j
            }
            if (current.value > stack[j].value) {
                current.parent = stack[j]
                current.depth += stack[j].depth
                stack.add(index, current)
                break
            }
            if (j == stack.size - 1) {
                stack.add(current)
                break
            }
        }
    }
}

private fun findRange(listOfDoubles: List<Double>): List<Double> {
    val groups = mutableListOf<Group>()
    for (i in listOfDoubles.indices) {
        val num = listOfDoubles[i]
        groups.add(Group(num, 1, i - 1))
    }

    createTree(groups)

    val g = groups.maxByOrNull { it.depth }

    val list = mutableListOf<Double>()
    val index = mutableListOf<Int>()

    var current = g
    while (current != null && current != current.parent) {
        list.add(listOfDoubles[current.index + 1])
        index.add(current.index + 1)
        current = current.parent
    }
    return list
}

private data class Group(val value: Double, var depth: Int, val index: Int, var parent: Group? = null)