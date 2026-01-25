package io.horizontalsystems.bankwallet.core.managers

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.OxyraNodeStorage
import io.horizontalsystems.bankwallet.entities.OxyraNodeRecord
// import io.horizontalsystems.marketkit.models.Blockchain
// import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Objects

class OxyraNodeManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val oxyraNodeStorage: OxyraNodeStorage,
    //private val marketKitWrapper: MarketKitWrapper
) {
    //private val blockchainType = BlockchainType.Oxyra

    private val _currentNodeUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val currentNodeUpdatedFlow = _currentNodeUpdatedFlow.asSharedFlow()

    private val _nodesUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodesUpdatedFlow = _nodesUpdatedFlow.asSharedFlow()

    val defaultNodesInitial = listOf(
        OxyraNode("103.214.169.20:17081", "Oxyra Node (17081)", "103.214.169.20:17081", trusted = true)
    )

    val defaultNodes: List<OxyraNode>
        get() {
            val nodeRecordsMap = oxyraNodeStorage.getAll().associateBy { it.url }
            return defaultNodesInitial.map {
                it.copy(trusted = nodeRecordsMap[it.host]?.trusted ?: false)
            }
        }

    val customNodes: List<OxyraNode>
        get() {
            val defaultNodesUrls = defaultNodesInitial.map { it.host }
            val customNodeRecords = oxyraNodeStorage.getAll().filterNot { defaultNodesUrls.contains(it.url) }
            return try {
                customNodeRecords.map { record ->
                    val uri = record.url.toUri()
                    OxyraNode(
                        host = record.url,
                        name = uri.host ?: "",
                        username = record.username,
                        password = record.password,
                        trusted = record.trusted,
                        serialized = serializeNode(uri, record.username, record.password)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    val allNodes: List<OxyraNode>
        get() = defaultNodes + customNodes

    val currentNode: OxyraNode
        get() {
            Log.d("sidOxyra", "Getting current node")
            val oxyraNodeHost = blockchainSettingsStorage.oxyraNodeHost()
            val rpcSource = allNodes.firstOrNull { it.host == oxyraNodeHost }
            
            val node = rpcSource ?: defaultNodes.first()
            Log.d("sidOxyra", "Current node: ${node.host}")
            return node
        }

    //val blockchain: Blockchain?
    //    get() = marketKitWrapper.blockchain(blockchainType.uid)

    fun save(node: OxyraNode) {
        val record = OxyraNodeRecord(
            url = node.host,
            username = node.username,
            password = node.password,
            trusted = node.trusted
        )
        oxyraNodeStorage.save(record)

        blockchainSettingsStorage.saveOxyraNode(node.host)
        _currentNodeUpdatedFlow.tryEmit(node.host)
    }

    private fun serializeNode(uri: Uri, username: String?, password: String?): String {
        return "$username:$password@${uri.host}:${uri.port}/mainnet/${uri.host ?: ""}"
    }

    fun addOxyraNode(url: String, username: String?, password: String?, trusted: Boolean) {
        val record = OxyraNodeRecord(
            url = url,
            username = username,
            password = password,
            trusted = trusted
        )

        oxyraNodeStorage.save(record)

        customNodes.firstOrNull { it.host == url }?.let {
            save(it)
        }

        _nodesUpdatedFlow.tryEmit(url)
    }

    fun delete(node: OxyraNode) {
        val isCurrent = node == currentNode

        oxyraNodeStorage.delete(node.host)

        if (isCurrent) {
            _currentNodeUpdatedFlow.tryEmit(node.host)
        }

        _nodesUpdatedFlow.tryEmit(node.host)
    }

    data class OxyraNode(
        val host: String,
        val name: String,
        val serialized: String,
        val username: String? = null,
        val password: String? = null,
        val trusted: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            return other is OxyraNode && other.host == this.host && other.trusted == this.trusted
        }

        override fun hashCode(): Int {
            return Objects.hash(host, trusted)
        }
    }
}
