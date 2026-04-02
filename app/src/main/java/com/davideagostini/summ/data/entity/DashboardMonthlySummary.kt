package com.davideagostini.summ.data.entity

data class DashboardMonthlySummary(
    val period: String = "",
    val incomeTotal: Double = 0.0,
    val expenseTotal: Double = 0.0,
    val cashFlow: Double = 0.0,
    val savingsRate: Double? = null,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netWorth: Double = 0.0,
    val liquidAssets: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val transactionCount: Int = 0,
    val activeAssetCount: Int = 0,
) {
    companion object {
        fun empty(period: String) = DashboardMonthlySummary(period = period)
    }
}
