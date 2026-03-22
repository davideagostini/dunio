package com.davideagostini.summ.data.firebase

object FirestorePaths {
    fun user(userId: String) = "users/$userId"
    fun household(householdId: String) = "households/$householdId"
    fun member(householdId: String, userId: String) = "households/$householdId/members/$userId"
    fun members(householdId: String) = "households/$householdId/members"
    fun transactions(householdId: String) = "households/$householdId/transactions"
    fun transaction(householdId: String, transactionId: String) =
        "households/$householdId/transactions/$transactionId"
    fun recurringTransactions(householdId: String) = "households/$householdId/recurringTransactions"
    fun recurringTransaction(householdId: String, recurringTransactionId: String) =
        "households/$householdId/recurringTransactions/$recurringTransactionId"
    fun monthCloses(householdId: String) = "households/$householdId/monthCloses"
    fun monthClose(householdId: String, period: String) =
        "households/$householdId/monthCloses/$period"
    fun categories(householdId: String) = "households/$householdId/categories"
    fun category(householdId: String, categoryId: String) =
        "households/$householdId/categories/$categoryId"
    fun invites(householdId: String) = "households/$householdId/invites"
    fun invite(householdId: String, inviteId: String) =
        "households/$householdId/invites/$inviteId"
    fun assets(householdId: String) = "households/$householdId/assets"
    fun asset(householdId: String, assetId: String) =
        "households/$householdId/assets/$assetId"
    fun assetHistory(householdId: String, assetId: String) =
        "households/$householdId/assets/$assetId/history"
    fun assetHistoryEntry(householdId: String, assetId: String, entryId: String) =
        "households/$householdId/assets/$assetId/history/$entryId"
}
