package io.bowsers.packlogger

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.children
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.Observer
import java.text.SimpleDateFormat
import java.util.*


private const val DATE_FORMAT = "MM/dd/yyyy HH:mm:ss"

class LogHistory : Fragment() {

    private val packHistory by lazy {
        val factory = PackHistory.Factory((activity!! as MainActivity).sheetsLoader)
        val viewModel = ViewModelProviders.of(this, factory).get(PackHistory::class.java)
        viewModel.table
    }

    private val packListViewModel: PackList by lazy {
        val factory = PackList.Factory((activity!! as MainActivity).sheetsLoader)
        ViewModelProviders.of(activity!!, factory).get(PackList::class.java)
    }

    private var packs: List<PackList.PackData>? = null
    private var packId: Int? = null

    override fun onActivityCreated(saved: Bundle?) {
        super.onActivityCreated(saved)

        configureSearchBox()

        packListViewModel.getPacks().observe(viewLifecycleOwner, Observer<List<PackList.PackData>> { packs ->
            this.packs = packs
        })

        (activity!!.findViewById(R.id.log_pack_submit) as Button).apply {
            setOnClickListener{
                submitPackLog()
            }
        }
    }

    private fun getProblemChecks() : List<CheckBox> {
        val checks = LinkedList<CheckBox>()
        val table = (activity!!.findViewById(R.id.check_problems) as TableLayout)

        for (tChild in table.children) {
            val row = tChild as TableRow
            for (rChild in row.children) {
                val check = rChild as CheckBox

                if (check.isChecked) {
                    checks.add(check)
                }
            }
        }

        return checks
    }

    private fun makeCheckNotes() : String {
        val problems = LinkedList<String>()

        for (check in getProblemChecks()) {
            problems.add("problem:${check.text}")
        }

        return problems.joinToString("; ")
    }

    private fun fillPackData() : PackHistory.PackData {
        val pack = PackHistory.PackData()
        val packId = getPackId()

        val notes = (activity!!.findViewById(R.id.enter_pack_notes) as EditText).text.toString().trim()

        pack.id = packId ?: 0
        pack.rating = (activity!!.findViewById(R.id.enter_pack_rating) as RatingBar).rating.toDouble()
        pack.notes = arrayOf(notes, makeCheckNotes()).joinToString("\n")
        pack.loggedBy = (activity!! as MainActivity).account?.displayName ?: "????"
        pack.date = SimpleDateFormat(DATE_FORMAT).format(Date())

        return pack
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_log_history, container, false)
    }

    private fun configureSearchBox() {
        val tv = activity!!.findViewById(R.id.log_pack_search) as AutoCompleteTextView
        val adapter = PackNameCompleter(context!!, R.layout.support_simple_spinner_dropdown_item,
                                        packListViewModel::getDataSynchronous)
        tv.setAdapter(adapter)
        tv.threshold = 1

        tv.setOnItemClickListener { parent, _, position, _ ->
            (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
                hideSoftInputFromWindow(tv.applicationWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }

        tv.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null)
                    updatePackId(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                packId = null
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })
    }

    private fun getPackId() : Int? {
        if (packId == null) {
            val tv = activity!!.findViewById(R.id.log_pack_search) as AutoCompleteTextView
            updatePackId(tv.text.toString())
        }

        return packId
    }

    private fun updatePackId(searchStr: String) {
        val packIdBox = activity!!.findViewById(R.id.log_pack_id) as TextView
        val packNameBox = activity!!.findViewById(R.id.log_pack_name) as TextView

        val pack = try {
            packs?.find{it.id == searchStr.toInt()}

        } catch (e: NumberFormatException) {
            packs?.find{searchStr.toLowerCase() == it.name.toLowerCase()}
        }

        if (pack != null) {
            packId = pack.id
            packIdBox.text = pack.id.toString()
            packNameBox.text = pack.name
        } else {
            packIdBox.text = ""
            packNameBox.text = ""

        }
    }

    private fun submitPackLog() {
        val pack = fillPackData()
        if (pack.id != 0 && pack.rating >= 1.0 && pack.rating <= 5.0) {
            packHistory.append(pack).executeInBackground()
            fragmentManager?.popBackStack()
        }
    }
}