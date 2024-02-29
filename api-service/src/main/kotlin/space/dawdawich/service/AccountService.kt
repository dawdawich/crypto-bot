package space.dawdawich.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import space.dawdawich.model.constants.Market
import space.dawdawich.repositories.mongo.AccountRepository
import space.dawdawich.repositories.mongo.ApiAccessTokenRepository
import space.dawdawich.repositories.mongo.entity.AccountDocument
import space.dawdawich.repositories.mongo.entity.ApiAccessTokenDocument
import java.util.*
import kotlin.time.Duration.Companion.days

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val apiAccessTokenRepository: ApiAccessTokenRepository
) {

    fun requestSalt(address: String): Long {
        val salt = (System.currentTimeMillis() / 1000) + 1.days.inWholeMilliseconds
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
