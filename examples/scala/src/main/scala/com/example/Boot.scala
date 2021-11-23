// Copyright © 2017-2021 UKG Inc. <https://www.ukg.com>

package com.example

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import com.example.account.BankAccount
import com.example.http.request.{
  CreateAccountRequest,
  CreditAccountRequest,
  DebitAccountRequest
}
import com.example.http.serializer.BankAccountRequestSerializer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.slf4j.{LoggerFactory, MDC}
import surge.scaladsl.common.{CommandFailure, CommandResult, CommandSuccess}
import com.example.http.request.RequestToCommand._
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import java.util.UUID
import scala.util.{Failure, Success}

object Boot extends App with PlayJsonSupport with BankAccountRequestSerializer {

  implicit val system = BankAccountEngine.surgeEngine.actorSystem
  private val log = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()

  val route =
    pathPrefix("bank-accounts") {
      concat(
        path("create") {
          post {
            entity(as[CreateAccountRequest]) { request =>
              val createAccountCommand = requestToCommand(request)
              MDC.put(
                "account_number",
                createAccountCommand.accountNumber.toString
              )
              val createdAccountF: Future[CommandResult[BankAccount]] =
                BankAccountEngine.surgeEngine
                  .aggregateFor(createAccountCommand.accountNumber)
                  .sendCommand(createAccountCommand)

              onComplete(createdAccountF) {
                case Success(commandResult) =>
                  commandResult match {
                    case CommandSuccess(aggregateState) =>
                      complete(aggregateState)
                    case CommandFailure(reason) =>
                      complete(
                        StatusCodes.BadRequest,
                        Map("message" -> reason.getMessage)
                      )
                  }
                case Failure(_) => complete(StatusCodes.InternalServerError)
              }
            }
          }
        },
        path("credit" / JavaUUID) { accountId =>
          put {
            entity(as[CreditAccountRequest]) { request =>
              val creditAccountCommand = requestToCommand(accountId, request)
              MDC.put(
                "account_number",
                creditAccountCommand.accountNumber.toString
              )
              val creditAccountF: Future[CommandResult[BankAccount]] =
                BankAccountEngine.surgeEngine
                  .aggregateFor(creditAccountCommand.accountNumber)
                  .sendCommand(creditAccountCommand)

              onComplete(creditAccountF) {
                case Success(commandResult) =>
                  commandResult match {
                    case CommandSuccess(aggregateState) =>
                      complete(aggregateState)
                    case CommandFailure(reason) =>
                      complete(
                        StatusCodes.BadRequest,
                        Map("message" -> reason.getMessage)
                      )
                  }
                case Failure(_) => complete(StatusCodes.InternalServerError)
              }
            }
          }
        },
        path("debit" / JavaUUID) { accountId: UUID =>
          put {
            entity(as[DebitAccountRequest]) { request =>
              val debitAccountCommand = requestToCommand(accountId, request)
              MDC.put(
                "account_number",
                debitAccountCommand.accountNumber.toString
              )
              val debitAccountF: Future[CommandResult[BankAccount]] =
                BankAccountEngine.surgeEngine
                  .aggregateFor(accountId)
                  .sendCommand(debitAccountCommand)

              onComplete(debitAccountF) {
                case Success(commandResult) =>
                  commandResult match {
                    case CommandSuccess(aggregateState) =>
                      complete(aggregateState)
                    case CommandFailure(reason) =>
                      complete(
                        StatusCodes.BadRequest,
                        Map("message" -> reason.getMessage)
                      )
                  }
                case Failure(_) => complete(StatusCodes.InternalServerError)
              }
            }
          }
        },
        path(JavaUUID) { uuid =>
          get {
            MDC.put("account_number", uuid.toString)
            val accountStateF =
              BankAccountEngine.surgeEngine.aggregateFor(uuid).getState
            log.info("Get account owner's state ")
            onSuccess(accountStateF) {
              case Some(accountState) => complete(accountState)
              case None               => complete(StatusCodes.NotFound)
            }
          }
        }
      )
    }

  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  val bindingFuture = Http().newServerAt(host, port).bind(route)

  log.info(s"Server is running on  http://$host:$port")
}
