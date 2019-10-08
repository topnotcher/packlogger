package io.bowsers.packlogger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PackList(private val loader: SheetsCollectionLoader) : ViewModel() {
    class Factory(private val loader: SheetsCollectionLoader) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PackList(loader) as T
        }
    }

    data class PackData(var id: Int=0, var name: String="")

    private val packs: MutableLiveData<List<PackData>> by lazy {
        MutableLiveData<List<PackData>>().also {
            table.select().let {
                it.setResultCallback(this::postValue)
            }.executeInBackground()
        }
    }

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

    fun getPacks(): LiveData<List<PackData>> {
        return packs
    }

    fun getDataSynchronous(): List<PackData> {
        return table.select().execute()
    }

    private fun postValue(value: List<PackData>) {
        packs.postValue(value)
    }
}