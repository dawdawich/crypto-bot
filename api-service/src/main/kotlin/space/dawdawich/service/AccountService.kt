package space.dawdawich.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import space.dawdawich.repositories.AccountRepository
import space.dawdawich.repositories.ApiAccessTokenRepository
import space.dawdawich.repositories.entity.AccountDocument
import space.dawdawich.repositories.entity.ApiAccessTokenDocument
import space.dawdawich.repositories.entity.constants.Market
import java.util.UUID

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val apiAccessTokenRepository: ApiAccessTokenRepository
) {

    fun requestSalt(address: String): Long {
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
