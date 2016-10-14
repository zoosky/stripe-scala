package org.mdedetrich.stripe.v1

import java.time.{LocalDate, OffsetDateTime}

import org.mdedetrich.stripe.Config._
import org.mdedetrich.stripe.v1.Accounts.LegalEntityType.Individual
import org.mdedetrich.stripe.v1.Accounts.{Account, LegalEntity, TosAcceptance}
import org.mdedetrich.stripe.v1.BankAccounts.BankAccountData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountIT extends IntegrationTest {

  val testCard = "4242424242424242"

  "Account" should {
    "create a managed account into which money can be paid" in {

      val accountF = AccountIT.createManagedAccountWithBankAccount

      whenReady(accountF) { account =>
        account shouldBe a[Accounts.Account]
        account.metadata should be(AccountIT.meta)
        account.transfersEnabled should be(true)
      }
    }
  }

}

object AccountIT {

  val meta = Map("foo" -> "bar")

  def createManagedAccountWithBankAccount: Future[Account] = {
    val dob           = LocalDate.now().minusYears(30)
    val tosAcceptance = Some(TosAcceptance(Some(OffsetDateTime.now()), Some("62.96.204.171")))
    val legalEntity   = Some(LegalEntity.default.copy(`type` = Some(Individual),
      firstName = Some("Horst"),
      lastName = Some("Kasuppke"),
      dob = Some(dob)))

    // weirdly, this needs to be here in order for the following line not to throw a NullPointerException
    Currency.lowerCaseNamesToValuesMap
    val ba = Some(BankAccountData.Source.Object.default("DE89370400440532013000", "DE", Currency.`Euro`))

    val accountInput = Accounts.AccountInput.default.copy(managed = true, metadata = meta)
    val accountUpdate = Accounts.AccountUpdate.default.copy(
      tosAcceptance = tosAcceptance,
      legalEntity = legalEntity,
      externalAccount = ba
    )

    for {
      account <- handleIdempotent(Accounts.create(accountInput))
      updatedAccount <- handleIdempotent(Accounts.update(account.id, accountUpdate))
    } yield updatedAccount

  }
}
