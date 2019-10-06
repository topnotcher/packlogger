package io.bowsers.packlogger

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round


/**
 * A simple [Fragment] subclass.
 * Use the [ShowPack.newInstance] factory method to
 * create an instance of this fragment.
 */
class ShowPack : Fragment() {
    private var packId: Int? = null
    private var packName: String? = null
    private var viewModel: PackHistory? = null

    companion object {
        private const val PACK_ID = "PACK_ID"
        private const val PACK_NAME = "PACK_NAME"

        @JvmStatic
        fun newInstance(packId: Int, packName: String) =
            ShowPack().apply {
                arguments = Bundle().apply {
                    putString(PACK_NAME, packName)
                    putInt(PACK_ID, packId)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            packName = it.getString(PACK_NAME)
            packId = it.getInt(PACK_ID)
        }
    }

    override fun onActivityCreated(saved: Bundle?) {
        super.onActivityCreated(saved)

        val table = createTable()

        viewModel = ViewModelProviders.of(this).get(PackHistory::class.java)?.apply {
            setLoader((activity!! as MainActivity).sheetsLoader)
            getHistory().removeObservers(viewLifecycleOwner)
            getHistory().observe(viewLifecycleOwner, Observer<List<PackHistory.PackData>> { history ->
                updateHistory(table, history)
            })
        }

        val lPackId = packId
        val lPackName = packName
        if (lPackId != null && lPackName != null) {
            showPackForReal(lPackId, lPackName)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.show_pack, container, false)
    }

    private fun showPackForReal(id: Int, name: String) {
        (activity!!.findViewById(R.id.displayed_pack_name) as TextView).text = name
        (activity!!.findViewById(R.id.displayed_pack_id) as TextView).text = id.toString()
        viewModel?.loadHistory(id)
    }

    private fun updateHistory(table: DataTable, history: List<PackHistory.PackData>) {
        val ratingBox = activity!!.findViewById(R.id.displayed_pack_rating) as TextView
        val freshnessBox = activity!!.findViewById(R.id.displayed_pack_freshness) as TextView
        val notesBox = activity!!.findViewById(R.id.displayed_pack_notes) as TextView

        table.clear()

        if (history.isNotEmpty()) {
            table.addRow("Rating", "Date", "Logged By")

            var topUpdated = false
            history.forEach {
                if (!topUpdated) {
                    topUpdated = true

                    ratingBox.text = it.rating.toString()
                    freshnessBox.text = calculateFreshness(it.date)
                    notesBox.text = it.notes
                }
                table.addRow(it.rating.toString(), it.date.split(' ')[0], it.loggedBy)
            }
        } else {
            ratingBox.text = "N/A"
            freshnessBox.text = ""
            notesBox.text = "No records"
        }

    }

    private fun calculateFreshness(dateStr: String) : String {
        var freshness = "?????"
        try {
            val fmt = if (" " in dateStr) "MM/dd/yyyy HH:mm:ss" else "MM/dd/yyyy"
            val date = SimpleDateFormat(fmt).parse(dateStr)
            val weeksAgo = round((Date().time - date.time).toDouble() / 1000 / 86400 / 7).toInt()
            val plural = if (weeksAgo != 1) "s" else ""
            freshness = "$weeksAgo week${plural} ago"
        } catch (e: java.text.ParseException) {
            e.printStackTrace()
        }

        return freshness
    }

    private fun createTable() : DataTable {
        val padVertical = 30
        return DataTable(activity!!.findViewById(R.id.pack_history_table), context).apply {
            setPadding(15, 15, padVertical, padVertical)
            setPadding(5, 5, padVertical, padVertical)
            setPadding(5, 5, padVertical, padVertical)
            setPadding(5, 15, padVertical, padVertical)

            setGravity(Gravity.CENTER)
            setGravity(Gravity.CENTER)
            setGravity(Gravity.CENTER)

            setFontSize(20.0f)
        }
    }
}
