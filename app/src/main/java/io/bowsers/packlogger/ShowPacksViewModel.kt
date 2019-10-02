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
        constructor(): this(0, "N/A", 0.0, "1/1/1970")
    }

    private var credential: GoogleAccountCredential? = null
    private var selection: String? = null
    private var packList: List<PackData> = LinkedList()
    private var cacheDir: File? = null

    val packs: MutableLiveData<List<PackData>> by lazy {
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
        object: AsyncTask<String, Int, List<PackData>>() {
            override fun doInBackground(vararg params: String?): List<PackData> {
                return loadPacksForReal()
            }

            override fun onPostExecute(result: List<PackData>) {
                super.onPostExecute(result)
                if (packList != null)
                    packs.postValue(result)
            }
        }.execute("foo")
      }


    //private val cacheFile: File by lazy {
        //File(arrayOf(context.cacheDir.toString(), "packlist.json").joinToString(File.separator))
    //}
    private fun loadPacksForReal() : List<PackData> {
        var range: String
        if (selection == "top_packs") {
            range = "TOP!A2:D"
        } else {
            range = "ALL!A2:D"
        }

        return SheetsCollectionLoader(credential).query(range).apply {
            columnTypes(ColumnType.INT, ColumnType.STRING, ColumnType.DOUBLE, ColumnType.STRING)
            unpackRows(PackData::class.java as Class<Any>, "id", "name", "rating", "date")
            if (cacheDir != null) {
                val cacheFile = File(arrayOf(cacheDir.toString(), "showpacks-${selection}.json").joinToString(File.separator))
                withCache(cacheFile, 1200)
            }
        }.execute() as List<PackData>
    }
}
