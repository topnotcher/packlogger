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
import java.util.*

class ShowPacksViewModel : ViewModel() {
    data class PackData (val id: Int, val name: String, val rating: Double, val date: String)

    private var credential: GoogleAccountCredential? = null
    private var selection: String? = null
    private var packList: List<PackData> = LinkedList<PackData>()

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

        //packList.clear()

        //for (i in 1..10) {
         //  packList.add(i)
        //}

    private fun loadPacksForReal() : List<PackData> {
        val spreadsheetId = "1G8EOexvxcP6n86BQsORNtwxsgpRT-VPrZt07NOZ-q-Q"
        val jsonFactory = JacksonFactory.getDefaultInstance()
        //GoogleNetHttpTransport.newTrustedTransport()
        val httpTransport = NetHttpTransport()
        val service = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("PackLogger")  // TODO
            .build()
        //.setApplicationName(getString(R.string.app_name))
        val range = "Ratings!A2:D"
        val response = service.Spreadsheets().values().get(spreadsheetId, range).execute()
        val values: List<List<Any>> = response.getValues()

        val newList = LinkedList<PackData>()

        values.forEach {row ->
            if (row.size >= 4) {
                val id = (row[0] as String).toInt()
                val name = row[1] as String
                val rating = (row[2] as String).toDouble()
                val date = row[3] as String

                newList.add(PackData(id, name, rating, date))
            }
        }
        return newList
    }
}
