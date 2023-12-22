package space.dawdawich.configuration.provider

import kotlinx.serialization.json.Json
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import space.dawdawich.configuration.provider.model.JWT
import space.dawdawich.configuration.provider.model.JwtAuthenticationToken
import space.dawdawich.configuration.provider.model.AccountDetails
import space.dawdawich.utils.baseDecode
import javax.crypto.Mac

@Component
open class JwtAuthenticationProvider(private val encryptor: Mac) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication {
        val jwt = authentication.credentials as JWT

        if (checkJwtSignature(jwt)) {
            return JwtAuthenticationToken(jwt, Json.decodeFromString<AccountDetails>(jwt.payload.baseDecode()))
        }
        throw BadCredentialsException("Provided JWT has invalid signature")
    }

    override fun supports(authentication: Class<*>?): Boolean = authentication?.let { JwtAuthenticationToken::class.java.isAssignableFrom(it) } ?: false

    private fun checkJwtSignature(jwt: JWT): Boolean {
        val result = jwt.signature.baseDecode() == String(encryptor.doFinal("${jwt.headers}.${jwt.payload}".toByteArray()))
        encryptor.reset()
        return result
    }
}
