package space.dawdawich.repositories.custom.impl

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import space.dawdawich.repositories.custom.CustomAnalyzerRepository
import space.dawdawich.repositories.custom.model.AnalyzerFilter
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument

class CustomAnalyzerRepositoryImpl(private val mongoTemplate: MongoTemplate) : CustomAnalyzerRepository {
    override fun findAnalyzersFilteredAndSorted(
        accountId: String,
        analyzerIds: List<String>?,
        page: Pageable,
        filter: AnalyzerFilter,
        sort: Sort?
    ): List<GridTableAnalyzerDocument> {
        val query = Query()

        filter.statusFilter?.let { status ->
            query.addCriteria(Criteria.where("isActive").isEqualTo(status))
        }
        if (filter.symbolFilter.isNotEmpty()) {
            query.addCriteria(
                Criteria.where("symbolInfo._id").`in`(filter.symbolFilter)
            )
        }
        if (analyzerIds != null) {
            query.addCriteria(
                Criteria.where("id").`in`(analyzerIds)
            )
        }

        query.addCriteria(Criteria.where("accountId").isEqualTo(accountId))
        query.with(page)
        sort?.let { query.with(it) }

        return mongoTemplate.find(query, GridTableAnalyzerDocument::class.java)
    }


}
