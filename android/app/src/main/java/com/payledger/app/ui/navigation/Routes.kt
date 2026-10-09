package com.payledger.app.ui.navigation

object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RESET = "reset"
    const val ACTIVATE = "activate"
    const val RENEW = "renew"
    const val MAIN = "main"
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_DETAIL = "transaction/{id}"
    const val ADD_TRANSACTION = "add_transaction"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    fun transactionDetail(id: String) = "transaction/$id"
}
