package com.testcountdown.widget

/** One upcoming test / interrogation. */
data class Exam(
    val id: Long,
    val course: String,
    val number: Int,
    /** Start of the test, epoch milliseconds. */
    val timeMillis: Long,
) {
    /** "MAT1203 (1)" */
    val label: String get() = "$course ($number)"

    fun timeLeft(now: Long): Long = timeMillis - now
}
