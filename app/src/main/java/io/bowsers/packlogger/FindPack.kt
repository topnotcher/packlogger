package io.bowsers.packlogger


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.SheetsScopes
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [FindPack.newInstance] factory method to
 * create an instance of this fragment.
 */
class FindPack : Fragment() {
    private var adapter: PackNameCompleter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (adapter == null) {
            val scopes = listOf(SheetsScopes.SPREADSHEETS)
            val credential =
                GoogleAccountCredential.usingOAuth2(activity!!.applicationContext, scopes)
            val aacount = (activity!! as MainActivity).account?.account

            credential!!.selectedAccount = aacount
            adapter = PackNameCompleter(context!!, R.layout.support_simple_spinner_dropdown_item, credential)

            val tv: AutoCompleteTextView = activity!!.findViewById(R.id.pack_search_str)
            tv.setAdapter(adapter)
            tv.threshold = 1
        }

        (activity!!.findViewById(R.id.pack_search_str) as View).apply {
            val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }

        //(activity!!.findViewById(R.id.pack_search_str) as View)?.requestFocus()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_pack, container, false)
    }
}
