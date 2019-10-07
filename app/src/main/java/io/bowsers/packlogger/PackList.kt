package io.bowsers.packlogger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PackList : ViewModel() {
    data class PackData(var id: Int=0, var name: String="")

    private val packs: MutableLiveData<List<PackData>> by lazy {
        MutableLiveData<List<PackData>>().also {
            buildQuery().executeInBackground()
        }
    }

    private var loader: SheetsCollectionLoader? = null

    fun setLoader(loader: SheetsCollectionLoader) {
        this.loader = loader
    }

    fun getPacks(): LiveData<List<PackData>> {
        return packs
    }

    fun getDataSynchronous(): List<PackData> {
        return buildQuery().execute()
    }

    private fun postValue(value: List<PackData>) {
        packs.postValue(value)
    }

    private fun buildQuery() : SheetsCollectionLoader.Query<PackData> {
        val range = "all-packs!A2:B"
        return loader!!.query<PackData>(range).apply {
            columnTypes(
                SheetsCollectionLoader.Query.ColumnType.INT,
                SheetsCollectionLoader.Query.ColumnType.STRING
            )
            unpackRows(PackData::class.java, "id", "name")
            withCache("packlist", 3600)
        }.setResultCallback(this::postValue)
    }

}