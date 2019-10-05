package io.bowsers.packlogger

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import io.bowsers.packlogger.SheetsCollectionLoader.Query.ColumnType
import java.io.File
import java.util.*

class ShowPacksViewModel : ViewModel() {
    data class PackData (var id: Int, var name: String, var rating: Double, var date: String) {
        constructor(): this(0, "N/A", 0.0, "")
    }

    private class LoadRequest<T>(private val query: SheetsCollectionLoader.Query<T>, private val resultCallback: (T) -> Unit) {
        fun postResult(result: T) {
            resultCallback(result)
        }

        fun execute() : T {
            return query.execute() as T
        }
    }

    private class LoadTask<T> : AsyncTask<SheetsCollectionLoader.Query<T>, Int, List<T>>() {
        override fun doInBackground(vararg params: SheetsCollectionLoader.Query<T>?): List<T>? {
            var result: List<T>? = null

            if (params.size == 1 && params[0] != null) {
                result = params[0]!!.execute()
            }

            return result
        }
    }

    private var credential: GoogleAccountCredential? = null
    private var selection: String? = null
    private var cacheDir: File? = null

    private val packs: MutableLiveData<List<PackData>> by lazy {
        MutableLiveData<List<PackData>>().also {
            loadPacks()
        }
    }

    companion object {
        const val SELECT_TOP_PACKS: String = "top_packs"
        const val SELECT_ALL_PACKS: String = "all_packs"
    }

    fun setCredential(credential : GoogleAccountCredential?) {
        this.credential = credential
    }

    fun setSelection(selection: String?) {
        this.selection = selection
    }

    fun setCacheDirectory(dir: File) {
        cacheDir = dir
    }

    fun getPacks(): LiveData<List<PackData>> {
        return packs
    }

    private fun loadPacks() {
        LoadTask<PackData>().execute(buildQuery())
    }

    private fun postValue(result: List<PackData>) {
        // We need this function because we can't pass packs::postValue in in loadPacks: packs is
        // lazy initialized and loadPacks is the initializer. This causes infinite recursion.
        packs.postValue(result)
    }

    private fun buildQuery() : SheetsCollectionLoader.Query<PackData> {
        var range: String
        if (selection == "top_packs") {
            range = "TOP!A2:D"
        } else {
            range = "ALL!A2:D"
        }

        return SheetsCollectionLoader<PackData>(credential).query(range).apply {
            columnTypes(ColumnType.INT, ColumnType.STRING, ColumnType.DOUBLE, ColumnType.STRING)
            unpackRows(PackData::class.java as Class<Any>, "id", "name", "rating", "date")
            if (cacheDir != null) {
                val cacheFile = File(arrayOf(cacheDir.toString(), "showpacks-${selection}.json").joinToString(File.separator))
                withCache(cacheFile, 1200)
            }
        }.setResultCallback(this::postValue)
    }
}
