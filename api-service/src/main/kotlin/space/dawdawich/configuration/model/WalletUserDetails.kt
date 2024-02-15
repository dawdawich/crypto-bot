package space.dawdawich.configuration.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class WalletUserDetails(address: String, signature: String, authorities: List<GrantedAuthority>, val salt: Long) :
    User(address, signature, authorities) {
    val address: String get() = username
    val signature: String get() = password
}
