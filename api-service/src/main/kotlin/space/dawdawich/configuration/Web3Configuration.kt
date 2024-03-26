package space.dawdawich.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

@Configuration
class Web3Configuration(@Value("\${app.blockchain-address}") private val blockchainAddress: String) {

    @Bean
    fun web3Client() = Web3j.build(HttpService(blockchainAddress))!!
}
