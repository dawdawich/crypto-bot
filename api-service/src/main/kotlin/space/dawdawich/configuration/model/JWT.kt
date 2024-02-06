package space.dawdawich.configuration.model

class JWT(val headers: String, val payload: String, val signature: String) {
    companion object {
        fun parseJwt(jwt: String): JWT {
            with(jwt.split(".")) {
                if (this.size == 3) {
                    return JWT(this[0], this[1], this[2])
                }
                throw IllegalArgumentException()
            }
        }
    }

    override fun toString(): String {
        return "$headers.$payload.$signature"
    }
}
