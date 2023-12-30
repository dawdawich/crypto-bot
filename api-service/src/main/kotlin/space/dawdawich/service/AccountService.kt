package space.dawdawich.service

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import space.dawdawich.configuration.provider.model.JWT
import space.dawdawich.repositories.AccountRepository
import space.dawdawich.repositories.ApiAccessTokenRepository
import space.dawdawich.repositories.entity.ApiAccessTokenDocument
import space.dawdawich.repositories.entity.constants.Market
import space.dawdawich.utils.baseDecode
import space.dawdawich.utils.baseEncode
import java.util.UUID
import javax.crypto.Mac

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val apiAccessTokenRepository: ApiAccessTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val encryptor: Mac
) {

    companion object {
        val jwtHeader = "{ \"alg\": \"HS256\", \"typ\": \"JWT\"}".baseEncode()
    }

    fun isEmailAllowedToRegistration(email: String): Boolean = accountRepository.existsByEmail(email)

    fun isAccountAlreadyRegistered(email: String, username: String): Boolean = accountRepository.existsByEmailAndUsername(email, username)

    fun fillAccountInfo(email: String, username: String, name: String, surname: String, password: String) =
        accountRepository.fillAccountInfo(email, username, name, surname, passwordEncoder.encode(password))

    fun getAccountById(id: String) = accountRepository.findByIdOrNull(id)!!

    fun getTokens(accountId: String)  = apiAccessTokenRepository.findAllByAccountId(accountId)

    fun requestAccessToken(basicAuth: String): JWT {
        with(basicAuth.replace("Basic ", "").trim().baseDecode().split(":")) {
            if (this.size == 2) {
                accountRepository.findByEmail(this[0])?.let { account ->
                    if (passwordEncoder.matches(this[1], account.password)) {
                        return createJwt(
                            buildJsonObject {
                                put("accountId", account.id)
                                put("role", account.role)
                            }.toString()
                        )
                    }
                    throw BadCredentialsException("Provided password is incorrect")
                } ?: throw BadCredentialsException("Email did not provided")
            }
            throw BadCredentialsException("Authorization payload is not correct or has invalid format")
        }
    }

    fun getApiTokens(accountId: String) = apiAccessTokenRepository.findAllByAccountId(accountId)

    fun addApiToken(accountId: String, apiKey: String, secretKey: String, market: String, test: Boolean): String {
        val id = UUID.randomUUID().toString()
        apiAccessTokenRepository.insert(ApiAccessTokenDocument(
            id,
            accountId,
            apiKey,
            secretKey,
            Market.valueOf(market),
            test
        ))
        return id
    }

    fun deleteApiToken(tokenId: String, accountId: String) = apiAccessTokenRepository.deleteByIdAndAccountId(tokenId, accountId)

    private fun createJwt(payload: String): JWT {
        val encodedPayload = payload.baseEncode()
        val signature = encryptor.doFinal("$jwtHeader.$encodedPayload".toByteArray()).baseEncode()

        return JWT(jwtHeader, encodedPayload, signature)
    }
}
