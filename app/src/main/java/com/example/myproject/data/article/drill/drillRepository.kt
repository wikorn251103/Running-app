package com.example.myproject.data.article.drill

import kotlinx.coroutines.flow.Flow

interface drillRepository {
    fun getdrillsFlow(): Flow<List<drillModel>>
}

class drillRepositoryImpl(
    private val drillService: drillService
) : drillRepository {
    override fun getdrillsFlow(): Flow<List<drillModel>> {
        return drillService.getdrillsFlow()
    }
}