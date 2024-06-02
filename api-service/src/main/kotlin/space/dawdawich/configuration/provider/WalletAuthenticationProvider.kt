package space.dawdawich.configuration.provider

import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import space.dawdawich.configuration.model.WalletAuthenticationRequest
import space.dawdawich.configuration.model.WalletUserDetails
import space.dawdawich.repositories.mongo.AccountRepository
import java.security.SignatureException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Authentication provider for wallet accounts.
 *
 * This class is responsible for authenticating wallet user credentials and retrieving user details.
 *
 * @property accountRepository The repository for accessing wallet accounts.
 */
@Component
class WalletAuthenticationProvider(private val accountRepository: AccountRepository) :
    AbstractUserDetailsAuthenticationProvider() {

    val log = KotlinLogging.logger {}

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneOffset.UTC)

    /**
     * Performs additional authentication checks during the authentication process.
     *
     * @param userDetails The user details object that contains the wallet user details.
     * @param authentication The authentication token.
     * @throws BadCredentialsException if the signature is not valid.
     * @throws CredentialsExpiredException if the salt has expired.
     * @throws BadCredentialsException if the signature size is invalid.
     */
    override fun additionalAuthenticationChecks(
        userDetails: UserDetails?,
        authentication: UsernamePasswordAuthenticationToken,
    ) {
        try {
            val walletUserDetails = userDetails as WalletUserDetails?
            if (walletUserDetails == null
                || !isSignatureValid(walletUserDetails.address, walletUserDetails.signature, walletUserDetails.salt)
            ) {
                log.debug("Authentication failed: signature is not valid")
                throw BadCredentialsException("Signature is not valid")
            } else if (walletUserDetails.salt > System.currentTimeMillis()) {
                throw CredentialsExpiredException("Salt has expired")
            }
        } catch (ex: ArrayIndexOutOfBoundsException) {
            log.debug { "Signature size is invalid" }
            throw BadCredentialsException("Signature is not valid")
        }
    }

    /**
     * Retrieves the UserDetails for the given username and authentication token.
     *
     * @param username The username of the user to retrieve.
     * @param authentication The authentication token containing the user's address and signature.
     * @return The UserDetails object for the user, or null if no user is found.
     */
    override fun retrieveUser(username: String, authentication: UsernamePasswordAuthenticationToken?): UserDetails? =
        (authentication as WalletAuthenticationRequest?)?.let { auth ->
            accountRepository.findByIdOrNull(auth.address)?.let { account ->
                WalletUserDetails(
                    auth.address,
                    auth.signature,
                    AuthorityUtils.createAuthorityList(account.role),
                    account.saltValidUntil
                )
            }
        }

    /**
     * Checks if the signature is valid for the given address, signature, and salt.
     *
     * @param address The address of the wallet account.
     * @param signature The signature associated with the wallet account.
     * @param salt The salt value associated with the wallet account.
     * @return true if the signature is valid, false otherwise.
     */
    private fun isSignatureValid(address: String, signature: String, salt: Long): Boolean {
        val formattedSalt =
            "Signature will be valid until:\n${formatter.format(Instant.ofEpochSecond(salt))}"

        val signatureBytes = Numeric.hexStringToByteArray(signature)
        var v: Byte = signatureBytes[64]
        if (v < 27) {
            v = (v + 27).toByte()
        }
        val r = Arrays.copyOfRange(signatureBytes, 0, 32)
        val s = Arrays.copyOfRange(signatureBytes, 32, 64)

        val data = Sign.SignatureData(v, r, s)

        return try {
            Sign.signedPrefixedMessageToKey(formattedSalt.toByteArray(), data)
                .let { publicKey -> "0x${Keys.getAddress(publicKey)}" }
                .let { restoredAddress -> address.equals(restoredAddress, ignoreCase = true) }
        } catch (e: SignatureException) {
            log.debug("Failed to recover public key")
            false
        }
    }


}
