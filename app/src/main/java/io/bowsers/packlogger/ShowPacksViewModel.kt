package io.bowsers.packlogger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.bowsers.packlogger.SheetsCollectionLoader.Table.ColumnType
import java.lang.IllegalArgumentException

class ShowPacksViewModel(private val selection: String, private val loader: SheetsCollectionLoader) : ViewModel() {

    class Factory(private val selection: String, private val loader: SheetsCollectionLoader)
        : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
           return ShowPacksViewModel(selection, loader) as T
        }
    }

    data class PackData (var id: Int=0, var name: String="", var rating: Double=0.0, var date: String="")

    private val packs: MutableLiveData<List<PackData>> by lazy {
        MutableLiveData<List<PackData>>().also {
            table.select().let {
                it.setResultCallback(this::postValue)
            }.executeInBackground()
        }
    }

    private val range by lazy {
        val columns = "A2:D"
        when (selection) {
            SELECT_TOP_PACKS -> "TOP!$columns"
            SELECT_ALL_PACKS -> "ALL!$columns"
            else -> throw IllegalArgumentException()
        }
    }

    private val table by lazy {
        loader.table<PackData>(range).apply {
            setColumnTypes(ColumnType.INT, ColumnType.STRING, ColumnType.DOUBLE, ColumnType.STRING)
            setRowType(PackData::class.java, "id", "name", "rating", "date")
            setCache("showpacks-${selection}", 1200)
        }
    }

    companion object {
        const val SELECT_TOP_PACKS: String = "top_packs"
        const val SELECT_ALL_PACKS: String = "all_packs"
    }

    fun getPacks(): LiveData<List<PackData>> {
        return packs
    }

    private fun postValue(result: List<PackData>) {
        // We need this function because we can't pass packs::postValue in in loadPacks: packs is
        // lazy initialized and loadPacks is the initializer. This causes infinite recursion.
        packs.postValue(result)
    }
}
