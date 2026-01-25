package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["url"])
data class OxyraNodeRecord(
    val url: String,
    val username: String?,
    val password: String?,
    val trusted: Boolean
)
