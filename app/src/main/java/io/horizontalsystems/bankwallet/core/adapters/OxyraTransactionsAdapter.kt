package io.horizontalsystems.bankwallet.core.adapters

import android.util.Log
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
import io.horizontalsystems.oxyrakit.OxyraKit
import io.horizontalsystems.oxyrakit.model.TransactionInfo
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.rx2.asFlowable

class OxyraTransactionsAdapter(
    private val kit: OxyraKit,
    private val transactionsProvider: OxyraTransactionsProvider,
    private val wallet: Wallet,
) : ITransactionsAdapter {

    private val TAG = "SidOxyra"

    override val explorerTitle: String = "Oxyra Explorer"

    override val transactionsState: AdapterState
        get() = kit.syncStateFlow.value.toAdapterState()

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = kit.syncStateFlow.asFlowable().doOnNext { 
            Log.d(TAG, "Adapter syncState updated: $it") 
        }.map { }

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
        Log.d(TAG, "📥 Fetching transactions: from=${from?.transactionHash}, type=$transactionType, limit=$limit")
        return transactionsProvider.getTransactions(from?.transactionHash, transactionType, address, limit)
            .map { transactions ->
                Log.d(TAG, "   Found ${transactions.size} transactions")
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
        Log.d(TAG, "🔄 Subscribing to new transactions: type=$transactionType")
        return transactionsProvider.getNewTransactionsFlowable(transactionType)
            .map { transactions ->
                Log.d(TAG, "   New transactions received: ${transactions.size}")
                transactions.map { getTransactionRecord(it) }
            }
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://oxyra-explorer.com/transaction/$transactionHash"

    private fun getTransactionRecord(transaction: TransactionInfo): TransactionRecord {
        Log.d(TAG, "🔍 Parsing transaction: ${transaction.hash.take(8)}... direction=${transaction.direction}")
        val blockHeight = if (transaction.blockheight == 0L || transaction.isPending) null else transaction.blockheight.toInt()
        return when (transaction.direction) {
            TransactionInfo.Direction.Direction_In -> {
                val subaddress = kit.getSubaddress(transaction.accountIndex, transaction.addressIndex)
                BitcoinIncomingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
                    confirmationsThreshold = TransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDownOxyra(DECIMALS),
                    failed = transaction.isFailed,
                    lockInfo = null,
                    conflictingHash = null,
                    showRawTransaction = false,
                    amount = transaction.amount.scaledDownOxyra(DECIMALS),
                    from = null,
                    to = subaddress?.address,
                    memo = transaction.notes,
                    source = wallet.transactionSource
                )
            }

            TransactionInfo.Direction.Direction_Out -> {
                BitcoinOutgoingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
                    confirmationsThreshold = TransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDownOxyra(DECIMALS),
                    failed = transaction.isFailed,
                    lockInfo = null,
                    conflictingHash = null,
                    showRawTransaction = false,
                    amount = transaction.amount.scaledDownOxyra(DECIMALS).negate(),
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
