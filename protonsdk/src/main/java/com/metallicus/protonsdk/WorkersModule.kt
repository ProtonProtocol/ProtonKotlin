package com.metallicus.protonsdk

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.*
import com.metallicus.protonsdk.common.Prefs
import com.metallicus.protonsdk.di.DaggerInjector
import com.metallicus.protonsdk.workers.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkersModule {
	@Inject
	lateinit var context: Context

	@Inject
	lateinit var workerFactory: ProtonWorkerFactory

	@Inject
	lateinit var prefs: Prefs

	private var workManager: WorkManager

	init {
		DaggerInjector.component.inject(this)

		val workManagerConfig = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()
		WorkManager.initialize(context, workManagerConfig)

		workManager = WorkManager.getInstance(context)
	}

	companion object {
		const val INIT = "WORKER_INIT"
		const val UPDATE_RATES = "WORKER_UPDATE_RATES"
	}

	fun init(chainProviderUrl: String) {
		workManager.pruneWork()

		prefs.clearInit()

		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		val chainProviderInputData = Data.Builder()
			.putString(InitChainProviderWorker.CHAIN_PROVIDER_URL, chainProviderUrl)
			.build()

		val initChainProvider = OneTimeWorkRequest.Builder(InitChainProviderWorker::class.java)
			.setConstraints(constraints).setInitialDelay(10, TimeUnit.SECONDS).setInputData(chainProviderInputData).build()

		val initTokenContracts = OneTimeWorkRequest.Builder(InitTokenContractsWorker::class.java)
			.setConstraints(constraints).build()

		val initActiveAccount = OneTimeWorkRequest.Builder(InitActiveAccountWorker::class.java)
			.setConstraints(constraints).build()

		workManager
			.beginUniqueWork(INIT, ExistingWorkPolicy.REPLACE, initChainProvider)
			.then(initTokenContracts)
			.then(initActiveAccount)
			.enqueue()

		// start periodic worker to update exchange rates
		val updateTokenContractRates = PeriodicWorkRequest.Builder(UpdateTokenContractRatesWorker::class.java, 15, TimeUnit.MINUTES)
			.setConstraints(constraints)
			.setInitialDelay(1, TimeUnit.MINUTES)
			.build()
		workManager.enqueueUniquePeriodicWork(UPDATE_RATES, ExistingPeriodicWorkPolicy.REPLACE, updateTokenContractRates)
	}

	fun onInitChainProvider(callback: (Boolean, Data?) -> Unit) {
		if (prefs.hasChainProvider) {
			callback(true, null)
		} else {
			val workInfoLiveData = workManager.getWorkInfosForUniqueWorkLiveData(INIT)
			val workInfoObserver = object : Observer<List<WorkInfo>> {
				override fun onChanged(workInfos: List<WorkInfo>) {
					val chainProviderWorkInfos =
						workInfos.filter { it.tags.contains(InitChainProviderWorker::class.java.name) }
					if (chainProviderWorkInfos.isEmpty() ||
						workInfos.any { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }) {
						val data = workInfos.find { it.state == WorkInfo.State.FAILED }?.outputData
						callback(false, data)
						workInfoLiveData.removeObserver(this)
					} else if (chainProviderWorkInfos.all { it.state == WorkInfo.State.SUCCEEDED }) {
						prefs.hasChainProvider = true
						callback(true, null)
						workInfoLiveData.removeObserver(this)
					}
				}
			}
			workInfoLiveData.observeForever(workInfoObserver)
		}
	}

	fun onInitTokenContracts(callback: (Boolean, Data?) -> Unit) {
		if (prefs.hasTokenContracts) {
			callback(true, null)
		} else {
			val workInfoLiveData = workManager.getWorkInfosForUniqueWorkLiveData(INIT)
			val workInfoObserver = object : Observer<List<WorkInfo>> {
				override fun onChanged(workInfos: List<WorkInfo>) {
					val tokenContractWorkInfos =
						workInfos.filter { it.tags.contains(InitTokenContractsWorker::class.java.name) }
					if (tokenContractWorkInfos.isEmpty() ||
						workInfos.any { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }) {
						val data = workInfos.find { it.state == WorkInfo.State.FAILED }?.outputData
						callback(false, data)
						workInfoLiveData.removeObserver(this)
					} else if (tokenContractWorkInfos.all { it.state == WorkInfo.State.SUCCEEDED }) {
						prefs.hasTokenContracts = true
						callback(true, null)
						workInfoLiveData.removeObserver(this)
					}
				}
			}
			workInfoLiveData.observeForever(workInfoObserver)
		}
	}

	fun onInitActiveAccount(callback: (Boolean, Data?) -> Unit) {
		if (prefs.hasActiveAccount) {
			callback(true, null)
		} else {
			val workInfoLiveData = workManager.getWorkInfosForUniqueWorkLiveData(INIT)
			val workInfoObserver = object : Observer<List<WorkInfo>> {
				override fun onChanged(workInfos: List<WorkInfo>) {
					val activeAccountWorkInfos =
						workInfos.filter { it.tags.contains(InitActiveAccountWorker::class.java.name) }
					if (activeAccountWorkInfos.isEmpty() ||
						workInfos.any { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }) {
						val data = workInfos.find { it.state == WorkInfo.State.FAILED }?.outputData
						callback(false, data)
						workInfoLiveData.removeObserver(this)
					} else if (activeAccountWorkInfos.all { it.state == WorkInfo.State.SUCCEEDED }) {
						prefs.hasActiveAccount = true
						callback(true, null)
						workInfoLiveData.removeObserver(this)
					}
				}
			}
			workInfoLiveData.observeForever(workInfoObserver)
		}
	}
}