package space.dawdawich.service

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import space.dawdawich.configuration.model.JWT
import space.dawdawich.repositories.AccountRepository
import space.dawdawich.repositories.ApiAccessTokenRepository
import space.dawdawich.repositories.entity.AccountDocument
import space.dawdawich.repositories.entity.ApiAccessTokenDocument
import space.dawdawich.repositories.entity.constants.Market
import space.dawdawich.utils.baseDecode
import space.dawdawich.utils.baseEncode
import java.util.UUID
import javax.crypto.Mac

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val apiAccessTokenRepository: ApiAccessTokenRepository
) {

    fun getTokens(accountId: String) = apiAccessTokenRepository.findAllByAccountId(accountId)

    fun requestNonce(address: String): Long {
        val salt = (System.currentTimeMillis() / 1000)
        return accountRepository.save(
            accountRepository.findByIdOrNull(address)?.apply { saltValidUntil = salt }
                ?: AccountDocument(address, salt)
        ).saltValidUntil
    }

    fun getApiTokens(accountId: String) = apiAccessTokenRepository.findAllByAccountId(accountId)

    fun addApiToken(accountId: String, apiKey: String, secretKey: String, market: String, test: Boolean): String {
        val id = UUID.randomUUID().toString()
        apiAccessTokenRepository.insert(
            ApiAccessTokenDocument(
                id,
                accountId,
                apiKey,
                secretKey,
                Market.valueOf(market),
                test
            )
        )
        return id
    }

    fun deleteApiToken(tokenId: String, accountId: String) =
        apiAccessTokenRepository.deleteByIdAndAccountId(tokenId, accountId)
}
