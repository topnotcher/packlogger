package io.bowsers.packlogger

import android.content.Context
import androidx.lifecycle.ViewModelProviders
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer

class ShowPacksFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.show_packs_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.getPacks().apply{
            removeObservers(viewLifecycleOwner)
            observe(viewLifecycleOwner, Observer<List<ShowPacksViewModel.PackData>> { packs ->
                updatePacks(packs)
            })
        }
    }

    private fun updatePacks(packs: List<ShowPacksViewModel.PackData>) {
        val t = createTable()
        t.clear()
        t.addRow("ID", "Name", "Rating", "Date")
        packs.forEach {pack ->
            var rating = ""
            if (pack.rating >= 1) {
                rating = pack.rating.toString()
            }

            t.addRow(pack.id.toString(), pack.name, rating, pack.date.split(' ')[0]).apply {
               setOnClickListener{
                   onButtonPressed(pack.id, pack.name)
               }
            }
        }
    }

    private var selection: String? = null

    private fun createTable() : DataTable {
        return DataTable(activity!!.findViewById(R.id.packs_table), context).apply {
            val padVertical = 30
            setPadding(15, 15, padVertical, padVertical)
            setPadding(5, 5, padVertical, padVertical)
            setPadding(5, 5, padVertical, padVertical)
            setPadding(5, 15, padVertical, padVertical)

            setGravity(Gravity.RIGHT)
            setGravity(Gravity.LEFT)
            setGravity(Gravity.CENTER)
            setGravity(Gravity.RIGHT)

            setFontSize(20.0f)
        }
    }

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

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(id: Int, name: String) {
        listener?.onShowPacksFragmentInteraction(id, name)
    }

    interface OnFragmentInteractionListener {
        fun onShowPacksFragmentInteraction(id: Int, name: String)
    }
}
