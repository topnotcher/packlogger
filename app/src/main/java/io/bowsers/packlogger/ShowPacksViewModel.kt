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
        var range: String
        if (selection == "top_packs") {
            range = "TOP!A2:D"
        } else {
            range = "ALL!A2:D"
        }

        return loader!!.query<PackData>(range).apply {
            columnTypes(ColumnType.INT, ColumnType.STRING, ColumnType.DOUBLE, ColumnType.STRING)
            unpackRows(PackData::class.java, "id", "name", "rating", "date")
            withCache("showpacks-${selection}", 1200)
        }.setResultCallback(this::postValue)
    }
}
