package com.walde

import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.Column
import java.util.*


object BudgetTransactions : UUIDTable()  {
    val transaction_type : Column<BudgetTransactionType> = customEnumeration("transaction_type",
        "ENUM('EXPENSE', 'INCOME')", {value -> BudgetTransactionType.valueOf(value as String)}, {it.name})
    val description : Column<String> = varchar("description",10000)
    val amount : Column<Double> = double("amount")
}

data class BudgetTransaction (val id : UUID?,
                              val transaction_type: BudgetTransactionType,
                              val description : String,
                              val amount : Double)

enum class BudgetTransactionType { EXPENSE, INCOME }