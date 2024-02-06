package space.dawdawich.configuration.model

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

class WalletAuthenticationRequest(address: String, signature: String) : UsernamePasswordAuthenticationToken(address, signature) {
    init {
        isAuthenticated = false
    }

    val address: String get() = principal.toString()
    val signature: String get() = credentials.toString()
}
