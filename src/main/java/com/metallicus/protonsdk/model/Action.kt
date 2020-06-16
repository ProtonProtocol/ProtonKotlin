package com.metallicus.protonsdk.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.metallicus.protonsdk.db.DefaultTypeConverters
import com.metallicus.protonsdk.db.EOSTypeConverters
import java.text.NumberFormat
import java.util.*
import kotlin.math.sign

@Entity(
	indices = [(Index(
		"accountName",
		"action_trace_act_account",
		"action_trace_act_data_quantity"
	))],
	primaryKeys = ["accountName", "action_trace_trxId", "action_trace_act_name", "action_trace_act_authorization"]
)
@TypeConverters(DefaultTypeConverters::class, EOSTypeConverters::class)
data class Action(
	@SerializedName("global_action_seq") val globalActionSeq: Int,
	@SerializedName("block_num") val blockNum: Int,
	@SerializedName("block_time") val blockTime: String,
	@SerializedName("action_trace")
	@Embedded(prefix = "action_trace_") val actionTrace: ActionTrace
) {
	lateinit var accountName: String

	lateinit var accountContact: AccountContact

	enum class IconType { AVATAR, SEND, RECEIVE, STAKE, UNSTAKE, BUY_RAM }

	fun isSender(): Boolean {
		return (accountName == actionTrace.act.data?.from && actionTrace.act.data.from != actionTrace.act.data.to)
	}

	fun getIconType(): IconType {
		return IconType.AVATAR

//		return if (accountContact.isLynxChain) {
//			IconType.AVATAR
//		} else {
//			if (actionTrace.act.data?.to == "eosio.ramfee" || actionTrace.act.data?.to == "eosio.ram") {
//				IconType.BUY_RAM
//			} else if (actionTrace.act.data?.to == "eosio.stake") {
//				IconType.STAKE
//			} else if (actionTrace.act.data?.from == "eosio.stake") {
//				IconType.UNSTAKE
//			} else if (!isSender()) {
//				IconType.RECEIVE
//			} else {
//				IconType.SEND
//			}
//		}
	}

	fun getDisplayName(): String {
		return when {
			actionTrace.act.data?.to == "eosio.ramfee" -> "Buy RAM Fee"
			actionTrace.act.data?.to == "eosio.ram" -> "Buy RAM"
			actionTrace.act.data?.to == "eosio.stake" -> "Staked Resources"
			actionTrace.act.data?.from == "eosio.stake" -> "Unstaked Resources"
			else -> accountContact.getDisplayName()
		}
	}

	private fun getAmount(): Double {
		var amount = 0.0
		actionTrace.act.data?.let { data ->
			amount = data.quantityToDouble()
			if (isSender()) {
				amount = -amount
			}
		}
		return amount
	}

	fun getAmountStr(precision: Long): String {
		val amount = getAmount()

		val nf = NumberFormat.getNumberInstance(Locale.US)
		nf.minimumFractionDigits = precision.toInt()
		nf.maximumFractionDigits = precision.toInt()

		var amountStr = nf.format(amount)

		if (amount.sign != -1.0) {
			amountStr = "+$amountStr"
		}

		return amountStr
	}

	fun getAmountCurrency(rate: Double): String {
		val amount = getAmount()

		val nf = NumberFormat.getCurrencyInstance(Locale.US)

		val amountCurrency = amount.times(rate)

		var amountCurrencyStr = nf.format(amountCurrency)
		if (amount.sign != -1.0) {
			amountCurrencyStr = "+$amountCurrencyStr"
		}

		return amountCurrencyStr
	}
}