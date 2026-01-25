package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.IAdapter

interface IMoneroAdapter : IAdapter {
    val receiveAddress: String
    val isMainNet: Boolean
    fun getSubaddresses(): List<MoneroSubAddress>
}

data class MoneroSubAddress(
    val addressIndex: Int,
    val address: String,
    val txsCount: Long
)
