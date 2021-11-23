package simulations

import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class CreditAccountSimulation extends Simulation {

  private val log = LoggerFactory.getLogger(getClass)

  val scalaAppUrl: String = System.getenv("SCALA_APP_URL")

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(scalaAppUrl)
    .header("content-Type", "application/json")

  private def createRequestBody(
      initialBalance: Int,
      accountNumber: UUID
  ): String = {
    s"""{
       |    "accountNumber": "$accountNumber",
       |    "accountOwner": "Jane Doe",
       |     "securityCode": "12345678910",
       |     "initialBalance": $initialBalance
       |}""".stripMargin
  }

  private def creditRequestBody(initialBalance: Int): String = {
    s"""{
       |     "amount": $initialBalance
       |}""".stripMargin
  }

  private def debitRequestBody(initialBalance: Int): String = {
    s"""{
       |     "amount": $initialBalance
       |}""".stripMargin
  }

  val creditChain: ChainBuilder = exec(
    http("Credit Account Request")
      .put("/bank-accounts/credit/${aggregateId}")
      .body(StringBody(creditRequestBody(100)))
      .check(status.is(200), jsonPath("$.balance").saveAs("balance"))
  )
    .exec(session => {
      log.info("Account Balance after credit:")
      log.info(session("balance").as[String])
      session
    })

  val debitChain: ChainBuilder = exec(
    http("Debit Account Request")
      .put("/bank-accounts/debit/${aggregateId}")
      .body(StringBody(debitRequestBody(100)))
      .check(status.is(200), bodyString.saveAs("balance"))
  )
    .exec(session => {
      log.info("Account Balance after debit:")
      log.info(session("balance").as[String])
      session
    })

  val scn: ScenarioBuilder = scenario("create user account")
    .exec(
      http("create account request")
        .post("/bank-accounts/create")
        .body(StringBody(createRequestBody(10000, UUID.randomUUID())))
        .check(
          status is (200),
          jsonPath("$.accountNumber")
            .saveAs("aggregateId")
        )
    )
    .exec(session => {
      log.info("User account response")
      log.info(session("aggregateId").as[String])
      session
    })
    .pause(500 milliseconds)
    .repeat(Random.between(1, 10))(creditChain)
    .pause(1 seconds)
    .repeat(Random.between(1, 10))(debitChain)

  setUp(
    scn
      .inject(atOnceUsers(1))
      .protocols(httpProtocol)
  )

}
