package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.custom.mongo.CustomAnalyzerRepository
import space.dawdawich.repositories.mongo.entity.AnalyzerDocument
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument

interface AnalyzerRepository : MongoRepository<AnalyzerDocument, String>, CustomAnalyzerRepository {

    @Query("{_id:  {\$in: ?0}}")
    @Update("{\$set:  {isActive: ?1, middlePrice: null, updateTime:  ?2}}")
    fun setAnalyzersActiveStatus(id: List<String>, status: Boolean, updateTime: Long = System.currentTimeMillis())

    @Query("{_id:  {\$in: ?0}}")
    @Update("{\$set:  {\"money\": null, middlePrice: null, pNl1: null, pNl12: null, pNl24: null, stabilityCoef: null, updateTime:  ?1}}")
    fun resetAnalyzers(ids: List<String>, updateTime: Long = System.currentTimeMillis())

    /**
     * Resets all analyzers with the provided IDs by setting various fields to null and updating the updateTime.
     *
     * @param accountId of analyzers needed to reset.
     * @param ids The list of analyzer IDs to exclude of reset.
     * @param updateTime The timestamp of the update. Default is the current system time.
     */
    @Query("{accountId:  ?0, _id:  {\$nin: ?1}}")
    @Update("{\$set:  {\"money\": null, middlePrice: null, pNl1: null, pNl12: null, pNl24: null, stabilityCoef: null, updateTime:  ?1}}")
    fun resetAllAnalyzers(accountId: String, ids: List<String>, updateTime: Long = System.currentTimeMillis())

    fun findAllByAccountIdAndIdNotIn(accountId: String, ids: Collection<String>): List<AnalyzerDocument>

    fun deleteByIdIn(ids: List<String>)

    fun countByAccountId(accountId: String): Int
    fun countByAccountIdAndSymbolInfoSymbolIn(accountId: String, symbols: List<String>): Int

    fun countByAccountIdAndIsActive(accountId: String, isActive: Boolean = true): Int

    fun countByAccountIdAndSymbolInfoSymbolInAndIsActive(accountId: String, symbols: List<String>, isActive: Boolean = true): Int


    fun findByIdAndAccountId(id: String, accountId: String): AnalyzerDocument?

    fun existsByIdAndAccountId(id: String, accountId: String): Boolean
    fun existsByIdInAndAccountId(id: List<String>, accountId: String): Boolean

    fun findAllByPublic(public: Boolean = true): List<AnalyzerDocument>

    fun findAllByAccountIdAndPublic(accountId: String, public: Boolean): List<AnalyzerDocument>

    @Aggregation(pipeline = [
        "{accountId: ?0, demoAccount:  ?1, market:  ?2, isActive:  true}",
        "{ \$sort: { pNl10M: -1 } }",
        "{ \$limit: 1 }"
    ])
    fun findMoreProfitableByLast10Minutes(accountId: String, isDemo: Boolean, market: String): AnalyzerDocument
}
