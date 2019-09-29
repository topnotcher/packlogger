package io.bowsers.packlogger

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
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
import io.bowsers.packlogger.ShowPacksFragment.Companion.newInstance
import io.bowsers.packlogger.ShowPacksViewModel.Companion.SELECT_TOP_PACKS

class MainActivity : FragmentActivity(), MainFragment.OnFragmentInteractionListener {

    companion object {
        private const val REQUEST_SIGN_IN = 1
        private val main = MainFragment()
        private var account: GoogleSignInAccount? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            requestSignIn()
        }
    }

    fun showPacks(view: View) {
//        val intent = Intent(this, ShowPacksActivity::class.java).apply {
//            putExtra("SELECT", view.tag as String? ?: "top_packs")
//        }

        //setContentView(R.layout.activity_show_packs)
//        startActivity(intent)

        val selection: String = view.tag as String? ?: ShowPacksViewModel.SELECT_TOP_PACKS
        val showPacks = ShowPacksFragment.newInstance(selection, account)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.main_container, showPacks)
        ft.addToBackStack(null)
        ft.commit()
    }

    fun findPack(view: View) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_container, FindPack())
            .addToBackStack(null)
            .commit()
    }

    override fun onMainFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun requestSignIn() {
        // .requestEmail()
        // .requestScopes(Scope(SheetsScopes.SPREADSHEETS_READONLY))

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
 //           val scopes = listOf(SheetsScopes.SPREADSHEETS)
  //          credential = GoogleAccountCredential.usingOAuth2(applicationContext, scopes)
   //         credential!!.selectedAccount = account!!.account

            supportFragmentManager.beginTransaction()
            .add(R.id.main_container, main)
            .commit()
            //ft.replace(R.id.main_container, MainFragment())

        } catch (e: ApiException) {
            Log.w("PackLogger", "signInResult: failed code = " + e.statusCode)
        }

    }
}
