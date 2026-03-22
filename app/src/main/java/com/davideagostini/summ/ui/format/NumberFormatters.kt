package com.davideagostini.summ.ui.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val usSymbols = DecimalFormatSymbols(Locale.US)
private val amountFormatter = DecimalFormat("#,##0.00", usSymbols)

fun formatAmount(value: Double): String = amountFormatter.format(value)

fun formatEuro(value: Double): String = "€${formatAmount(value)}"
