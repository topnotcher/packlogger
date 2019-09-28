package io.bowsers.packlogger

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
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
        val credential = GoogleAccountCredential.usingOAuth2(activity!!.applicationContext, scopes)
        val aacount = account!!.account
        credential!!.selectedAccount = aacount

        viewModel.setCredential(credential)
        viewModel.setSelection(selection)

        viewModel.getPacks().observe(this, Observer<List<ShowPacksViewModel.PackData>> { packs ->
            updatePacks(packs)
        })
    }

    private fun updatePacks(packs: List<ShowPacksViewModel.PackData>) {
        val table: TableLayout = activity!!.findViewById(R.id.packs_table)
        table.removeAllViewsInLayout()
        packs.forEach {
            val row = TableRow(context)
            addColumn(row, it.id.toString())
            addColumn(row, it.name)
            addColumn(row, it.rating.toString())
            addColumn(row, it.date.toString())

            row.layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
            table.addView(row)
        }
    }

    private fun addColumn(row: TableRow, text: String) {
        val textView = TextView(context)
        textView.minWidth = 4
        textView.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)

        textView.text = text
        row.addView(textView)
    }

    private var selection: String? = null
    private var account: GoogleSignInAccount? = null

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
