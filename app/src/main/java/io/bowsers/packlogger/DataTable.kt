package io.bowsers.packlogger

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.forEach
import java.util.*

class DataTable constructor (private val table: TableLayout, private val context: Context?) {
    data class Padding(val left: Int, val right: Int, val top: Int = 15, val bottom: Int = 15)

    var rows = 0
    private val padding: LinkedList<Padding> = LinkedList()
    private val gravity: LinkedList<Int> = LinkedList()
    private var fontSize: Float = 10.0f

    fun clear() {
        rows = 0
        table.removeAllViewsInLayout()
    }

    fun setPadding(left: Int, right: Int, top: Int, bottom: Int): DataTable {
        padding.add(Padding(left, right, top, bottom))
        return this
    }

    fun setGravity(g: Int) : DataTable {
        gravity.add(g)
        return this
    }

    fun setFontSize(size: Float) : DataTable {
        fontSize = size
        return this
    }

    private fun addColumn(row: TableRow, text: String) : TextView {
        val textView = TextView(context)
        textView.minWidth = 4
        textView.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)

        textView.text = text
        row.addView(textView)

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        return textView
    }

    fun addRow(vararg values: String) : TableRow {
        val row = TableRow(context)
        var idx = 0
        val p: Iterator<Padding> = padding.iterator()
        val g: Iterator<Int> = gravity.iterator()

        values.forEach {
            val tv = addColumn(row, it)

            if (g.hasNext()) {
                tv.gravity = g.next()
            }

            if (p.hasNext()) {
                val pad = p.next()
                tv.setPadding(pad.left, pad.top, pad.right, pad.bottom)
            }

            idx += 1
        }

        row.layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        table.addView(row)
        addSeparator()

        if (rows == 0) {
            styleHeaderRow(row)
        }

        if (rows % 2 == 1) {
            row.setBackgroundColor(Color.parseColor("#eeeeee"))
        }

        rows += 1
        return row
    }

    private fun styleHeaderRow(row: TableRow) {
        row.forEach {
            (it as TextView).setTypeface(null, Typeface.BOLD)
        }
    }

    fun addSeparator() {
        val row = TableRow(context)
        row.layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        row.minimumHeight = 1
        row.setBackgroundColor(Color.parseColor("#b8b8b8"))

        table.addView(row)
    }
}
