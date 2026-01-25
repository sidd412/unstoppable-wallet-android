package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import android.util.Log
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendOxyraAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.BackgroundManagerState
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import io.horizontalsystems.bankwallet.core.managers.OxyraNodeManager
import io.horizontalsystems.oxyrakit.OxyraKit
import io.horizontalsystems.oxyrakit.Balance
import io.horizontalsystems.oxyrakit.SyncState
import io.horizontalsystems.oxyrakit.Seed
import io.horizontalsystems.oxyrakit.data.Subaddress
import io.horizontalsystems.oxyrakit.model.TransactionInfo

class OxyraAdapter(
    private val kit: OxyraKit,
    private val transactionsProvider: OxyraTransactionsProvider,
    private val transactionsAdapter: OxyraTransactionsAdapter,
    private val backgroundManager: BackgroundManager,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendOxyraAdapter, ITransactionsAdapter by transactionsAdapter {

    private val TAG = "SidOxyra"
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    init {
        Log.d(TAG, "🚀 OxyraAdapter initialized")
        Log.d(TAG, "📍 Receive Address: ${kit.receiveAddress}")
    }

    private var balance = Balance(0, 0)

    override var balanceState: AdapterState = kit.syncStateFlow.value.toAdapterState()

    override val balanceData: BalanceData
        get() = balance.toBalanceData()

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val receiveAddress: String
        get() = kit.receiveAddress

    override val isMainNet: Boolean
        get() = true

    override fun start() {
        Log.e(TAG, "▶️▶️▶️ STARTING OXYRA ADAPTER (Entry Point) ▶️▶️▶️")
        
        // Direct call to kit.start() just to see if suspends
        // GlobalScope.launch { kit.start() } 
        
        coroutineScope.launch {
            Log.e(TAG, "⚡ Inside CoroutineScope Launch")
            kit.balanceFlow.collect { newBalance ->
                Log.d(TAG, "💰 Balance Updated: Unlocked=${newBalance.unlocked.scaledDownOxyra(DECIMALS)}, Total=${newBalance.all.scaledDownOxyra(DECIMALS)}")
                balance = newBalance
                balanceUpdatedSubject.onNext(Unit)
            }
        }

        coroutineScope.launch {
            kit.syncStateFlow.collect { syncState ->
                Log.d(TAG, "🔄 Sync State Changed: ${syncState.javaClass.simpleName}")
                when (syncState) {
                    is SyncState.Syncing -> Log.d(TAG, "   Progress: ${syncState.progress?.times(100)}%")
                    is SyncState.NotSynced -> Log.e(TAG, "   Error: ${syncState.error?.message}")
                    is SyncState.Synced -> Log.d(TAG, "   ✅ Fully Synced!")
                    else -> {}
                }
                balanceState = syncState.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }

        coroutineScope.launch {
            kit.allTransactionsFlow.collect { transactions ->
                transactionsProvider.onTransactions(transactions)
            }
        }

        coroutineScope.launch {
            kit.start()
        }

        coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterBackground) {
                    kit.saveState()
                }
            }
        }
    }

    override fun stop() {
        kit.saveState()
        coroutineScope.launch {
            kit.stop()
            coroutineScope.cancel()
        }
    }

    override fun refresh() {
        coroutineScope.launch {
            Log.d(TAG, "🌐 Connecting to Oxyra network...")
            try {
                kit.start()
                Log.d(TAG, "✅ Successfully connected to network")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start kit: ${e.message}", e)
            }
        }
    }

    override val debugInfo: String
        get() = ""

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        Log.d(TAG, "💸 Sending Transaction:")
        Log.d(TAG, "   Amount: $amount OXRX")
        Log.d(TAG, "   To: $address")
        Log.d(TAG, "   Memo: ${memo ?: "(none)"}")
        
        val amountLong = amount.movePointRight(DECIMALS).toLong()
        try {
            kit.send(amountLong, address, memo)
            Log.d(TAG, "✅ Transaction sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Send failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun estimateFee(
        amount: BigDecimal,
        address: String,
        memo: String?
    ): BigDecimal {
        Log.d(TAG, "📊 Estimating fee for amount: $amount OXRX")
        val amountLong = amount.movePointRight(DECIMALS).toLong()
        val fee = kit.estimateFee(amountLong, address, memo)
        val feeDecimal = fee.scaledDownOxyra(DECIMALS)
        Log.d(TAG, "   Estimated fee: $feeDecimal OXRX")
        return feeDecimal
    }

    fun getSubaddresses(): List<Subaddress> {
        return kit.getSubaddresses()
    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    companion object {
        const val DECIMALS = 8

        fun create(
            context: Context,
            wallet: Wallet,
            restoreSettings: RestoreSettings,
            node: OxyraNodeManager.OxyraNode
        ): OxyraAdapter {
            Log.e("SidOxyra", "🔥🔥🔥 OxyraAdapter.create() called for wallet ${wallet.account.id}")
            val birthdayHeightStr: String?
            val seed: Seed
            when (val accountType = wallet.account.type) {
                is AccountType.Mnemonic -> {
                    birthdayHeightStr = restoreSettings.birthdayHeight?.toString()
                    seed = Seed.Bip39(accountType.words, accountType.passphrase)
                }

                is AccountType.OxyraWatchAccount -> {
                    birthdayHeightStr = accountType.restoreHeight.toString()
                    seed = Seed.WatchOnly(address = "", viewPrivateKey = accountType.privateViewKey)
                }

                else -> throw IllegalStateException("Unsupported account type: ${wallet.account.type.javaClass.simpleName}")
            }

            val birthdayHeightOrDate: String = when (wallet.account.origin) {
                AccountOrigin.Created -> {
                    birthdayHeightStr ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }

                AccountOrigin.Restored -> {
                    birthdayHeightStr ?: "1"
                }
            }
            
            val kit = OxyraKit.getInstance(
                context,
                seed,
                birthdayHeightOrDate,
                wallet.account.id + "_oxyra",
                node.host,
                node.trusted
            )

            val transactionsProvider = OxyraTransactionsProvider()
            val transactionsAdapter = OxyraTransactionsAdapter(kit, transactionsProvider, wallet)

            return OxyraAdapter(
                kit,
                transactionsProvider,
                transactionsAdapter,
                App.backgroundManager
            )
        }

        fun clear(walletId: String) {
            OxyraKit.deleteWallet(App.instance, walletId)
        }
    }

    override fun validate(address: String) {
        Log.d(TAG, "✅ Validating address: ${address.take(10)}...")
        OxyraKit.validateAddress(address)  // Throws exception if invalid
        Log.d(TAG, "   Address is valid")
    }
}

// Extension functions
fun Long.scaledDownOxyra(decimals: Int): BigDecimal {
    return this.toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
}

fun SyncState.toAdapterState(): AdapterState = when (this) {
    is SyncState.NotSynced -> AdapterState.NotSynced(error ?: Exception("Unknown error"))
    is SyncState.Synced -> AdapterState.Synced
    is SyncState.Connecting -> AdapterState.Connecting
    is SyncState.Syncing -> AdapterState.Syncing(progress = progress?.let {
        (it * 100).roundToInt().coerceAtMost(100)
    })
}

fun AccountType.toSeed(): Seed = when (this) {
    is AccountType.Mnemonic -> Seed.Bip39(words, passphrase)
    is AccountType.OxyraWatchAccount -> Seed.WatchOnly(address = "", viewPrivateKey = privateViewKey)
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to Oxyra Seed")
}

fun Balance.toBalanceData(): BalanceData {
    val available = unlocked.scaledDownOxyra(OxyraAdapter.DECIMALS)
    val pending = (all - unlocked).coerceAtLeast(0).scaledDownOxyra(OxyraAdapter.DECIMALS)
    return BalanceData(available, pending = pending)
}
