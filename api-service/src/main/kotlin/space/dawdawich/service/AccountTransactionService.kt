package space.dawdawich.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.Transaction
import space.dawdawich.controller.model.account.transactions.TransactionResponse
import space.dawdawich.repositories.mongo.AccountTransactionRepository
import space.dawdawich.repositories.mongo.ServerConfigRepository
import space.dawdawich.repositories.mongo.entity.AccountTransactionDocument
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@Service
class AccountTransactionService(
    private val accountTransactionRepository: AccountTransactionRepository,
    private val serverConfigRepository: ServerConfigRepository,
    private val web3Client: Web3j,
    @Value("\${app.joat-token-address}") private val joatContractAddress: String
) {

    companion object {
        const val SHIFT_TX_INPUT_VALUE = 10
        const val TOKEN_DECIMALS = 18
        const val ADMIN_WALLET_ADDRESS = "0xe6acDe7D649bdD633A68EAD07b9E8A3FB907b36a"

        val transactionMetaInfo = Function(
            "transfer",
            mutableListOf<Type<*>>(),
            listOf(
                TypeReference.create(Address::class.java),
                TypeReference.create(Uint256::class.java),
            )
        )
    }

    fun getTransactions(accountId: String): List<TransactionResponse> =
        accountTransactionRepository.findByAccountId(accountId).map {
            TransactionResponse(it.value, it.time)
        }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    private fun checkNewTransactions() {
        val lastCheckedBlock: Long = serverConfigRepository.getConfig().lastCheckedBlock + 1
        val lastBlock = web3Client.ethBlockNumber().send().blockNumber.longValueExact()

        if (lastCheckedBlock >= (lastBlock + 1)) {
            return
        }

        val result = mutableListOf<AccountTransactionDocument>()

        for (blockIndex in lastCheckedBlock..lastBlock) {
            result += web3Client.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(BigInteger.valueOf(blockIndex)),
                true
            )
                .send().block.let { block ->
                    block.transactions
                        .asSequence()
                        .filterIsInstance<Transaction>()
                        .filter { tx -> joatContractAddress.equals(tx.to, true) }
                        .filter { tx -> web3Client.ethGetTransactionReceipt(tx.hash).send().transactionReceipt.isPresent }
                        .map { tx ->
                            tx to FunctionReturnDecoder.decode(
                                tx.input.substring(SHIFT_TX_INPUT_VALUE),
                                transactionMetaInfo.outputParameters
                            )
                        }
                        .filter { pair ->
                            ADMIN_WALLET_ADDRESS.equals(pair.second[0].toString(), true)
                        }
                        .filter { pair -> (pair.second[1] as Uint256).value.toLong() > 0 }
                        .map { pair ->
                            val valueSent = (pair.second[1] as Uint256).value.divide(
                                BigInteger.TEN.pow(
                                    TOKEN_DECIMALS
                                )
                            ).toInt()
                            AccountTransactionDocument(pair.first.from, valueSent, block.timestamp.toLong())
                        }
                        .toList()
                }
        }

        if (result.isNotEmpty()) {
            accountTransactionRepository.saveAll(result)
        }
        serverConfigRepository.updateLastCheckedBlock(lastBlock)
    }
}
