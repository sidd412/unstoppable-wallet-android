package io.horizontalsystems.bankwallet.core.adapters

<<<<<<< HEAD
import android.util.Log
=======
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.OxyraAdapter.Companion.DECIMALS
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
<<<<<<< HEAD
import io.horizontalsystems.oxyrakit.OxyraKit
import io.horizontalsystems.oxyrakit.model.TransactionInfo
=======
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.rx2.asFlowable

class OxyraTransactionsAdapter(
    private val kit: OxyraKit,
    private val transactionsProvider: OxyraTransactionsProvider,
    private val wallet: Wallet,
) : ITransactionsAdapter {

<<<<<<< HEAD
    private val TAG = "SidOxyra"

=======
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
    override val explorerTitle: String = "Oxyra Explorer"

    override val transactionsState: AdapterState
        get() = kit.syncStateFlow.value.toAdapterState()

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
<<<<<<< HEAD
        get() = kit.syncStateFlow.asFlowable().doOnNext { 
            Log.d(TAG, "Adapter syncState updated: $it") 
        }.map { }
=======
        get() = kit.syncStateFlow.asFlowable().map { }
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f

    override val lastBlockInfo: LastBlockInfo?
        get() = kit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = kit.lastBlockUpdatedFlow.asFlowable()

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?
    ): Single<List<TransactionRecord>> {
<<<<<<< HEAD
        Log.d(TAG, "📥 Fetching transactions: from=${from?.transactionHash}, type=$transactionType, limit=$limit")
        return transactionsProvider.getTransactions(from?.transactionHash, transactionType, address, limit)
            .map { transactions ->
                Log.d(TAG, "   Found ${transactions.size} transactions")
=======
        return transactionsProvider.getTransactions(from?.transactionHash, transactionType, address, limit)
            .map { transactions ->
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
                transactions.map {
                    getTransactionRecord(it)
                }
            }
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?
    ): Flowable<List<TransactionRecord>> {
<<<<<<< HEAD
        Log.d(TAG, "🔄 Subscribing to new transactions: type=$transactionType")
        return transactionsProvider.getNewTransactionsFlowable(transactionType)
            .map { transactions ->
                Log.d(TAG, "   New transactions received: ${transactions.size}")
=======
        return transactionsProvider.getNewTransactionsFlowable(transactionType)
            .map { transactions ->
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
                transactions.map { getTransactionRecord(it) }
            }
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://oxyra-explorer.com/transaction/$transactionHash"

<<<<<<< HEAD
    private fun getTransactionRecord(transaction: TransactionInfo): TransactionRecord {
        Log.d(TAG, "🔍 Parsing transaction: ${transaction.hash.take(8)}... direction=${transaction.direction}")
        val blockHeight = if (transaction.blockheight == 0L || transaction.isPending) null else transaction.blockheight.toInt()
        return when (transaction.direction) {
            TransactionInfo.Direction.Direction_In -> {
=======
    private fun getTransactionRecord(transaction: OxyraTransactionInfo): TransactionRecord {
        val blockHeight = if (transaction.blockheight == 0L || transaction.isPending) null else transaction.blockheight.toInt()
        return when (transaction.direction) {
            OxyraTransactionInfo.Direction.Direction_In -> {
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
                val subaddress = kit.getSubaddress(transaction.accountIndex, transaction.addressIndex)
                BitcoinIncomingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
<<<<<<< HEAD
                    confirmationsThreshold = TransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDownOxyra(DECIMALS),
=======
                    confirmationsThreshold = OxyraTransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDown(DECIMALS),
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
                    failed = transaction.isFailed,
                    lockInfo = null,
                    conflictingHash = null,
                    showRawTransaction = false,
<<<<<<< HEAD
                    amount = transaction.amount.scaledDownOxyra(DECIMALS),
=======
                    amount = transaction.amount.scaledDown(DECIMALS),
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
                    from = null,
                    to = subaddress?.address,
                    memo = transaction.notes,
                    source = wallet.transactionSource
                )
            }

<<<<<<< HEAD
            TransactionInfo.Direction.Direction_Out -> {
=======
            OxyraTransactionInfo.Direction.Direction_Out -> {
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
                BitcoinOutgoingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
<<<<<<< HEAD
                    confirmationsThreshold = TransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDownOxyra(DECIMALS),
=======
                    confirmationsThreshold = OxyraTransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDown(DECIMALS),
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
                    failed = transaction.isFailed,
                    lockInfo = null,
                    conflictingHash = null,
                    showRawTransaction = false,
<<<<<<< HEAD
                    amount = transaction.amount.scaledDownOxyra(DECIMALS).negate(),
=======
                    amount = transaction.amount.scaledDown(DECIMALS).negate(),
>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
                    to = null,
                    sentToSelf = false,
                    memo = transaction.notes,
                    source = wallet.transactionSource,
                    replaceable = false
                )
            }
        }
    }
}
<<<<<<< HEAD
=======

// Oxyra Transaction Info data class
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

>>>>>>> e2dcf40944c75f7bf6c82239c558e000755e3d0f
