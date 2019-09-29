package io.bowsers.packlogger

import android.content.Context
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import java.util.*

class PackNameCompleter(
    private val context: Context,
    private val resource: Int,
    private val credential: GoogleAccountCredential?
) : BaseAdapter(), Filterable {

    private data class PackData(val id: Int, val name: String)
    private var resultList = ArrayList<PackData>()
    //private var packList = arrayOf(
        //PackData(1, "Trident"),
        //PackData(2, "Blade"),
        //PackData(3, "Talon"),
        //PackData(4, "Umbriel"),
        //PackData(5, "Oberon")
    //)

    private val packs: MutableLiveData<List<PackData>> by lazy {
        MutableLiveData<List<PackData>>().also {
            loadPacks()
        }
    }

    override fun getCount(): Int {
        return resultList.size
    }

    override fun getItem(position: Int) : String {
        return resultList[position].name
    }

    override fun getItemId(position: Int) : Long {
        return resultList[position].id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return (convertView ?: {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            inflater.inflate(resource, parent, false)
        }()).apply {this as TextView
            this.text = getItem(position)
        }
    }

    override fun getFilter() : Filter {
        return object: Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults().apply {
                    // initial size >= 2: there are at most two packs that start with any given letter.
                    val list = ArrayList<PackData>(4)

                    if (packs.value != null) {
                        packs.value!!.forEach {
                            if (constraint != null && it.name.toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                                list.add(it)
                            }
                        }
                    }

                    this.values = list
                    this.count = list.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if ((results?.count ?: 0) > 0) {
                    resultList = results!!.values as ArrayList<PackData>
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    private fun loadPacks() {
        object: AsyncTask<String, Int, List<PackData>>() {
            override fun doInBackground(vararg params: String?): List<PackData> {
                return loadPacksForReal()
            }

            override fun onPostExecute(result: List<PackData>) {
                super.onPostExecute(result)
                if (result != null)
                    packs.postValue(result)
            }
        }.execute()
    }

    private fun loadPacksForReal() : List<PackData> {
        val spreadsheetId = "1G8EOexvxcP6n86BQsORNtwxsgpRT-VPrZt07NOZ-q-Q"
        val jsonFactory = JacksonFactory.getDefaultInstance()
        //GoogleNetHttpTransport.newTrustedTransport()
        val httpTransport = NetHttpTransport()
        val service = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("PackLogger")  // TODO
            .build()
        //.setApplicationName(getString(R.string.app_name))

        var range = "all-packs!A2:B"
        val response = service.Spreadsheets().values().get(spreadsheetId, range).execute()
        val values: List<List<Any>> = response.getValues()

        val newList = ArrayList<PackData>(values.size)

        values.forEach { row ->
            val id = (row[0] as String).toInt()
            val name = row[1] as String

            newList.add(PackData(id, name))
        }

        return newList
    }
}