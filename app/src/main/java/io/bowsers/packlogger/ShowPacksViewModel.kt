package io.bowsers.packlogger

import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class ShowPacksViewModel : ViewModel() {
    private var account: GoogleSignInAccount? = null
    private var selection: String? = null

    companion object {
        const val SELECT_TOP_PACKS: String = "top_packs"
        const val SELECT_ALL_PACKS: String = "all_packs"
    }

    fun setAccount(account : GoogleSignInAccount?) {
        this.account = account
    }

    fun setSelection(selection: String?) {
        this.selection = selection
    }

}
