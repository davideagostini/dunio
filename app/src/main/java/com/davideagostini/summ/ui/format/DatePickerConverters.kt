package com.davideagostini.summ.ui.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

fun datePickerMillisToLocalStartOfDayMillis(selectedDateMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(selectedDateMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()

    return localDate.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

fun localStartOfDayMillisToDatePickerMillis(epochMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return localDate.atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
}

fun currentLocalStartOfDayMillis(): Long =
    LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
