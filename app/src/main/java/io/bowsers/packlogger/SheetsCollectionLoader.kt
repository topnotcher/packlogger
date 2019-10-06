package io.bowsers.packlogger

import android.os.AsyncTask
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties

class SheetsCollectionLoader(private val credential: GoogleAccountCredential?) {
    val SPREADSHEET_ID = "1G8EOexvxcP6n86BQsORNtwxsgpRT-VPrZt07NOZ-q-Q"
    private var cacheDir: File? = null


    private class Cache(val cacheFile: File, val lifetime: Long) {

        data class CachedData(var timestamp: Long, var data: List<List<Any>>?) {
            constructor() : this(0, null)
        }

        data class CacheResult(var cache: CachedData, val lifetime: Long) {
            val expired get() = (System.currentTimeMillis() / 1000 - cache.timestamp) > lifetime
            val data = cache.data
        }

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

    class Query<T: Any>(private val loader: SheetsCollectionLoader,  private val range: String) {

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

        enum class ColumnType {
            INT,
            LONG,
            DOUBLE,
            SHORT,
            STRING,
        }

        private var mappedTypes: List<ColumnType>? = null
        private var mappedRowType: Class<T>? = null
        private var mappedFieldNames: List<String>? = null
        private var cache: Cache? = null
        private var resultCallback: ((List<T>) -> Unit)? = null
        private var resultFilter: ((List<T>) -> List<T>)? = null

       fun columnTypes(vararg types: ColumnType) : Query<T> {
            val newTypes = ArrayList<ColumnType>(types.size)
            newTypes.addAll(types)
            mappedTypes = newTypes

            return this
        }

        fun unpackRows(clazz: Class<T>, vararg names: String) : Query<T> {
            rowType(clazz)
            columnNames(*names)
            return this
        }

        fun withCache(cacheKey: String, lifetime: Long) : Query<T> {
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

        fun setResultCallback(callback: (List<T>) -> Unit) : Query<T> {
            resultCallback = callback
            return this
        }

        fun filterResults(callback: (List<T>) -> List<T>) : Query<T> {
            this.resultFilter = callback
            return this
        }

        private fun rowType(clazz: Class<T>, vararg names: String) {
            mappedRowType = clazz
            columnNames(*names)
        }

        private fun columnNames(vararg names: String) {
            val out = LinkedList<String>()
            out.addAll(names)
            mappedFieldNames = out
        }

        fun executeInBackground() {
            LoadTask<T>().execute(this)
        }

        fun execute() : List<T> {
            return LoadTask<T>().execute(this).get()
        }

        fun executeForReal() : List<T> {
            var data: List<T>? = null
            val cached = cache?.getCache()
            var doLoad = false

            if (cached != null) {
                val cachedData = cached.data
                if (cachedData != null) {
                    data = processData(cachedData)
                }

                doLoad = cached.expired
            }

            if (data == null || doLoad) {
                val freshData = loader.getData(range)
                cache?.apply{update(freshData)}

                data = processData(freshData)
            }

            return data
        }

        private fun processData(data: List<List<Any>>) : List<T> {
            val it = (ensureMutable(data) as MutableList<Any>).listIterator()

            while (it.hasNext()) {
                val values = ensureMutable(it.next() as List<Any>)
                it.set(values)

                if (mappedTypes != null)
                    convertDataTypes(values)
                else
                    throw IllegalStateException()

                if (mappedFieldNames != null && mappedRowType != null) {
                    it.set(convertRowType(values) as Any)
                } else {
                    throw IllegalStateException()
                }
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
            val tit = mappedTypes!!.iterator()

            while (vit.hasNext() && tit.hasNext()) {
                val value = vit.next() as String
                val type = tit.next()

                vit.set(convertDataType(value, type))
            }
        }

        private fun convertDataType(value: String, type: ColumnType) : Any {
            return when (type) {
                ColumnType.INT -> value.toInt()
                ColumnType.LONG -> value.toLong()
                ColumnType.DOUBLE -> value.toDouble()
                ColumnType.SHORT -> value.toShort()
                ColumnType.STRING -> value
            }
        }

        private fun convertRowType(values: List<Any>) : T {
            val obj: T = mappedRowType!!.newInstance()

            val nit = mappedFieldNames!!.iterator()
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

    fun <T: Any> query(range: String) : Query<T> {
        return Query(this, range)
    }

    private fun getData(range: String) : List<List<Any>> {
        val jsonFactory = JacksonFactory.getDefaultInstance()
        val httpTransport = NetHttpTransport()
        val service = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("PackLogger")  // TODO
            .build()

        val response = service.Spreadsheets().values().get(SPREADSHEET_ID, range).execute()
        return response.getValues()
    }

    fun setCacheDir(cacheDir: File) {
        this.cacheDir = cacheDir
    }
}
