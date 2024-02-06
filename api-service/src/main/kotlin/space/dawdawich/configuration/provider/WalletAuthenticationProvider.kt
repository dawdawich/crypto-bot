package space.dawdawich.configuration.provider

import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import space.dawdawich.configuration.model.WalletAuthenticationRequest
import space.dawdawich.configuration.model.WalletUserDetails
import space.dawdawich.repositories.AccountRepository
import java.security.SignatureException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class WalletAuthenticationProvider(private val accountRepository: AccountRepository) :
    AbstractUserDetailsAuthenticationProvider() {

    val log = KotlinLogging.logger {}

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneOffset.UTC)

    override fun additionalAuthenticationChecks(userDetails: UserDetails?, authentication: UsernamePasswordAuthenticationToken) {
        val walletUserDetails = userDetails as WalletUserDetails?
        if (walletUserDetails == null || !isSignatureValid(walletUserDetails.address, walletUserDetails.signature, walletUserDetails.salt)) {
            log.debug("Authentication failed: signature is not valid");
            throw BadCredentialsException("Signature is not valid");
        }
    }

    override fun retrieveUser(username: String, authentication: UsernamePasswordAuthenticationToken?): UserDetails? =
        (authentication as WalletAuthenticationRequest?)?.let { auth ->
            accountRepository.findByIdOrNull(auth.address)?.let { account ->
                WalletUserDetails(auth.address, auth.signature, account.saltValidUntil)
            }
        }

    private fun isSignatureValid(address: String, signature: String, salt: Long): Boolean {
        val formattedNonce =
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
            Sign.signedPrefixedMessageToKey(formattedNonce.toByteArray(), data)
                .let { publicKey -> "0x${Keys.getAddress(publicKey)}" }
                .let { restoredAddress -> address.equals(restoredAddress, ignoreCase = true) }
        } catch (e: SignatureException) {
            log.debug("Failed to recover public key")
            false
        }
    }


}
