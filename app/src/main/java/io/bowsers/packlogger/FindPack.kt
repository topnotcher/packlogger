package io.bowsers.packlogger


import android.content.Context
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.SheetsScopes


/**
 * A simple [Fragment] subclass.
 * Use the [FindPack.newInstance] factory method to
 * create an instance of this fragment.
 */
class FindPack : Fragment() {
    private var adapter: PackNameCompleter? = null
    private var displayedPack: Int? = null
    private var textChangedHandler: Handler? = null
    private var viewModel: PackList? = null
    private var packs: List<PackList.PackData>? = null

    companion object {
        private const val KEY_DISPLAYED_PACK = "DISPLAYED_PACK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(saved: Bundle?) {
        super.onActivityCreated(saved)
        viewModel = ViewModelProviders.of(this).get(PackList::class.java)

        val scopes = listOf(SheetsScopes.SPREADSHEETS)
        val credential =
            GoogleAccountCredential.usingOAuth2(activity!!.applicationContext, scopes)
        val aacount = (activity!! as MainActivity).account?.account
        credential!!.selectedAccount = aacount

        viewModel!!.setCredential(credential)
        viewModel!!.setCacheDirectory(context!!.cacheDir)

        adapter =
            PackNameCompleter(context!!, R.layout.support_simple_spinner_dropdown_item, viewModel!!::getDataSynchronous)
        val tv: AutoCompleteTextView = activity!!.findViewById(R.id.pack_search_str)
        tv.setAdapter(adapter)
        tv.threshold = 1

        configureSearchActions(tv)

        // TODO: This won't work because the packs list is not loaded...
        if (saved != null) {
            if (saved.containsKey(KEY_DISPLAYED_PACK)) {
                displayedPack = saved.getInt(KEY_DISPLAYED_PACK)

            } else {
                showKeyboard()
            }
        } else {
            showKeyboard()
        }

        packs = viewModel!!.getPacks().value
        viewModel!!.getPacks().removeObservers(viewLifecycleOwner)
        viewModel!!.getPacks().observe(viewLifecycleOwner, Observer<List<PackList.PackData>> { packs->
            this.packs = packs
            if (displayedPack != null) {
                showPackForReal(displayedPack!!)
            }
        })
    }

    override fun onSaveInstanceState(saved: Bundle) {
        if (displayedPack != null) {
            saved.putInt(KEY_DISPLAYED_PACK, displayedPack!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_pack, container, false)
    }

    fun showPack(searchStr: String): Boolean {
        var id: Int? = null

        try {
            id = packs?.find{it.id == searchStr.toInt()}?.id

        } catch (e: NumberFormatException) {
            id = packs?.find{searchStr.toLowerCase() == it.name.toLowerCase()}?.id
        }

        if (id != null)
            showPack(id)

        return true
    }

    private fun showPack(id: Int): Boolean {

        if (id != displayedPack) {
            showPackForReal(id)
        }

        return true
    }

    private fun showPackForReal(id: Int) {
        displayedPack = id
        val pack = packs?.find{it.id == id}


        if (pack != null) {
            // If we display a pack and it's the same pack that's in the search box,
            // clear the search box so it is ready for a new search
            val tv = activity!!.findViewById(R.id.pack_search_str) as TextView
            val search = tv.text.toString().toLowerCase()
            if (pack.name?.toLowerCase() == search || pack.id?.toString() == search) {
               tv.text = ""
            }

            (activity!!.findViewById(R.id.displayed_pack_name) as TextView).text = pack.name
            (activity!!.findViewById(R.id.displayed_pack_id) as TextView).text = pack.id.toString()
        }


        //Log.i("FindPack", "showing Pack ${name}, $id")

        //if (name != null) {

            // Hide the section with the pack we're trying to show.
            //(activity!!.findViewById(R.id.show_pack_layout) as View).visibility = View.INVISIBLE
        //}
    }

    private fun configureSearchActions(tv: AutoCompleteTextView) {
        tv.setImeActionLabel("Show", EditorInfo.IME_ACTION_DONE)

        tv.setOnItemClickListener { parent, view, position, id ->
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

    private fun showKeyboard() {
        (activity!!.findViewById(R.id.pack_search_str) as View).apply {
            requestFocus()
        }
        (activity!!.findViewById(R.id.pack_search_str) as View).apply {
            val imm =
                activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}
