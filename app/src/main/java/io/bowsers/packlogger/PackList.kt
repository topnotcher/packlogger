package io.bowsers.packlogger

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import java.io.File

class PackList : ViewModel() {
    public data class PackData(var id: Int, var name: String) {
        constructor() : this(0, "N/A")
    }

    private val packs: MutableLiveData<List<PackData>> by lazy {
        MutableLiveData<List<PackData>>().also {
            buildQuery().executeInBackground()
        }
    }

    private var loader: SheetsCollectionLoader? = null
    private var cacheDir: File? = null
    private val cacheFile: File by lazy {
        File(arrayOf(cacheDir.toString(), "packlist.json").joinToString(File.separator))
    }

    fun setLoader(loader: SheetsCollectionLoader) {
        this.loader = loader
    }

    fun setCacheDirectory(cacheDir: File) {
        this.cacheDir = cacheDir
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
            withCache(cacheFile, 3600)
        }.setResultCallback(this::postValue)
    }

}