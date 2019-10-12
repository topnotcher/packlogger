package io.bowsers.packlogger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PackList(private val loader: SheetsCollectionLoader) : ViewModel() {
    class Factory(private val loader: SheetsCollectionLoader) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PackList(loader) as T
        }
    }

    data class PackData(var id: Int=0, var name: String="")

    private val packs: MutableLiveData<List<PackData>> by lazy {
        MutableLiveData<List<PackData>>().also {
            loadPacksOnce()
        }
    }

    private var packList: List<PackData>? = null

    private var loadStarted = false
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var loaded = false

    private val table by lazy {
        loader.table<PackData>("all-packs!A2:B").apply {
            setColumnTypes(
                SheetsCollectionLoader.Table.ColumnType.INT,
                SheetsCollectionLoader.Table.ColumnType.STRING
            )
            setRowType(PackData::class.java, "id", "name")
            setCache("packlist", 86400)
        }
    }

    private fun shouldLoad() : Boolean {
        var load = false
        synchronized(loadStarted) {
            if (!loadStarted) {
                loadStarted = true
                load = true
            }
        }
        return load
    }

    private fun loadPacksOnce() {
        if (shouldLoad()) {
            doLoad()
        }
    }

    private fun doLoad() {
        table.select()
            .setResultCallback(this::postValue)
            .executeInBackground()
    }

    fun getPacks(): LiveData<List<PackData>> {
        return packs
    }

    fun getDataSynchronous(): List<PackData> {
        loadPacksOnce()

        lock.withLock {
            if (!loaded) condition.await()
        }

        return packList!!
    }

    private fun postValue(value: List<PackData>) {
        packs.postValue(value)

        // postValue posts to the GUI thread. We can't guarantee packs.value is set when any waiters of
        // condition wake up, so set the data in a separate property as well.
        packList = value

        lock.withLock {
            loaded = true
            condition.signalAll()
        }
    }
}