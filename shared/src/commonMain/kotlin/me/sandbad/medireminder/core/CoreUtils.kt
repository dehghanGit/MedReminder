package me.sandbad.medireminder.core

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

fun currentDateTime(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

fun today(): LocalDate = currentDateTime().date

fun LocalDateTime.epochMillis(): Long =
    toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

fun LocalDateTime.plusMinutes(minutes: Int): LocalDateTime =
    toInstant(TimeZone.currentSystemDefault())
        .plus(minutes.minutes)
        .toLocalDateTime(TimeZone.currentSystemDefault())

fun LocalDate.at(time: LocalTime): LocalDateTime = atTime(time)

/** "08:05" — the storage form and the form shown next to a dose. */
fun LocalTime.formatHm(): String {
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    return "$h:$m"
}

fun LocalDateTime.formatHm(): String = time.formatHm()

fun parseHm(value: String): LocalTime? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return LocalTime(h, m)
}

fun List<LocalTime>.encodeTimes(): String = joinToString(",") { it.formatHm() }

fun String.decodeTimes(): List<LocalTime> =
    split(",").mapNotNull { parseHm(it) }.sorted()

fun Set<Int>.encodeDays(): String? = if (isEmpty()) null else sorted().joinToString(",")

fun String?.decodeDays(): Set<Int> =
    this?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.filter { it in 1..7 }?.toSet().orEmpty()

/** Same wire format as [encodeDays], but for calendar days-of-month (1..31). */
fun String?.decodeDaysOfMonth(): Set<Int> =
    this?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.filter { it in 1..31 }?.toSet().orEmpty()
