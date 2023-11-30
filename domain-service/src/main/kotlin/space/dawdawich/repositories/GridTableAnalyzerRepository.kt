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

    @Query("{'id' : ?0}")
    @Update("{'\$set': { 'money': ?1 }}")
    fun updateMoney(id: String, money: Double)

    fun findAllByOrderByMoneyDesc(pageable: Pageable = PageRequest.of(0, 20)): Page<GridTableAnalyzerDocument>
}
