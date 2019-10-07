package io.bowsers.packlogger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.*

class PackHistory(private val loader: SheetsCollectionLoader) : ViewModel() {
    class Factory(private val loader: SheetsCollectionLoader) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PackHistory(loader) as T
        }
    }

    data class PackData(var id: Int=0, var rating: Double=0.0, var date: String="",
                        var loggedBy: String="", var notes: String="")

    private val history: MutableLiveData<List<PackData>> by lazy {
        MutableLiveData<List<PackData>>()
    }

    fun getHistory(): LiveData<List<PackData>> {
        return history
    }

    private fun postValue(value: List<PackData>) {
        history.postValue(value)
    }

    fun loadHistory(packId: Int) {
        buildQuery(packId).executeInBackground()
    }

    private fun filterResults(packId: Int, results: List<PackData>) : List<PackData> {
        // I initially wanted to bisect this, but: List gives no guarantee of random access
        // (although I know it is an ArrayList) and retrieving the data was already O(1) anyway
        // (it had to go through conversion to PackData), so there's not much benefit... Make it
        // work today. Make it fast next week. (Hi next week me. Sorry.)
        val filtered = LinkedList<PackData>()
        var found = false
        for (result in results) {
            if (result.id == packId) {
                found = true
                filtered.add(result)
            } else if (found) {
               break
            }
        }

        return filtered
    }

    private fun buildQuery(packId: Int) : SheetsCollectionLoader.Query<PackData> {
        val range = "logs-sorted!A2:E"
        return loader.query<PackData>(range).apply {
            columnTypes(
                SheetsCollectionLoader.Query.ColumnType.INT,
                SheetsCollectionLoader.Query.ColumnType.DOUBLE,
                SheetsCollectionLoader.Query.ColumnType.STRING,
                SheetsCollectionLoader.Query.ColumnType.STRING,
                SheetsCollectionLoader.Query.ColumnType.STRING
            )
            unpackRows(PackData::class.java, "id", "rating", "date", "loggedBy", "notes")
            withCache("logs", 1200)
        }.setResultCallback(this::postValue).filterResults{results -> filterResults(packId, results)}
    }
}