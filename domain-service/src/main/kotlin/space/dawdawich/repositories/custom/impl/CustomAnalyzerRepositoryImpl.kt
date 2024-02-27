package space.dawdawich.repositories.custom.impl

import org.bson.Document
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.LookupOperation
import org.springframework.data.mongodb.core.aggregation.MatchOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import space.dawdawich.repositories.custom.CustomAnalyzerRepository
import space.dawdawich.repositories.custom.model.AnalyzerFilter
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument

class CustomAnalyzerRepositoryImpl(private val mongoTemplate: MongoTemplate) : CustomAnalyzerRepository {
    override fun findAnalyzersFilteredAndSorted(
        accountId: String,
        analyzerIds: List<String>?,
        page: Pageable,
        filter: AnalyzerFilter,
        sort: Sort?,
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

    override fun countActiveAnalyzersInFolder(folderId: String, status: Boolean?, symbols: List<String>): Int {
        val matchOperations = mutableListOf<MatchOperation>()
        val lookup = LookupOperation.newLookup()
            .from("grid_table_analyzer")
            .localField("analyzerId")
            .foreignField("_id")
            .`as`("analyzer")
        matchOperations += Aggregation.match(Criteria.where("folderId").isEqualTo(folderId))
        status?.let {
            matchOperations += Aggregation.match(Criteria.where("analyzer.isActive").isEqualTo(it))
        }
        if (symbols.isNotEmpty()) {
            matchOperations += Aggregation.match(Criteria.where("analyzer.symbolInfo._id").`in`(symbols))
        }
        val count = Aggregation.count().`as`("total")
        val aggregation = Aggregation.newAggregation(lookup, *matchOperations.toTypedArray(), count)

        return mongoTemplate.aggregate(
                aggregation,
                "folder_analyzer",
                Document::class.java
            ).uniqueMappedResult?.getInteger("total") ?: 0
    }


}
