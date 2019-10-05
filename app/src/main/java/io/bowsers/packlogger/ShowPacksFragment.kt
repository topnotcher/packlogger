package io.bowsers.packlogger

import android.graphics.Color
import android.graphics.Typeface
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.forEach
import androidx.lifecycle.Observer
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.SheetsScopes


class ShowPacksFragment : Fragment() {

    private lateinit var viewModel: ShowPacksViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selection = it.getString(SELECTION)
            account = it.getParcelable(ACCOUNT)
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
        viewModel = ViewModelProviders.of(this).get(ShowPacksViewModel::class.java)

        val scopes = listOf(SheetsScopes.SPREADSHEETS)
        val credential =
            GoogleAccountCredential.usingOAuth2(activity!!.applicationContext, scopes)
        val aacount = account!!.account
        credential!!.selectedAccount = aacount

        viewModel.setCredential(credential)
        viewModel.setSelection(selection)
        if (context != null)
            viewModel.setCacheDirectory(context!!.cacheDir)

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

            t.addRow(it.id.toString(), it.name, rating, it.date)
        }
    }

    private var selection: String? = null
    private var account: GoogleSignInAccount? = null
    private var table: DataTable? = null

    companion object {
        private const val SELECTION = "SELECT"
        private const val ACCOUNT = "ACCOUNT"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param selection Parameter 1.
         * @return A new instance of fragment MainFragment.
         */
        @JvmStatic
        fun newInstance(selection: String, account: GoogleSignInAccount??) =
            ShowPacksFragment().apply {
                arguments = Bundle().apply {
                    putString(SELECTION, selection)
                    putParcelable(ACCOUNT, account)
                }
            }
    }
}
