package space.dawdawich.repositories.custom.redis

interface CustomAnalyzerStabilityRepository {
    fun deleteByAnalyzerIds(ids: Set<String>)
}
