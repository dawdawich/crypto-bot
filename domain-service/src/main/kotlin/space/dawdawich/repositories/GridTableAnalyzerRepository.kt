package space.dawdawich.repositories

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument

interface GridTableAnalyzerRepository : MongoRepository<GridTableAnalyzerDocument, String> {

    @Query("{_id:  ?0}")
    @Update("{\$set:  {isActive:  ?1, middlePrice: null, updateTime:  ?2}}")
    fun setAnalyzerActiveStatus(id: String, status: Boolean, updateTime: Long = System.currentTimeMillis())

    @Query("{accountId:  ?0}")
    @Update("{\$set:  {isActive:  ?1, middlePrice: null, updateTime:  ?2}}")
    fun setAllAnalyzersActiveStatus(accountId: String, status: Boolean, updateTime: Long = System.currentTimeMillis())

    fun deleteByAccountId(accountId: String)

    fun deleteByIdAndAccountId(id: String, accountId: String)

    fun countByAccountId(accountId: String): Int

    fun findByIdAndAccountId(id: String, accountId: String): GridTableAnalyzerDocument?

    fun existsByIdAndAccountId(id: String, accountId: String): Boolean

    fun findAllByAccountId(accountId: String, pageable: Pageable): List<GridTableAnalyzerDocument>

    fun findAllByPublic(public: Boolean = true): List<GridTableAnalyzerDocument>
}
