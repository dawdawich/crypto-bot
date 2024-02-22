package space.dawdawich.repositories.custom

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import space.dawdawich.repositories.custom.model.AnalyzerFilter
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument

interface CustomAnalyzerRepository {
    fun findAnalyzersFilteredAndSorted(accountId: String, analyzerIds: List<String>?, page: Pageable, filter: AnalyzerFilter, sort: Sort? = null): List<GridTableAnalyzerDocument>
}
