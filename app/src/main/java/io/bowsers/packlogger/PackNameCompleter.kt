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
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import io.bowsers.packlogger.SheetsCollectionLoader.Query.ColumnType
import java.io.*
import java.util.*

class PackNameCompleter(
    private val context: Context,
    private val resource: Int,
    private val credential: GoogleAccountCredential?
) : BaseAdapter(), Filterable {

    private data class PackData(var id: Int, var name: String) {
        constructor() : this(0, "N/A")
    }

    private var resultList = ArrayList<PackData>()

    private val cacheFile: File by lazy {
        File(arrayOf(context.cacheDir.toString(), "packlist.json").joinToString(File.separator))
    }
    private var packs: List<PackData>? = null
    //init {
        //packs
            //.observe(this, Observer<List<ShowPacksViewModel.PackData>> { packs ->
                //updatePacks(packs)
            //})
//
    //}

    override fun getCount(): Int {
        return resultList.size
    }

    override fun getItem(position: Int) : String {
        return resultList[position].name!!
    }

    override fun getItemId(position: Int) : Long {
        return resultList[position].id!!.toLong()
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

                    if (packs == null) {
                        loadPacks()
                    }

                    if (packs != null) {
                        packs!!.forEach {
                            if (constraint != null && it.name!!.toLowerCase().startsWith(constraint.toString().toLowerCase())) {
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
                }
            }
        }
    }

    private fun loadPacks() {
        packs = loadPacksForReal()
    }

    private fun updatePackData() : AsyncTask<String, Int, List<PackData>> {
        return object : AsyncTask<String, Int, List<PackData>>() {
            override fun doInBackground(vararg params: String?): List<PackData>? {
                val result = loadPacksForReal()
                //cache.updateCache(result)
                packs = result

                return result
            }

            override fun onPostExecute(result: List<PackData>?) {
                super.onPostExecute(result)
                notifyDataSetChanged()
            }
        }.execute()
    }

    private fun loadPacksForReal() : List<PackData> {
        val range = "all-packs!A2:B"
        return SheetsCollectionLoader(credential).query(range).apply {
            columnTypes(ColumnType.INT, ColumnType.STRING)
            unpackRows(PackData::class.java as Class<Any>, "id", "name")
            withCache(cacheFile, 3600)
        }.execute() as List<PackData>
    }
}