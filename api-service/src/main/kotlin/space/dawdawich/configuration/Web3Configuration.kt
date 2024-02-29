package space.dawdawich.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

@Configuration
class Web3Configuration {

    @Bean
    fun web3Client() = Web3j.build(HttpService("https://sepolia.infura.io/v3/833bc9ae0e7a4d899cb8a78de45081ed"))
}
