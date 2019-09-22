package io.bowsers.packlogger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_show_packs.*

class ShowPacksActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_packs)

        val editText: EditText = findViewById(R.id.editText)
        editText.setText(getPackSelectionType())
    }

    private fun getPackSelectionType(): String {
        return intent.getStringExtra("SELECT") ?: "top_packs"
    }
}
