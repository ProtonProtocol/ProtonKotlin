package com.metallicus.protonsdk.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.metallicus.protonsdk.eosio.commander.model.chain.PackedTransaction
import com.metallicus.protonsdk.eosio.commander.model.chain.SignedTransaction
import com.metallicus.protonsdk.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

data class AccountBody(val account_name: String)
data class TableRowsBody(
	val scope: String,
	val code: String,
	val table: String,
	val lower_bound: String = "",
	val upper_bound: String = "",
	val limit: Long = 1,
	val json: Boolean = true)
data class UserNameBody(val signature: String, val name: String)
data class JsonToBinBody(val code: String, val action: String, val args: JsonElement)
data class RequiredKeysBody(val transaction: SignedTransaction, val available_keys: List<String>)

interface ProtonChainService {
	@GET
	suspend fun getChainProvider(@Url url: String): Response<JsonObject>

	@GET
	suspend fun getExchangeRates(@Url url: String): Response<JsonArray>

	@PUT
	suspend fun updateUserName(
		@Url url: String,
		@Body body: UserNameBody): Response<JsonObject>

	@PUT
	suspend fun uploadUserAvatar(
		@Url url: String,
		@Body body: MultipartBody): Response<JsonObject>

	@GET//("/v2/state/get_key_accounts?public_key=")
	suspend fun getKeyAccounts(
		@Url url: String,
		@Query("public_key") publicKey: String
	): Response<KeyAccount>

	@POST//("/v1/chain/get_account")
	suspend fun getAccount(
		@Url url: String,
		@Body body: AccountBody
	): Response<Account>

	@GET//("/v2/state/get_tokens?account=")
	suspend fun getCurrencyBalances(
		@Url url: String,
		@Query("account") account: String
	): Response<JsonObject>

	@GET//("/v2/history/get_actions?account=&transfer.symbol=&filter=&limit=")
	suspend fun getActions(
		@Url url: String,
		@Query("account") account: String,
		@Query("transfer.symbol") symbol: String,
		//@Query("filter") filter: String,
		@Query("limit") limit: Int
	): Response<JsonObject>

	@POST//("/v1/chain/get_table_rows")
	suspend fun getTableRows(
		@Url url: String,
		@Body body: TableRowsBody
	): Response<JsonObject>

	@POST//("/v1/chain/abi_json_to_bin")
	suspend fun jsonToBin(
		@Url url: String,
		@Body body: JsonToBinBody): Response<JsonToBinResponse>

	@POST//("/v1/chain/get_info")
	suspend fun getChainInfo(@Url url: String): Response<ChainInfo>

	@POST//("/v1/chain/get_required_keys")
	suspend fun getRequiredKeys(
		@Url url: String,
		@Body body: RequiredKeysBody): Response<RequiredKeysResponse>

	@POST//("/v1/chain/push_transaction")
	suspend fun pushTransaction(
		@Url url: String,
		@Body body: PackedTransaction): Response<JsonObject>
}