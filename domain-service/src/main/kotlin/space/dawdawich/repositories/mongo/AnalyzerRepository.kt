package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.custom.CustomAnalyzerRepository
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument

interface AnalyzerRepository : MongoRepository<GridTableAnalyzerDocument, String>, CustomAnalyzerRepository {

    @Query("{_id:  {\$in: ?0}}")
    @Update("{\$set:  {isActive: ?1, middlePrice: null, updateTime:  ?2}}")
    fun setAnalyzersActiveStatus(id: List<String>, status: Boolean, updateTime: Long = System.currentTimeMillis())

    @Query("{_id:  {\$in: ?0}}")
    @Update("{\$set:  {\"money\": null, middlePrice: null, pNl1: null, pNl12: null, pNl24: null, stabilityCoef: null, updateTime:  ?1}}")
    fun resetAnalyzers(ids: List<String>, updateTime: Long = System.currentTimeMillis())

    fun deleteByIdIn(ids: List<String>)

    fun countByAccountId(accountId: String): Int
    fun countByAccountIdAndSymbolInfoSymbolIn(accountId: String, symbols: List<String>): Int

    fun countByAccountIdAndIsActive(accountId: String, isActive: Boolean = true): Int

    fun countByAccountIdAndSymbolInfoSymbolInAndIsActive(accountId: String, symbols: List<String>, isActive: Boolean = true): Int


    fun findByIdAndAccountId(id: String, accountId: String): GridTableAnalyzerDocument?

    fun existsByIdAndAccountId(id: String, accountId: String): Boolean
    fun existsByIdInAndAccountId(id: List<String>, accountId: String): Boolean

    fun findAllByPublic(public: Boolean = true): List<GridTableAnalyzerDocument>
}
