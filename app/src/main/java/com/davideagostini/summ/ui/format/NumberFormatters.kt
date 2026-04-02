package com.davideagostini.summ.ui.format

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

private val usSymbols = DecimalFormatSymbols(Locale.US)
private val amountFormatter = DecimalFormat("#,##0.00", usSymbols)
private val amountInputSanitizer = Regex("[^0-9,.-]")

const val DEFAULT_CURRENCY = "EUR"

fun formatAmount(value: Double): String = amountFormatter.format(value)

fun formatEditableAmount(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

fun parseAmount(value: String): Double? {
    val sanitized = amountInputSanitizer.replace(value.trim(), "")
    if (sanitized.isBlank()) return null

    val lastComma = sanitized.lastIndexOf(',')
    val lastDot = sanitized.lastIndexOf('.')
    val decimalSeparator = when {
        lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
        lastComma >= 0 && looksLikeDecimalSeparator(sanitized, lastComma) -> ','
        lastDot >= 0 && looksLikeDecimalSeparator(sanitized, lastDot) -> '.'
        else -> null
    }

    val normalized = if (decimalSeparator == null) {
        sanitized.replace(",", "").replace(".", "")
    } else {
        val decimalIndex = sanitized.lastIndexOf(decimalSeparator)
        val integerPart = sanitized.substring(0, decimalIndex).replace(",", "").replace(".", "")
        val decimalPart = sanitized.substring(decimalIndex + 1).replace(",", "").replace(".", "")
        if (decimalPart.isBlank()) integerPart else "$integerPart.$decimalPart"
    }

    return normalized.toDoubleOrNull()
}

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

private fun looksLikeDecimalSeparator(value: String, separatorIndex: Int): Boolean {
    val digitsBefore = value.substring(0, separatorIndex).count { it.isDigit() }
    val digitsAfter = value.substring(separatorIndex + 1).count { it.isDigit() }
    return digitsBefore > 0 && digitsAfter in 1..2
}
