package io.horizontalsystems.bankwallet.core.adapters

data class OxyraTransactionInfo(
    val hash: String,
    val amount: Long,
    val fee: Long,
    val timestamp: Long,
    val blockheight: Long,
    val confirmations: Int,
    val isPending: Boolean,
    val isFailed: Boolean,
    val direction: Direction,
    val accountIndex: Int,
    val addressIndex: Int,
    val notes: String?
) {
    enum class Direction {
        Direction_In,
        Direction_Out
    }

    companion object {
        const val CONFIRMATION = 10
    }
}
