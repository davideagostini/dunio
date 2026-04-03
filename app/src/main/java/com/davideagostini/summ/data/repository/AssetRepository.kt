package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.dao.AssetDao
import com.davideagostini.summ.data.entity.Asset
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Repository facade for assets and asset history.
 *
 * The assets feature mostly works with month-scoped history snapshots, while export still needs
 * access to the full history stream. Both entry points live here behind a small API surface.
 */
class AssetRepository @Inject constructor(
    private val assetDao: AssetDao,
) {
    val allAssetHistory: Flow<List<AssetHistoryEntry>> = assetDao.getAllAssetHistory()
    fun observeAssetHistoryForMonth(period: String): Flow<List<AssetHistoryEntry>> =
        assetDao.getAssetHistoryForMonth(period)
    fun observeHasAnyAssetHistory(): Flow<Boolean> = assetDao.getHasAnyAssetHistory()

    suspend fun insert(asset: Asset) = assetDao.insert(asset)
    suspend fun update(asset: Asset) = assetDao.update(asset)
    suspend fun delete(asset: Asset) = assetDao.delete(asset)
}
