package space.dawdawich.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

/**
 * Configuration class for Web3.
 *
 * @param blockchainAddress The URL of the blockchain address.
 */
@Configuration
class Web3Configuration(@Value("\${app.blockchain-address}") private val blockchainAddress: String) {

    /**
     * Returns an instance of the web3 client.
     *
     * This method is annotated with `@Bean` to indicate that it is a Spring bean, and it is responsible for creating and configuring
     * the web3 client instance.
     *
     * To create the web3 client instance, the method uses the `Web3j.build(HttpService)` method, passing in an `HttpService` instance
     * initialized with the `blockchainAddress` parameter. The `HttpService` is responsible for providing an HTTP connection to the
     * blockchain address.
     *
     * The method returns the created web3 client instance.
     *
     * @return an instance of the web3 client.
     */
    @Bean
    fun web3Client() = Web3j.build(HttpService(blockchainAddress))!!
}
