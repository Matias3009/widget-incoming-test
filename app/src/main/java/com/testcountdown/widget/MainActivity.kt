package com.testcountdown.widget

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {

    private lateinit var repo: ExamRepository
    private lateinit var list: ListView
    private lateinit var emptyText: TextView
    private val adapter = ExamAdapter()

    private val scaleOptions = intArrayOf(2, 4, 8, 12, 16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = ExamRepository(this)
        applySystemBarInsets(findViewById(R.id.root))

        list = findViewById(R.id.exam_list)
        emptyText = findViewById(R.id.empty_text)
        list.adapter = adapter
        // Bars are drawn to the list's width, so redraw once it is known / changes.
        list.addOnLayoutChangeListener { _, l, _, r, _, ol, _, or, _ ->
            if (r - l != or - ol) list.post { adapter.notifyDataSetChanged() }
        }
        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            showEditor(adapter.getItem(pos))
        }
        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
            confirmDelete(adapter.getItem(pos)); true
        }

        findViewById<Button>(R.id.add_button).setOnClickListener { showEditor(null) }
        findViewById<Button>(R.id.clear_button).setOnClickListener {
            repo.deleteFinished()
            refresh()
        }

        // "Less time left = bigger bar" vs "less time left = smaller bar"
        val sizeSwitch = findViewById<Switch>(R.id.less_time_bigger)
        sizeSwitch.isChecked = repo.settings.lessTimeIsBigger
        sizeSwitch.setOnCheckedChangeListener { _, checked ->
            repo.settings = repo.settings.copy(lessTimeIsBigger = checked)
            refresh()
        }

        // Range of the width scale
        val scaleSpinner = findViewById<Spinner>(R.id.scale_spinner)
        scaleSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            scaleOptions.map { getString(R.string.scale_weeks, it) },
        )
        scaleSpinner.setSelection(scaleOptions.indexOf(repo.settings.scaleWeeks).coerceAtLeast(0))
        scaleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (repo.settings.scaleWeeks != scaleOptions[pos]) {
                    repo.settings = repo.settings.copy(scaleWeeks = scaleOptions[pos])
                    refresh()
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.exams = repo.getAll()
        adapter.notifyDataSetChanged()
        emptyText.visibility = if (adapter.count == 0) View.VISIBLE else View.GONE
        ExamWidgetProvider.updateAll(this)
    }

    private fun showEditor(existing: Exam?) {
        val view = layoutInflater.inflate(R.layout.dialog_exam, null)
        val courseInput = view.findViewById<EditText>(R.id.course_input)
        val numberInput = view.findViewById<EditText>(R.id.number_input)
        val dateButton = view.findViewById<Button>(R.id.date_button)
        val timeButton = view.findViewById<Button>(R.id.time_button)

        val cal = Calendar.getInstance().apply {
            if (existing != null) {
                timeInMillis = existing.timeMillis
            } else {
                add(Calendar.DAY_OF_YEAR, 7)
                set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0)
            }
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        existing?.let {
            courseInput.setText(it.course)
            numberInput.setText(it.number.toString())
        }

        fun updateButtons() {
            dateButton.text = DateFormat.getMediumDateFormat(this).format(cal.time)
            timeButton.text = DateFormat.getTimeFormat(this).format(cal.time)
        }
        updateButtons()

        dateButton.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d); updateButtons()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        timeButton.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); updateButtons()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), DateFormat.is24HourFormat(this)).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) R.string.add_test else R.string.edit_test)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (existing != null) setNeutralButton(R.string.delete) { _, _ ->
                    repo.delete(existing.id); refresh()
                }
            }
            .create()
        dialog.setOnShowListener {
            // Validate before closing the dialog.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val course = courseInput.text.toString().trim().uppercase()
                if (course.isEmpty()) {
                    courseInput.error = getString(R.string.error_course)
                    return@setOnClickListener
                }
                val number = numberInput.text.toString().toIntOrNull() ?: 1
                repo.save(Exam(existing?.id ?: repo.newId(), course, number, cal.timeInMillis))
                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, R.string.warning_past, Toast.LENGTH_LONG).show()
                }
                refresh()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmDelete(exam: Exam) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_delete, exam.label))
            .setPositiveButton(R.string.delete) { _, _ -> repo.delete(exam.id); refresh() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** targetSdk 35 draws edge-to-edge: keep content clear of the status/navigation bars. */
    private fun applySystemBarInsets(root: View) {
        val base = intArrayOf(root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom)
        root.setOnApplyWindowInsetsListener { v, insets ->
            val (l, t, r, b) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                listOf(i.left, i.top, i.right, i.bottom)
            } else {
                @Suppress("DEPRECATION")
                listOf(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            v.setPadding(base[0] + l, base[1] + t, base[2] + r, base[3] + b)
            insets
        }
    }

    /** Shows the same bars as the widget (a live preview), plus the date of each test. */
    private inner class ExamAdapter : BaseAdapter() {
        var exams: List<Exam> = emptyList()

        override fun getCount() = exams.size
        override fun getItem(position: Int) = exams[position]
        override fun getItemId(position: Int) = exams[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_exam, parent, false)
            val exam = exams[position]
            val now = System.currentTimeMillis()
            val density = resources.displayMetrics.density
            val width = parent.width - parent.paddingLeft - parent.paddingRight -
                view.paddingLeft - view.paddingRight
            val rowWidth = if (width > 0) width else resources.displayMetrics.widthPixels - (64 * density).toInt()
            view.findViewById<ImageView>(R.id.pill)
                .setImageBitmap(
                    PillRenderer.render(
                        exam, now, repo.settings, rowWidth, density,
                        outsideTextColor = getColor(R.color.widget_text),
                    )
                )
            val date = DateFormat.getMediumDateFormat(this@MainActivity).format(exam.timeMillis) + "  " +
                DateFormat.getTimeFormat(this@MainActivity).format(exam.timeMillis)
            view.findViewById<TextView>(R.id.date).text =
                if (exam.timeMillis <= now) getString(R.string.finished_on, date) else date
            return view
        }
    }
}
