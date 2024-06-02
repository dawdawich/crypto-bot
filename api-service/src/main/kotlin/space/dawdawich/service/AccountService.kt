package space.dawdawich.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import space.dawdawich.model.constants.Market
import space.dawdawich.repositories.mongo.AccountRepository
import space.dawdawich.repositories.mongo.ApiAccessTokenRepository
import space.dawdawich.repositories.mongo.ManagerRepository
import space.dawdawich.repositories.mongo.entity.AccountDocument
import space.dawdawich.repositories.mongo.entity.ApiAccessTokenDocument
import java.util.*
import kotlin.time.Duration.Companion.days

/**
 * Service class for handling account-related operations.
 *
 * @constructor Creates an instance of AccountService with the specified repositories.
 * @param accountRepository The repository for accessing and manipulating AccountDocument objects.
 * @param apiAccessTokenRepository The repository for accessing and manipulating ApiAccessTokenDocument objects.
 * @param managerRepository The repository for accessing and manipulating TradeManagerDocument objects.
 */
@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val apiAccessTokenRepository: ApiAccessTokenRepository,
    private val managerRepository: ManagerRepository
) {

    /**
     * Requests a salt for account authentication.
     *
     * @param address The address of the account.
     * @return The salt valid until the current time in milliseconds.
     */
    fun requestSalt(address: String): Long {
        val salt = (System.currentTimeMillis() / 1000) + 1.days.inWholeMilliseconds
        return accountRepository.save(
            accountRepository.findByIdOrNull(address)?.apply { saltValidUntil = salt }
                ?: AccountDocument(address, salt)
        ).saltValidUntil
    }

    /**
     * Retrieves the API tokens associated with a given account ID.
     *
     * @param accountId The ID of the account.
     * @return A list of API tokens associated with the account.
     */
    fun getApiTokens(accountId: String) = apiAccessTokenRepository.findAllByAccountId(accountId)

    /**
     * Adds an API token for a specific account.
     *
     * @param accountId The ID of the account.
     * @param apiKey The API key for the token.
     * @param secretKey The secret key for the token.
     * @param market The market for which the token is generated.
     * @param test Specifies whether the token is for a test account or not.
     * @return The ID of the added API token.
     */
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

    /**
     * Deletes an API token associated with a specific account. If the token is deleted successfully,
     * all managers assigned with this token will also be deleted.
     *
     * @param tokenId The ID of the API token to be deleted.
     * @param accountId The ID of the account.
     */
    fun deleteApiToken(tokenId: String, accountId: String) {
        // Api Token was existed, also need to delete managers assigned with this token
        if (apiAccessTokenRepository.deleteByIdAndAccountId(tokenId, accountId) > 0) {
            managerRepository.deleteAllByApiTokenId(tokenId)
        }
    }
}
