package com.metallicus.protonsdk.repository

import com.google.gson.JsonObject
import com.metallicus.protonsdk.api.ProtonChainService
import com.metallicus.protonsdk.api.TableRowsBody
import com.metallicus.protonsdk.db.TokenContractDao
import com.metallicus.protonsdk.model.TokenContract
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenContractRepository @Inject constructor(
	private val tokenContractDao: TokenContractDao,
	private val protonChainService: ProtonChainService
) {
	suspend fun removeAll() {
		tokenContractDao.removeAll()
	}

	suspend fun addTokenContract(tokenContract: TokenContract) {
		tokenContractDao.insert(tokenContract)
	}

	suspend fun fetchTokenContracts(chainUrl: String, tokensTableScope: String, tokensTableCode: String, tokensTableName: String): Response<JsonObject> {
		return protonChainService.getTableRows("$chainUrl/v1/chain/get_table_rows", TableRowsBody(tokensTableScope, tokensTableCode, tokensTableName))
	}

	suspend fun getTokenContract(tokenContractId: String): TokenContract {
		return tokenContractDao.findById(tokenContractId)
	}

	suspend fun getTokenContracts(): List<TokenContract> {
		return tokenContractDao.findAll()
	}
}
