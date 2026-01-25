package io.horizontalsystems.bankwallet.core.adapters

import android.util.Log
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.SyncState
import io.horizontalsystems.monerokit.model.TransactionInfo
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import kotlinx.coroutines.rx2.asFlowable

class MoneroTransactionsAdapter(
    private val kit: MoneroKit,
    private val wallet: Wallet
) : ITransactionsAdapter {

    private val TAG = "SidOxyra"

    override val explorerTitle: String = "Monero Explorer"

    override val transactionsState: AdapterState
        get() = convertToAdapterState(kit.syncStateFlow.value)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = kit.syncStateFlow.asFlowable().map { }

    override val lastBlockInfo: LastBlockInfo?
        get() = kit.lastBlockHeight?.let { LastBlockInfo(it.toInt()) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = kit.lastBlockUpdatedFlow.asFlowable()

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?
    ): Single<List<TransactionRecord>> {
        return Single.just(emptyList())
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?
    ): Flowable<List<TransactionRecord>> {
        return kit.allTransactionsFlow.asFlowable().map { transactions ->
            transactions.map { convertTransaction(it) }
        }
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://xmrchain.net/tx/$transactionHash"
    }
    
    fun start() { }
    fun stop() { }

    private fun convertToAdapterState(syncState: SyncState): AdapterState = when (syncState) {
        is SyncState.Synced -> AdapterState.Synced
        is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SyncState.Syncing -> AdapterState.Syncing(syncState.progress?.times(100)?.toInt())
        else -> AdapterState.NotSynced(Exception("Unknown state"))
    }

    private fun convertTransaction(transaction: TransactionInfo): TransactionRecord {
        val amount = transaction.amount.toBigDecimal().movePointLeft(12)
        val fee = transaction.fee.toBigDecimal().movePointLeft(12)
        val blockHeight = if (transaction.blockheight == 0L || transaction.isPending) null else transaction.blockheight.toInt()

        return if (transaction.direction == TransactionInfo.Direction.Direction_In) {
            BitcoinIncomingTransactionRecord(
                token = wallet.token,
                uid = transaction.hash,
                transactionHash = transaction.hash,
                transactionIndex = 0,
                blockHeight = blockHeight,
                confirmationsThreshold = 10,
                timestamp = transaction.timestamp,
                fee = fee,
                failed = transaction.isFailed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = amount,
                from = null,
                to = null,
                memo = transaction.notes,
                source = wallet.transactionSource
            )
        } else {
            BitcoinOutgoingTransactionRecord(
                token = wallet.token,
                uid = transaction.hash,
                transactionHash = transaction.hash,
                transactionIndex = 0,
                blockHeight = blockHeight,
                confirmationsThreshold = 10,
                timestamp = transaction.timestamp,
                fee = fee,
                failed = transaction.isFailed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = amount.negate(),
                to = null,
                sentToSelf = false,
                memo = transaction.notes,
                source = wallet.transactionSource,
                replaceable = false
            )
        }
    }
}
