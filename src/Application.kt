package com.walde

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.GsonConverter
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json,GsonConverter())
        gson {
            setPrettyPrinting()
        }
    }
    install(CallLogging)
    install(DefaultHeaders)

    // you need to change the parameters here, when you don't use docker-compose
    Database.connect("jdbc:mysql://db:3306/${System.getenv("MYSQL_DATABASE")}",
        driver = "com.mysql.cj.jdbc.Driver",
        user = System.getenv("MYSQL_USER"),
        password = System.getenv("MYSQL_PASSWORD"))

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(BudgetTransactions)
    }

    routing {

        static("/") {
            resources("static")
        }

        route("transactions-api"){

            get{

                val BudgetTransactionsList: List<BudgetTransaction> = transaction {
                    BudgetTransactions.selectAll().map {
                        BudgetTransaction(it[BudgetTransactions.id].value,
                            it[BudgetTransactions.transaction_type] ,
                            it[BudgetTransactions.description],
                            it[BudgetTransactions.amount])
                    }.toList()
                }

                call.respond(HttpStatusCode.OK,BudgetTransactionsList)
            }

            post{

                val incomingTransaction: BudgetTransaction = call.receive()

                val newTransactionId: EntityID<UUID>? = transaction {
                    BudgetTransactions.insert {
                        it[transaction_type] = incomingTransaction.transaction_type
                        it[description] = incomingTransaction.description
                        it[amount] = incomingTransaction.amount
                    } get BudgetTransactions.id
                }

                if (newTransactionId !=null)
                    call.respondText(newTransactionId.toString(),ContentType.Text.Plain,HttpStatusCode.OK)
                else
                    call.respond(HttpStatusCode.InternalServerError)
            }


            delete{
                val transactionId : String = call.receiveText()

                val deleteResult: Pair<Int,Int> = transaction {

                    val countBefore : Int = BudgetTransactions.select {
                        BudgetTransactions.id eq UUID.fromString(transactionId)
                    }.count()

                    BudgetTransactions.deleteWhere {
                        BudgetTransactions.id eq UUID.fromString(transactionId)
                    }

                    val countAfter : Int = BudgetTransactions.select {
                        BudgetTransactions.id eq UUID.fromString(transactionId)
                    }.count()

                    return@transaction Pair(countBefore,countAfter)

                }

                when{
                    deleteResult.first == 1 && deleteResult.second ==0 ->
                        call.respond(HttpStatusCode.OK,"deleted transaction: $transactionId")
                    deleteResult.first == 0 ->
                        call.respond(HttpStatusCode.NotFound,"transaction does not exist: $transactionId")
                    deleteResult.second == 1 && deleteResult.first == 1 ->
                        call.respond(HttpStatusCode.BadRequest,"cannot delete transaction: $transactionId")
                }
            }
        }
    }
}

