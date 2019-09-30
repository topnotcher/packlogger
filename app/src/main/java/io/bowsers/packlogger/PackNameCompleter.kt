package io.bowsers.packlogger

import android.content.Context
import android.os.AsyncTask
import android.util.JsonToken
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import java.io.*
import java.nio.file.Paths
import java.util.*

class PackNameCompleter(
    private val context: Context,
    private val resource: Int,
    private val credential: GoogleAccountCredential?
) : BaseAdapter(), Filterable {

    private data class PackData(var id: Int, var name: String) {
        constructor() : this(0, "N/A")
    }
    private data class CacheData(var timestamp: Long, var data: Array<PackData>) {
        constructor() : this(0, arrayOf())
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
        val cached = readCache()
        var result: List<PackData>? = null

        if (cached != null) {
            val newPacks = ArrayList<PackData>(cached!!.data.size)
            newPacks.addAll(cached.data)
            packs = newPacks
            notifyDataSetChanged()

            // fire the async task, but don't wait
            if (3601 - cached.timestamp > 3600) {
                updatePackData()
            }

        } else {
            // No cache: we need to load the data now. get() waits for the result.
            updatePackData().get()
        }
    }

    private fun updatePackData() : AsyncTask<String, Int, List<PackData>> {
        return object : AsyncTask<String, Int, List<PackData>>() {
            override fun doInBackground(vararg params: String?): List<PackData>? {
                val result = loadPacksForReal()
                updateCache(result)
                packs = result
                notifyDataSetChanged()

                return result
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

    private fun updateCache(cacheList: List<PackData>) {
        val tmpFile = File("${cacheFile}.tmp")

        if (tmpFile.exists()) {
            tmpFile.delete()
        }

        val writer = BufferedWriter(FileWriter(tmpFile))
        val gen = JacksonFactory.getDefaultInstance().createJsonGenerator(writer)
        gen.writeStartObject()
        gen.writeFieldName("timestamp")
        gen.writeNumber(0)

        gen.writeFieldName("data")
        gen.writeStartArray()
        cacheList.forEach{
            gen.writeStartObject()

            gen.writeFieldName("id")
            gen.writeNumber(it.id!!)

            gen.writeFieldName("name")
            gen.writeString(it.name)

            gen.writeEndObject()
        }
        gen.writeEndArray()
        gen.writeEndObject()
        gen.close()

        tmpFile.renameTo(cacheFile)
    }

    private fun readCache(): CacheData? {
        var cached: CacheData? = null
        if (cacheFile.exists()) {
            try {
                val mapper = ObjectMapper()
                cached = mapper.readValue(cacheFile, CacheData::class.java)
            } catch (e: IOException) {
               e.printStackTrace()
            }
        }

        return cached
    }
}