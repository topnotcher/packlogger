package io.bowsers.packlogger


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders


/**
 * A simple [Fragment] subclass.
 * Use the [FindPack.newInstance] factory method to
 * create an instance of this fragment.
 */
class FindPack : Fragment() {
    private var adapter: PackNameCompleter? = null
    private var packs: List<PackList.PackData>? = null
    private val viewModel: PackList by lazy {
        val factory = PackList.Factory((activity!! as MainActivity).sheetsLoader)
        ViewModelProviders.of(this, factory).get(PackList::class.java)
    }

    override fun onActivityCreated(saved: Bundle?) {
        super.onActivityCreated(saved)

        adapter =
            PackNameCompleter(context!!, R.layout.support_simple_spinner_dropdown_item, viewModel::getDataSynchronous)
        val tv: AutoCompleteTextView = activity!!.findViewById(R.id.pack_search_str)
        tv.setAdapter(adapter)
        tv.threshold = 1

        configureSearchActions(tv)

        viewModel.getPacks().removeObservers(viewLifecycleOwner)
        viewModel.getPacks().observe(viewLifecycleOwner, Observer<List<PackList.PackData>> { packs->
            this.packs = packs
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_pack, container, false)
    }

    private fun showPack(searchStr: String): Boolean {
        val id = try {
            packs?.find{it.id == searchStr.toInt()}?.id

        } catch (e: NumberFormatException) {
            packs?.find{searchStr.toLowerCase() == it.name.toLowerCase()}?.id
        }

        if (id != null)
            showPack(id)

        return true
    }

    private fun showPack(id: Int): Boolean {
        showPackForReal(id)
        return true
    }

    private fun showPackForReal(id: Int) {
        val pack = packs?.find{it.id == id}

        if (pack != null) {
            // If we display a pack and it's the same pack that's in the search box,
            // clear the search box so it is ready for a new search. If there's not a match,
            // this is either a typo or the the activity's state was saved with a displayed
            // pack and a partial search -- retain the contents in that case.
            val tv = activity!!.findViewById(R.id.pack_search_str) as AutoCompleteTextView
            val search = tv.text.toString().toLowerCase()
            if (pack.name?.toLowerCase() == search || pack.id?.toString() == search) {
                val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(tv.applicationWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                tv.text.clear()
            }

            activity!!.supportFragmentManager
                .beginTransaction()
                .replace(R.id.content_area, ShowPack.newInstance(pack.id, pack.name))
                .commit()
        }
    }

    private fun configureSearchActions(tv: AutoCompleteTextView) {
        tv.setImeActionLabel("Show", EditorInfo.IME_ACTION_DONE)

        tv.setOnItemClickListener { parent, _, position, _ ->
            showPack(parent.getItemAtPosition(position) as String)
        }

        tv.setOnEditorActionListener { v, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_DONE) && showPack(v.text.toString())
        }

        (activity!!.findViewById(R.id.show_pack) as Button).apply {
            setOnClickListener {
                showPack(tv.text.toString())
            }
        }
    }
}
