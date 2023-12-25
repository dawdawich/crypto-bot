package space.dawdawich.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import org.springframework.data.repository.query.Param
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument

interface GridTableAnalyzerRepository : MongoRepository<GridTableAnalyzerDocument, String> {

    @Query("{_id:  ?0}")
    @Update("{\$set:  {isActive:  ?1, middlePrice: null, updateTime:  ?2}}")
    fun setAnalyzerActiveStatus(id: String, status: Boolean, updateTime: Long = System.currentTimeMillis())

    @Query("{accountId:  ?0}")
    @Update("{\$set:  {isActive:  ?1, middlePrice: null, updateTime:  ?2}}")
    fun setAllAnalyzersActiveStatus(accountId: String, status: Boolean, updateTime: Long = System.currentTimeMillis())

    fun deleteByAccountId(accountId: String)

    fun countByIdAndAccountId(id: String, accountId: String): Int

    fun findAllByOrderByMoneyDesc(pageable: Pageable = PageRequest.of(0, 20)): Page<GridTableAnalyzerDocument>

    fun findAllByAccountId(accountId: String): List<GridTableAnalyzerDocument>

    fun findAllByPublic(public: Boolean = true): List<GridTableAnalyzerDocument>
}
