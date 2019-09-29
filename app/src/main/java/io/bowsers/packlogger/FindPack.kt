package io.bowsers.packlogger


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
            adapter = PackNameCompleter(context!!, R.layout.support_simple_spinner_dropdown_item)

            val tv: AutoCompleteTextView = activity!!.findViewById(R.id.pack_search_str)
            tv.setAdapter(adapter)
            tv.threshold = 1
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_pack, container, false)
    }
}
