package io.bowsers.packlogger

import android.os.AsyncTask
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import java.io.File
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties

class SheetsCollectionLoader(private val credential: GoogleAccountCredential?) {
    private val SPREADSHEET_ID = "1G8EOexvxcP6n86BQsORNtwxsgpRT-VPrZt07NOZ-q-Q"
    private var cacheDir: File? = null
    private val spreadsheets by lazy {
        val jsonFactory = JacksonFactory.getDefaultInstance()
        val httpTransport = NetHttpTransport()
        val service = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("PackLogger")  // TODO
            .build()

        service.Spreadsheets()
    }

    data class CachedData(var timestamp: Long=0, var data: List<List<Any>>?=null)

    data class CacheResult(var cache: CachedData, val lifetime: Long) {
        val expired get() = (System.currentTimeMillis() / 1000 - cache.timestamp) > lifetime
        val data = cache.data
    }

    private class Cache(val cacheFile: File, val lifetime: Long) {

        val exists: Boolean get() = cacheFile.exists()

        fun update(data: List<List<Any>>) {
            val tmpFile = File("${cacheFile}.tmp")

            if (tmpFile.exists()) {
                tmpFile.delete()
            }

            jacksonObjectMapper().apply {
                writeValue(tmpFile, CachedData(System.currentTimeMillis() / 1000, data))
            }

            tmpFile.renameTo(cacheFile)
        }

        fun getCache(): CacheResult? {
            var result: CacheResult? = null
            if (exists) {
                try {
                    jacksonObjectMapper().apply {
                        result = CacheResult(readValue(cacheFile), lifetime)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    cacheFile.delete()
                }
            }


            return result
        }
    }

    class Table<T: Any>(val loader: SheetsCollectionLoader, val range: String) {
        enum class ColumnType {
            INT,
            LONG,
            DOUBLE,
            SHORT,
            STRING,
        }

        lateinit var columnTypes: Iterable<ColumnType> private set
        lateinit var rowClass: Class<T> private set
        lateinit var columnNames: List<String> private set
        private var cache: Cache? = null

        fun select() : Query<T> {
            return Query(this).apply {

            }
        }

        fun setColumnTypes(vararg types: ColumnType) : Table<T> {
            val newTypes = ArrayList<ColumnType>(types.size)
            newTypes.addAll(types)
            columnTypes = newTypes

            return this
        }

        fun setRowType(clazz: Class<T>, vararg names: String) : Table<T> {
            rowType(clazz)
            columnNames(*names)
            return this
        }

        fun setCache(cacheKey: String, lifetime: Long) : Table<T> {
            val cacheDir = loader.cacheDir
            if (cacheDir != null) {
                val cacheFile = File(
                    arrayOf(
                        cacheDir.toString(),
                        "${cacheKey}.json"
                    ).joinToString(File.separator)
                )
                cache = Cache(cacheFile, lifetime)
            }
            return this
        }

        fun getCache() : CacheResult? {
           return cache?.getCache()
        }

        fun updateCache(data: List<List<Any>>) {
           cache?.update(data)
        }

        private fun rowType(clazz: Class<T>, vararg names: String) {
            rowClass = clazz
            columnNames(*names)
        }

        private fun columnNames(vararg names: String) {
            val out = LinkedList<String>()
            out.addAll(names)
            columnNames = out
        }

    }

    class Query<T: Any>(private val table: Table<T>) {

        private class LoadTask<T: Any> : AsyncTask<Query<T>, Int, List<T>>() {
            override fun doInBackground(vararg params: Query<T>?): List<T>? {
                var result: List<T>? = null

                if (params.size == 1 && params[0] != null) {
                    try {
                        result = params[0]!!.executeForReal()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                return result
            }
        }

        private var resultCallback: ((List<T>) -> Unit)? = null
        private var resultFilter: ((List<T>) -> List<T>)? = null

        fun setResultCallback(callback: (List<T>) -> Unit) : Query<T> {
            resultCallback = callback
            return this
        }

        fun filterResults(callback: (List<T>) -> List<T>) : Query<T> {
            this.resultFilter = callback
            return this
        }
        fun executeInBackground() {
            LoadTask<T>().execute(this)
        }

        fun execute() : List<T> {
            return LoadTask<T>().execute(this).get()
        }

        fun executeForReal() : List<T> {
            var data: List<T>? = null
            val cached = table.getCache()
            var doLoad = false

            if (cached != null) {
                val cachedData = cached.data
                if (cachedData != null) {
                    data = processData(cachedData)
                }

                doLoad = cached.expired
            }

            if (data == null || doLoad) {
                val freshData = table.loader.getData(table.range)
                table.updateCache(freshData)
                data = processData(freshData)
            }

            return data
        }

        private fun processData(data: List<List<Any>>) : List<T> {
            val it = (ensureMutable(data) as MutableList<Any>).listIterator()

            while (it.hasNext()) {
                val values = ensureMutable(it.next() as List<Any>)
                it.set(values)

                convertDataTypes(values)
                it.set(convertRowType(values) as Any)
            }

            val converted = data as List<T>
            val result = resultFilter?.invoke(converted) ?: converted
            resultCallback?.invoke(result)
            return result
        }

        private fun <S> ensureMutable(list: List<S>) : MutableList<S> {
            return when (list) {
                is MutableList -> list
                else -> ArrayList(list)
            }
        }

        private fun convertDataTypes(values: MutableList<Any>) {
            val vit = values.listIterator()
            val tit = table.columnTypes.iterator()

            while (vit.hasNext() && tit.hasNext()) {
                val value = vit.next() as String
                val type = tit.next()

                vit.set(convertDataType(value, type))
            }
        }

        private fun convertDataType(value: String, type: Table.ColumnType) : Any {
            return when (type) {
                Table.ColumnType.INT -> value.toInt()
                Table.ColumnType.LONG -> value.toLong()
                Table.ColumnType.DOUBLE -> value.toDouble()
                Table.ColumnType.SHORT -> value.toShort()
                Table.ColumnType.STRING -> value
            }
        }

        private fun convertRowType(values: List<Any>) : T {
            val obj: T = table.rowClass.newInstance()

            val nit = table.columnNames.iterator()
            val vit = values.iterator()

            while (vit.hasNext() && nit.hasNext()) {
                val colName = nit.next()
                val value = vit.next()

                val prop = obj::class.memberProperties.find { it.name == colName }
                if (prop is KMutableProperty<*>)
                    prop.setter.call(obj, value)
            }


            return obj
        }
    }

    fun <T: Any>table(range: String) : Table<T> {
       return Table(this, range)
    }

    private fun getData(range: String) : List<List<Any>> {
        val response = spreadsheets.values().get(SPREADSHEET_ID, range).execute()
        return response.getValues()
    }

    fun setCacheDir(cacheDir: File) {
        this.cacheDir = cacheDir
    }
}
