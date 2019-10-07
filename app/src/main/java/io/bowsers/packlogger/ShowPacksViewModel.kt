package io.bowsers.packlogger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.bowsers.packlogger.SheetsCollectionLoader.Query.ColumnType

class ShowPacksViewModel : ViewModel() {
    data class PackData (var id: Int=0, var name: String="", var rating: Double=0.0, var date: String="")

    private var selection: String? = null
    private var loader: SheetsCollectionLoader? = null

    private val packs: MutableLiveData<List<PackData>> by lazy {
        MutableLiveData<List<PackData>>().also {
            loadPacks()
        }
    }

    companion object {
        const val SELECT_TOP_PACKS: String = "top_packs"
        const val SELECT_ALL_PACKS: String = "all_packs"
    }

    fun setSelection(selection: String?) {
        this.selection = selection
    }

    fun setLoader(loader: SheetsCollectionLoader) {
        this.loader = loader
    }

    fun getPacks(): LiveData<List<PackData>> {
        return packs
    }

    private fun loadPacks() {
        buildQuery().executeInBackground()
    }

    private fun postValue(result: List<PackData>) {
        // We need this function because we can't pass packs::postValue in in loadPacks: packs is
        // lazy initialized and loadPacks is the initializer. This causes infinite recursion.
        packs.postValue(result)
    }

    private fun buildQuery() : SheetsCollectionLoader.Query<PackData> {
        val range =
            if (selection == "top_packs") {
                "TOP!A2:D"
            } else {
                "ALL!A2:D"
            }

        return loader!!.query<PackData>(range).apply {
            columnTypes(ColumnType.INT, ColumnType.STRING, ColumnType.DOUBLE, ColumnType.STRING)
            unpackRows(PackData::class.java, "id", "name", "rating", "date")
            withCache("showpacks-${selection}", 1200)
        }.setResultCallback(this::postValue)
    }
}
