package space.dawdawich.configuration.model

import org.springframework.security.core.userdetails.User

class WalletUserDetails(address: String, signature: String, val salt: Long) :
    User(address, signature, listOf()) {
    val address: String get() = username
    val signature: String get() = password
}
