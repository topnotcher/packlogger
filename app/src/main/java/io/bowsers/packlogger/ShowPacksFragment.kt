package io.bowsers.packlogger

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.google.android.gms.auth.api.signin.GoogleSignInAccount


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

        if (activity != null) {
            val editText: EditText = activity!!.findViewById(R.id.editText)
            editText.setText(selection)
        }
        viewModel.setAccount(account)
        viewModel.setSelection(selection)
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
        fun newInstance(selection: String, account: GoogleSignInAccount?) =
            ShowPacksFragment().apply {
                arguments = Bundle().apply {
                    putString(SELECTION, selection)
                    putParcelable(ACCOUNT, account)
                }
            }
    }
}
