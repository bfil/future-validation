package com.bfil.api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import com.bfil.api.TestApi.ApiResult

import scalaz._

class FutureValidationConversionsSpec extends Specification with AwaitableFutureValidation {

  trait ApiScope extends Scope with FutureValidationConversions {
    val service = new TestService
    val error = ApiError("An error occurred")
  }

  "FutureValidationConversions" in {

    "implicit conversions" should {

      "implicitly convert a value to a future validation successful value" in new ApiScope {

        val actual: ApiResult[String] = "hello"

        actual.await.result must beEqualTo("hello")

      }

      "implicitly convert a value to a future validation error value" in new ApiScope {

        val actual: ApiResult[String] = error

        actual.await.error must beEqualTo(error)

      }

      "implicitly convert a future to a sealed future validation" in new ApiScope {

        implicit def exceptionHandler[T]: ExceptionHandler[ApiError, T] = {
          case ex => Validation.failure(error)
        }

        val successfulResult: ApiResult[String] = Future("hello")
        val failureResult: ApiResult[String] = Future {
          throw new Exception("An exception occurred")
        }

        successfulResult.await.result must beEqualTo("hello")
        failureResult.await.error must beEqualTo(error)

      }
    }
  }

  "RichFutureValidation" in {

    object AnotherTestApi {
      type AnotherApiResult[T] = FutureValidation[AnotherApiError, T]
      object AnotherApiResult extends TypedFutureValidation[AnotherApiError]
    }
    case class AnotherApiError(message: String)

    import AnotherTestApi._
    class AnotherTestService {
      def respondWith(result: String): AnotherApiResult[String] = AnotherApiResult.success(result)
      def failWith(error: AnotherApiError): AnotherApiResult[String] = AnotherApiResult.failure(error)
    }
    val anotherService = new AnotherTestService

    implicit def apiResultToAnotherApiResult[T](result: ApiResult[T]): AnotherApiResult[T] = result leftMap {
      error => AnotherApiError(error.message)
    }
    
    type SimpleResult[T] = FutureValidation[String, T] 
    implicit def apiResultToSimpleResult[T](result: ApiResult[T]):SimpleResult[T] = result leftMap {
      error => error.message
    }
    
    "convertTo" should {

      "convert between 2 defined apis using an implicit conversions" in new ApiScope {

        val simpleActual = service.respondWith("hello").convertTo[SimpleResult]
        val anotherActual = service.respondWith("hello").convertTo[AnotherApiResult]

        simpleActual.await.result === "hello"
        anotherActual.await.result === "hello"
        
        val simpleActualFailure = service.failWith(error).convertTo[SimpleResult]
        val anotherActualFailure = service.failWith(error).convertTo[AnotherApiResult]
        
        simpleActualFailure.await.error === "An error occurred"
        anotherActualFailure.await.error === AnotherApiError("An error occurred")

      }
    }
    
    "convertAndMap" should {

      "convert and map 2 defined apis using an implicit conversions" in new ApiScope {

        val actual: AnotherApiResult[Option[String]] = service.respondWith("hello") convertAndMap { x =>
          Some(x)
        }

        actual.await.result === Some("hello")

      }
    }

    "convertAndFlatMap" should {

      "convert and flatMap 2 defined apis using an implicit conversions" in new ApiScope {

        val actual = service.respondWith("hello") convertAndFlatMap { x =>
          anotherService.respondWith(s"$x world")
        }

        actual.await.result === "hello world"

      }
    }
  }
}