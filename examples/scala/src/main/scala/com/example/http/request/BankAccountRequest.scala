// Copyright © 2017-2021 UKG Inc. <https://www.ukg.com>

package com.example.http.request

import com.example.command.{CreateAccount, CreditAccount, DebitAccount}

import java.util.UUID

case class CreateAccountRequest(
    accountOwner: String,
    securityCode: String,
    initialBalance: Double
)

case class CreditAccountRequest(amount: Double)

case class DebitAccountRequest(amount: Double)

object RequestToCommand {
  def requestToCommand(request: CreateAccountRequest): CreateAccount = {
    val newAccountNumber = UUID.randomUUID()
    CreateAccount(
      newAccountNumber,
      request.accountOwner,
      request.securityCode,
      request.initialBalance
    )
  }

  def requestToCommand(
      accountNumber: UUID,
      request: CreditAccountRequest
  ): CreditAccount = {
    CreditAccount(accountNumber, request.amount)
  }

  def requestToCommand(
      accountNumber: UUID,
      request: DebitAccountRequest
  ): DebitAccount = {
    DebitAccount(accountNumber, request.amount)
  }
}
