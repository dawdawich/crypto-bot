package space.dawdawich.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import space.dawdawich.repositories.mongo.AccountRepository
import space.dawdawich.repositories.mongo.entity.AccountDocument
import kotlin.time.Duration.Companion.days

/**
 * Service class for handling account-related operations.
 *
 * @constructor Creates an instance of AccountService with the specified repositories.
 * @param accountRepository The repository for accessing and manipulating AccountDocument objects.
 */
@Service
class AccountService(
    private val accountRepository: AccountRepository,
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
}
