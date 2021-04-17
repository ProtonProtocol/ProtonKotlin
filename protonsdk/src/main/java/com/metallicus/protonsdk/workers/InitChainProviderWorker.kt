/*
 * Copyright (c) 2021 Proton Chain LLC, Delaware
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.metallicus.protonsdk.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.metallicus.protonsdk.R
import com.metallicus.protonsdk.common.Prefs
import com.metallicus.protonsdk.common.ProtonError
import com.metallicus.protonsdk.model.ChainProvider
import com.metallicus.protonsdk.model.ChainUrlInfo
import com.metallicus.protonsdk.model.KYCProvider
import com.metallicus.protonsdk.repository.ChainProviderRepository
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import timber.log.Timber

class InitChainProviderWorker
@AssistedInject constructor(
	@Assisted context: Context,
	@Assisted params: WorkerParameters,
	private val prefs: Prefs,
	private val chainProviderRepository: ChainProviderRepository
) : CoroutineWorker(context, params) {
	companion object {
		const val PROTON_CHAIN_URL = "protonChainUrl"
	}

	private val kycProvidersTableScope = context.getString(R.string.kycProvidersTableScope)
	private val kycProvidersTableCode = context.getString(R.string.kycProvidersTableCode)
	private val kycProvidersTableName = context.getString(R.string.kycProvidersTableName)

	@Suppress("BlockingMethodInNonBlockingContext")
	override suspend fun doWork(): Result {
		val protonChainUrl = inputData.getString(PROTON_CHAIN_URL).orEmpty()

		return try {
			val response = chainProviderRepository.fetchChainProvider(protonChainUrl)
			if (response.isSuccessful) {
				chainProviderRepository.removeAll()

				val chainProvider = Gson().fromJson(response.body(), ChainProvider::class.java)
				chainProvider.protonChainUrl = protonChainUrl

				val acceptableChainBlockDiff = ChainUrlInfo.ACCEPTABLE_CHAIN_BLOCK_DIFF
				val acceptableHyperionHistoryBlockDiff = ChainUrlInfo.ACCEPTABLE_HYPERION_HISTORY_BLOCK_DIFF

				val chainUrlStats = mutableListOf<ChainUrlInfo>()
				chainProvider.chainUrls.forEach { chainUrl ->
					try {
						val chainUrlResponse = chainProviderRepository.getChainInfo(chainUrl)
						if (chainUrlResponse.isSuccessful) {
							chainUrlResponse.body()?.let { chainInfo ->
								val blockDiff = chainInfo.headBlockNum.toLong() - chainInfo.lastIrreversibleBlockNum.toLong()
								val responseTimeMillis = chainUrlResponse.raw().receivedResponseAtMillis - chainUrlResponse.raw().sentRequestAtMillis
								val inSync = blockDiff < acceptableChainBlockDiff

								val chainUrlInfo = ChainUrlInfo(chainUrl, responseTimeMillis, blockDiff, inSync)
								chainUrlStats.add(chainUrlInfo)
							}
						}
					} catch (e: Exception) {
						Timber.d(e)
					}
				}
				chainProvider.chainUrlStats = chainUrlStats

				val hyperionHistoryUrlStats = mutableListOf<ChainUrlInfo>()
				chainProvider.hyperionHistoryUrls.forEach { hyperionHistoryUrl ->
					try {
						val healthResponse = chainProviderRepository.getHealth(hyperionHistoryUrl)
						if (healthResponse.isSuccessful) {
							var blockDiff: Long? = null

							healthResponse.body()?.let { body ->
								var headBlockNum = 0L
								var lastIndexedBlock = 0L
								val health = body.get("health").asJsonArray
								health.forEach { healthElement ->
									val serviceObj = healthElement.asJsonObject
									if (serviceObj.get("service").asString == "NodeosRPC") {
										val serviceDataObj = serviceObj.get("service_data").asJsonObject
										headBlockNum = serviceDataObj.get("head_block_num").asLong
									}
									if (serviceObj.get("service").asString == "Elasticsearch") {
										val serviceDataObj = serviceObj.get("service_data").asJsonObject
										lastIndexedBlock = serviceDataObj.get("last_indexed_block").asLong
									}
								}

								if (headBlockNum != 0L && lastIndexedBlock != 0L) {
									blockDiff = headBlockNum - lastIndexedBlock
								}
							}

							blockDiff?.let { blkDiff ->
								val responseTimeMillis = healthResponse.raw().receivedResponseAtMillis - healthResponse.raw().sentRequestAtMillis
								val inSync = blkDiff < acceptableHyperionHistoryBlockDiff

								val chainUrlInfo = ChainUrlInfo(hyperionHistoryUrl, responseTimeMillis, blkDiff, inSync)
								hyperionHistoryUrlStats.add(chainUrlInfo)
							}
						}
					} catch (e: Exception) {
						Timber.d(e)
					}
				}
				chainProvider.hyperionHistoryUrlStats = hyperionHistoryUrlStats

				val fastestChainUrl = chainUrlStats.filter { it.inSync }.minByOrNull { it.responseTimeMillis }
				fastestChainUrl?.apply {
					chainProvider.chainUrl = url
				}

				val fastestHyperionUrl = hyperionHistoryUrlStats.filter { it.inSync }.minByOrNull { it.responseTimeMillis }
				fastestHyperionUrl?.apply {
					chainProvider.hyperionHistoryUrl = url
				}

				val kycProviders = mutableListOf<KYCProvider>()

				val kycProvidersResponse = chainProviderRepository.fetchKYCProviders(
					chainProvider.chainUrl,
					kycProvidersTableScope,
					kycProvidersTableCode,
					kycProvidersTableName)
				if (kycProvidersResponse.isSuccessful) {
					val responseJsonObject = kycProvidersResponse.body()

					val gson = Gson()
					val rows = responseJsonObject?.getAsJsonArray("rows")
					rows?.forEach {
						val kycProvidersJsonObject = it.asJsonObject

						val kycProvider =
							gson.fromJson(kycProvidersJsonObject, KYCProvider::class.java)

						if (kycProvider.blackListed == 0) {
							kycProviders.add(kycProvider)
						}
					}
				}

				chainProvider.kycProviders = kycProviders

				chainProviderRepository.addChainProvider(chainProvider)

				prefs.activeChainId = chainProvider.chainId

				Result.success()
			} else {
				val msg = response.errorBody()?.string()
				val errorMsg = if (msg.isNullOrEmpty()) {
					response.message()
				} else {
					msg
				}
				Timber.d(errorMsg)

				val errorData = Data.Builder()
					.putString(ProtonError.ERROR_MESSAGE_KEY, errorMsg)
					.putInt(ProtonError.ERROR_CODE_KEY, response.code())
					.build()

				Result.failure(errorData)
			}
		} catch (e: Exception) {
			Timber.d(e)

			Result.failure()
		}
	}

	@AssistedInject.Factory
	interface Factory : ChildWorkerFactory
}