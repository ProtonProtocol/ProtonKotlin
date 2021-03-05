/*
 * Copyright (c) 2020 Proton Chain LLC, Delaware
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
package com.metallicus.protonsdk

import android.content.Context
import com.metallicus.protonsdk.common.Prefs
import com.metallicus.protonsdk.di.DaggerInjector
import com.metallicus.protonsdk.model.ChainProvider
import com.metallicus.protonsdk.repository.ChainProviderRepository
import javax.inject.Inject

/**
 * Helper class used for [ChainProvider] based operations
 */
class ChainProviderModule {
	@Inject
	lateinit var context: Context

	@Inject
	lateinit var chainProviderRepository: ChainProviderRepository

	@Inject
	lateinit var prefs: Prefs

	init {
		DaggerInjector.component.inject(this)
	}

	suspend fun getActiveChainProvider(): ChainProvider {
		val chainId = prefs.activeChainId
		return chainProviderRepository.getChainProvider(chainId)
	}

	suspend fun updateChainUrl(chainId: String, chainUrl: String) {
		chainProviderRepository.updateChainUrl(chainId, chainUrl)
	}

	suspend fun updateHyperionHistoryUrl(chainId: String, hyperionHistoryUrl: String) {
		chainProviderRepository.updateHyperionHistoryUrl(chainId, hyperionHistoryUrl)
	}
}