package space.dawdawich.configuration.filter

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.web.authentication.AuthenticationFilter
import space.dawdawich.configuration.model.WalletAuthenticationRequest
import space.dawdawich.utils.baseDecode

class WalletAuthenticationFilter(authenticationManager: AuthenticationManager) :
    AuthenticationFilter(authenticationManager, { request ->
        val address = request.getHeader("Account-Address")?.baseDecode() ?: ""
        val signature = request.getHeader("Account-Address-Signature")?.baseDecode() ?: ""
        WalletAuthenticationRequest(address, signature)
    })
