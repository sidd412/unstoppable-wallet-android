package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import android.util.Log
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendMoneroAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.BackgroundManagerState
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.SyncState
import io.horizontalsystems.monerokit.Seed
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import java.math.BigDecimal
import kotlin.math.roundToInt
import java.net.URI

class MoneroAdapter(
    wallet: Wallet,
    private val kit: MoneroKit,
    private val transactionsAdapter: MoneroTransactionsAdapter,
    private val backgroundManager: BackgroundManager
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendMoneroAdapter, IMoneroAdapter, ITransactionsAdapter by transactionsAdapter {

    private val TAG = "SidOxyra"
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        Log.d(TAG, "🚀 MoneroAdapter initialized")
        Log.d(TAG, "📍 Receive Address: ${kit.receiveAddress}")
    }
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    override val isMainNet: Boolean = true
    
    override fun getSubaddresses(): List<MoneroSubAddress> {
        return emptyList() // TODO: Implement if MoneroKit supports it, relying on kit.receiveAddress for now
    }

    override fun start() {
        coroutineScope.launch {
            kit.balanceFlow.collect {
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            kit.syncStateFlow.collect {
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            kit.start()
        }
        transactionsAdapter.start()

        coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterBackground) {
                    // kit.saveState() 
                }
            }
        }
    }

    override fun stop() {
        coroutineScope.launch {
             kit.stop()
        }
        transactionsAdapter.stop()
        coroutineScope.cancel()
    }

    override fun refresh() {
        coroutineScope.launch {
            kit.start()
        }
    }

    override val debugInfo: String
        get() = "" 

    override val receiveAddress: String
        get() = kit.receiveAddress

    override val balanceState: AdapterState
        get() = convertToAdapterState(kit.syncStateFlow.value)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceData: BalanceData
        get() {
            val balance = kit.balanceFlow.value
            val available = balance.unlocked.toBigDecimal().movePointLeft(12)
            val frozen = (balance.all - balance.unlocked).toBigDecimal().movePointLeft(12)
            return BalanceData(available, frozen) 
        }

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        kit.send(amount.movePointRight(12).toLong(), address, memo)
    }

    override suspend fun estimateFee(amount: BigDecimal, address: String, memo: String?): BigDecimal {
        // MoneroKit estimateFee might take Long
        return kit.estimateFee(amount.movePointRight(12).toLong(), address, memo).toBigDecimal().movePointLeft(12)
    }

    override fun validate(address: String) {
        MoneroKit.validateAddress(address)
    }

    private fun convertToAdapterState(syncState: SyncState): AdapterState = when (syncState) {
        is SyncState.Synced -> AdapterState.Synced
        is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SyncState.Syncing -> AdapterState.Syncing(syncState.progress?.times(100)?.toInt())
        else -> AdapterState.NotSynced(Exception("Unknown state"))
    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    companion object {
        fun create(
            context: Context,
            wallet: Wallet,
            restoreSettings: RestoreSettings,
            node: MoneroNodeManager.MoneroNode
        ): MoneroAdapter {
            val accountType = wallet.account.type
            val seed = accountType.toMoneroSeed()
            
            val kit = MoneroKit.getInstance(
                context,
                seed,
                restoreSettings.birthdayHeight?.toString() ?: "1",
                wallet.account.id + "_monero",
                node.host, 
                node.trusted
            )

            val transactionsAdapter = MoneroTransactionsAdapter(kit, wallet)
            return MoneroAdapter(wallet, kit, transactionsAdapter, App.backgroundManager)
        }
        
        fun clear(walletId: String) {
            MoneroKit.deleteWallet(App.instance, walletId)
        }
    }
}

fun AccountType.toMoneroSeed(): Seed = when (this) {
    is AccountType.Mnemonic -> Seed.Bip39(words, passphrase)
    // Add other types if Monero supports them, e.g. Watch
    else -> throw IllegalStateException("Unsupported account type for Monero")
}
