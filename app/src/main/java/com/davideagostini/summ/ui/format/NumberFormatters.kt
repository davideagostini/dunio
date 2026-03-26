package com.davideagostini.summ.ui.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

private val usSymbols = DecimalFormatSymbols(Locale.US)
private val amountFormatter = DecimalFormat("#,##0.00", usSymbols)

const val DEFAULT_CURRENCY = "EUR"

fun formatAmount(value: Double): String = amountFormatter.format(value)

fun normalizeCurrencyCode(value: String): String =
    value.trim().uppercase(Locale.ROOT).ifBlank { DEFAULT_CURRENCY }

fun isSupportedCurrencyCode(value: String): Boolean =
    runCatching { Currency.getInstance(normalizeCurrencyCode(value)) }.isSuccess

fun currencySymbol(currency: String): String =
    runCatching {
        Currency.getInstance(normalizeCurrencyCode(currency)).getSymbol(Locale.getDefault())
    }.getOrElse { normalizeCurrencyCode(currency) }

fun formatCurrency(value: Double, currency: String = DEFAULT_CURRENCY): String =
    "${currencySymbol(currency)}${formatAmount(value)}"
