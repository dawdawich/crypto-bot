package space.dawdawich.repositories.custom.redis.impl

import org.springframework.data.redis.core.RedisTemplate
import space.dawdawich.repositories.custom.redis.CustomAnalyzerStabilityRepository
import space.dawdawich.repositories.redis.entity.AnalyzerMoneyModel

class CustomAnalyzerStabilityRepositoryImpl(private val redisTemplate: RedisTemplate<String, AnalyzerMoneyModel>) :
    CustomAnalyzerStabilityRepository {

    override fun deleteByAnalyzerIds(ids: Set<String>) {
//        ids.forEach { id ->
//
//            // Find all keys by pattern
//            val keys = redisTemplate.
//
//
//            // Delete all found keys
//            if (keys.isNotEmpty()) {
//                redisTemplate.delete(keys)
//            }
//        }
    }
}
