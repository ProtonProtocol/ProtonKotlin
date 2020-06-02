package com.proton.protonchain.repository

import com.google.gson.JsonObject
import com.proton.protonchain.api.AccountBody
import com.proton.protonchain.api.ProtonChainService
import com.proton.protonchain.api.TableRowsBody
import com.proton.protonchain.db.AccountDao
import com.proton.protonchain.model.Account
import com.proton.protonchain.model.ChainAccount
import com.proton.protonchain.model.KeyAccount
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
	private val accountDao: AccountDao,
	private val protonChainService: ProtonChainService
) {
	suspend fun fetchAccount(chainUrl: String, accountName: String): Response<Account> {
		return protonChainService.getAccountAsync("$chainUrl/v1/chain/get_account", AccountBody(accountName))
	}

	suspend fun fetchAccountInfo(chainUrl: String, accountName: String, usersInfoTableScope: String, usersInfoTableCode: String): Response<JsonObject> {
		return protonChainService.getTableRows("$chainUrl/v1/chain/get_table_rows", TableRowsBody(usersInfoTableScope, usersInfoTableCode, "usersinfo", accountName, accountName))
	}

	suspend fun getChainAccount(chainId: String, accountName: String): ChainAccount {
		return accountDao.findByAccountName(chainId, accountName)
	}

	suspend fun addAccount(account: Account) {
		accountDao.insert(account)
	}

	fun updateAccount(account: Account) {
		accountDao.update(account)
	}

	suspend fun fetchStateHistoryKeyAccount(chainUrl: String, publicKey: String): Response<KeyAccount> {
		return protonChainService.getStateHistoryKeyAccountsAsync("$chainUrl/v2/state/get_key_accounts", publicKey)
	}
}
