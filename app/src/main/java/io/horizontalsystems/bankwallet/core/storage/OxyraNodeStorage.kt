package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.OxyraNodeRecord

class OxyraNodeStorage(private val oxyraNodeDao: OxyraNodeDao) {

    fun save(oxyraNodeRecord: OxyraNodeRecord) {
        oxyraNodeDao.insert(oxyraNodeRecord)
    }

    fun delete(url: String) {
        oxyraNodeDao.delete(url)
    }

    fun getAll(): List<OxyraNodeRecord> {
        return oxyraNodeDao.getAll()
    }

}
