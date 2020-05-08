package com.proton.protonchain.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.proton.protonchain.model.ChainProvider
import com.proton.protonchain.model.TokenContract

@Database(
	entities = [
		ChainProvider::class,
		TokenContract::class],
	version = 4,
	exportSchema = false
)
abstract class ProtonChainDb : RoomDatabase() {
	abstract fun chainProviderDao(): ChainProviderDao
	abstract fun tokenContractDao(): TokenContractDao
}