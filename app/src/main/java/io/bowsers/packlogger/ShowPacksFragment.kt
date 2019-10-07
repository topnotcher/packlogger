package io.bowsers.packlogger

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer

class ShowPacksFragment : Fragment() {

    private val viewModel: ShowPacksViewModel by lazy {
        val factory = ShowPacksViewModel.Factory(selection!!, (activity!! as MainActivity).sheetsLoader)
        ViewModelProviders.of(this, factory).get(ShowPacksViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selection = it.getString(SELECTION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.show_packs_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (table == null) {
            val padVertical = 30
            table = DataTable(activity!!.findViewById(R.id.packs_table), context)
            table!!
                .setPadding(15, 15, padVertical, padVertical)
                .setPadding(5, 5, padVertical, padVertical)
                .setPadding(5, 5, padVertical, padVertical)
                .setPadding(5, 15, padVertical, padVertical)

                .setGravity(Gravity.RIGHT)
                .setGravity(Gravity.LEFT)
                .setGravity(Gravity.CENTER)
                .setGravity(Gravity.RIGHT)

                .setFontSize(20.0f)
        }

        viewModel.getPacks()
            .observe(this, Observer<List<ShowPacksViewModel.PackData>> { packs ->
                updatePacks(packs)
        })
    }

    private fun updatePacks(packs: List<ShowPacksViewModel.PackData>) {
        val t = table!!
        t.clear()
        t.addRow("ID", "Name", "Rating", "Date")
        packs.forEach {
            var rating = ""
            if (it.rating >= 1) {
                rating = it.rating.toString()
            }

            t.addRow(it.id.toString(), it.name, rating, it.date.split(' ')[0])
        }
    }

    private var selection: String? = null
    private var table: DataTable? = null

    companion object {
        private const val SELECTION = "SELECT"

        @JvmStatic
        fun newInstance(selection: String) =
            ShowPacksFragment().apply {
                arguments = Bundle().apply {
                    putString(SELECTION, selection)
                }
            }
    }
}
