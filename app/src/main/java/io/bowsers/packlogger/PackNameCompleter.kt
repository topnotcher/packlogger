package io.bowsers.packlogger

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import java.util.*

class PackNameCompleter(
    private val context: Context,
    private val resource: Int,
    private val getData: () -> List<PackList.PackData>
) : BaseAdapter(), Filterable {

    // Only access in getFilter(): lazy loading blocks.
    private val packs: List<PackList.PackData> by lazy {
        getData()
    }

    private var resultList = ArrayList<PackList.PackData>()

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
                    val list = ArrayList<PackList.PackData>(4)

                    packs.forEach {
                        if (constraint != null && it.name!!.toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                            list.add(it)
                        }
                    }

                    this.values = list
                    this.count = list.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if ((results?.count ?: 0) > 0) {
                    resultList = results!!.values as ArrayList<PackList.PackData>
                }
            }
        }
    }
}