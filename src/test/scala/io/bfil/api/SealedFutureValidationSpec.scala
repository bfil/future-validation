package io.bfil.api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import io.bfil.api.SealedTestApi.SealedApiResult

import SealedTestApi.SealedApiResult
import scalaz._

object SealedTestApi extends TypedFutureValidationInstances[SealedApiError] {
  type SealedApiResult[T] = FutureValidation[SealedApiError, T]
  object SealedApiResult extends SealedFutureValidation[SealedApiError] {
    def exceptionHandler[T] = identityExceptionHandler
  }
  def zero = SealedApiError("An error occurred")
}
case class SealedApiError(message: String)

class SealedTestService {
  import SealedTestApi._
  def respondWith(result: String): SealedApiResult[String] = SealedApiResult.success(result)
  def failWith(error: SealedApiError): SealedApiResult[String] = SealedApiResult.failure(error)
}

class SealedFutureValidationSpec extends Specification with Mockito with AwaitableFutureValidation {

  import SealedTestApi._
  trait ApiScope extends Scope {
    val service = spy(new TestService)
    val error = SealedApiError("An error occurred")
  }

  "SealedFutureValidation" in {
    
    "constructor" should {
      
      "create a new future validation from a future sealing it with the exception handler" in new ApiScope {
        
        val future: Future[String] = Future.failed(new Exception("An exception occurred"))
        
        val actual: SealedApiResult[String] = SealedApiResult(future)
        
        actual.await.error must beEqualTo(error)
        
      }
      
      "create a future validation from a future that contains a validation sealing it with the exception handler" in new ApiScope {
        
        val future: Future[Validation[SealedApiError, String]] = Future.failed(new Exception("An exception occurred")).map(Validation.success)
        
        val actual: SealedApiResult[String] = SealedApiResult(future)
        
        actual.await.error must beEqualTo(error)
        
      }
    }
  }
}