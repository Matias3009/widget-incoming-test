package com.testcountdown.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** User-facing options that change how the bars are drawn. */
data class WidgetSettings(
    /** true: the closer the test, the LONGER the bar. false: the closer the test, the SHORTER the bar. */
    val lessTimeIsBigger: Boolean = true,
    /** Time left that maps to the end of the scale (the bar width saturates beyond this). */
    val scaleWeeks: Int = 8,
)

/** Stores tests and settings in SharedPreferences (as JSON). */
class ExamRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("exams", Context.MODE_PRIVATE)

    fun getAll(): List<Exam> {
        val raw = prefs.getString(KEY_EXAMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Exam(
                    id = o.getLong("id"),
                    course = o.getString("course"),
                    number = o.getInt("number"),
                    timeMillis = o.getLong("time"),
                )
            }.sortedBy { it.timeMillis }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Tests that have not started yet, closest first. */
    fun getUpcoming(now: Long = System.currentTimeMillis()): List<Exam> =
        getAll().filter { it.timeMillis > now }

    fun save(exam: Exam) {
        val list = getAll().filter { it.id != exam.id } + exam
        write(list)
    }

    fun delete(id: Long) = write(getAll().filter { it.id != id })

    fun deleteFinished(now: Long = System.currentTimeMillis()) =
        write(getAll().filter { it.timeMillis > now })

    fun newId(): Long = System.currentTimeMillis()

    var settings: WidgetSettings
        get() = WidgetSettings(
            lessTimeIsBigger = prefs.getBoolean(KEY_LESS_IS_BIGGER, true),
            scaleWeeks = prefs.getInt(KEY_SCALE_WEEKS, 8),
        )
        set(value) {
            prefs.edit()
                .putBoolean(KEY_LESS_IS_BIGGER, value.lessTimeIsBigger)
                .putInt(KEY_SCALE_WEEKS, value.scaleWeeks)
                .apply()
        }

    private fun write(list: List<Exam>) {
        val array = JSONArray()
        list.sortedBy { it.timeMillis }.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("course", it.course)
                    .put("number", it.number)
                    .put("time", it.timeMillis)
            )
        }
        prefs.edit().putString(KEY_EXAMS, array.toString()).apply()
    }

    private companion object {
        const val KEY_EXAMS = "exams"
        const val KEY_LESS_IS_BIGGER = "less_time_is_bigger"
        const val KEY_SCALE_WEEKS = "scale_weeks"
    }
}
