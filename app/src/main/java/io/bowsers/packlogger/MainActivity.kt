package io.bowsers.packlogger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.SheetsScopes

class MainActivity : FragmentActivity(), MainFragment.OnFragmentInteractionListener {

    companion object {
        private const val KEY_ACCOUNT = "ACCOUNT"
        private const val REQUEST_SIGN_IN = 1
        private val main = MainFragment()
    }
    var account: GoogleSignInAccount? = null
    val sheetsLoader by lazy {
        createSheetsLoader()
    }

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContentView(R.layout.activity_main)

        if (saved != null && saved.containsKey(KEY_ACCOUNT)) {
            account = saved.getParcelable(KEY_ACCOUNT)
        }

        if (account == null) {
            requestSignIn()
        }
    }

    override fun onSaveInstanceState(saved: Bundle) {
        super.onSaveInstanceState(saved)
        if (account != null) {
            saved.putParcelable(KEY_ACCOUNT, account!!)
        }
    }

    fun showPacks(view: View) {
        val selection: String = view.tag as String? ?: ShowPacksViewModel.SELECT_TOP_PACKS
        val showPacks = ShowPacksFragment.newInstance(selection)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.content_area, showPacks)
        ft.addToBackStack(null)
        ft.commit()
    }

    fun findPack(view: View) {
    }

    override fun onMainFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(applicationContext, signInOptions)

        startActivityForResult(client.signInIntent, REQUEST_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SIGN_IN && intent != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            account = task.getResult(ApiException::class.java)

            supportFragmentManager.beginTransaction()
            .add(R.id.main_container, main)
            .commit()

        } catch (e: ApiException) {
            Log.w("PackLogger", "signInResult: failed code = " + e.statusCode)
        }

    }

    private fun createSheetsLoader() : SheetsCollectionLoader {
        val lAccount = account
        if (lAccount != null) {
            val scopes = listOf(SheetsScopes.SPREADSHEETS)
            val credential =
                GoogleAccountCredential.usingOAuth2(applicationContext, scopes)
            credential!!.selectedAccount = lAccount.account

            return SheetsCollectionLoader(credential).apply{
                setCacheDir(applicationContext.cacheDir)
            }

        } else {
            throw IllegalStateException()
        }
    }
}
