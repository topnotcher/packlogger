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

        var row = TableRow(context)
        var tv: TextView? = null
        row.layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)

        tv = addColumn(row, "ID")
        tv.gravity = Gravity.RIGHT
        tv.setTypeface(null, Typeface.BOLD)
        tv.setPadding(15, 15, 15, 15)

        tv = addColumn(row, "Name")
        tv.setTypeface(null, Typeface.BOLD)

        tv = addColumn(row, "Rating")
        tv.setTypeface(null, Typeface.BOLD)
        tv.gravity = Gravity.CENTER

        tv = addColumn(row, "Date")
        tv.gravity = Gravity.RIGHT
        tv.setTypeface(null, Typeface.BOLD)
        tv.setPadding(5,15,15,15)

        table.addView(row)
        addSeparator(table)

        packs.forEach {
            val row = TableRow(context)
            var textView: TextView? = null
            textView = addColumn(row, it.id.toString())
            textView.gravity = Gravity.RIGHT
            textView.setPadding(15, 15, 15, 15)

            addColumn(row, it.name)

            if (it.rating < 1) {
                textView = addColumn(row, "")
            } else {
                textView = addColumn(row, it.rating.toString())
            }
            textView.gravity = Gravity.CENTER

            textView = addColumn(row, it.date.toString())
            textView.gravity = Gravity.RIGHT
            textView.setPadding(5,15,15,15)

            row.layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
            table.addView(row)
            addSeparator(table)
        }
    }

    private fun addColumn(row: TableRow, text: String) : TextView {
        val textView = TextView(context)
        textView.minWidth = 4
        textView.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)

        textView.text = text
        row.addView(textView)

        return textView
    }

    private fun addSeparator(table: TableLayout) {
        val row = TableRow(context)
        row.layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        row.minimumHeight = 1
        row.setBackgroundColor(Color.parseColor("#d9d9d9"))
        table.addView(row)
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
