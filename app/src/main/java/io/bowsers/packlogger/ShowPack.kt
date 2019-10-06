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
 * Use the [ShowPack.newInstance] factory method to
 * create an instance of this fragment.
 */
class ShowPack : Fragment() {
    private var packId: Int? = null
    private var packName: String? = null

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
    }
}
