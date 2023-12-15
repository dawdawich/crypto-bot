package space.dawdawich.configuration.provider.model

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

class JwtAuthenticationToken(private val token: JWT, private val principal: AccountDetails?) : Authentication {
    override fun getName(): String  = principal?.accountId ?: "id"

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = mutableListOf(GrantedAuthority { principal?.role ?: "USER" })

    override fun getCredentials(): Any = token

    override fun getDetails(): Any? = null

    override fun getPrincipal(): Any? = principal

    override fun isAuthenticated(): Boolean = principal != null

    override fun setAuthenticated(isAuthenticated: Boolean) {
        this.isAuthenticated = isAuthenticated
    }
}
