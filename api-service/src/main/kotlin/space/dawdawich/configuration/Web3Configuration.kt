package space.dawdawich.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

@Configuration
class Web3Configuration {

    @Bean
    @Profile("!prod")
    fun testWeb3Client() = Web3j.build(HttpService("https://sepolia.infura.io/v3/833bc9ae0e7a4d899cb8a78de45081ed"))!!

    @Bean
    @Profile("prod")
    fun web3Client() = Web3j.build(HttpService("https://mainnet.infura.io/v3/833bc9ae0e7a4d899cb8a78de45081ed"))!!
}
