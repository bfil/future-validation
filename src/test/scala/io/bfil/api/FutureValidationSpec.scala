package io.bfil.api

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import io.bfil.api.TestApi.ApiResult

import TestApi.ApiResult
import scalaz._
import scalaz.Scalaz._

object TestApi extends TypedFutureValidationInstances[ApiError] {
  type ApiResult[T] = FutureValidation[ApiError, T]
  object ApiResult extends TypedFutureValidation[ApiError]
  def zero = ApiError("An error occurred")
}
case class ApiError(message: String)

class TestService {
  import TestApi._
  def respondWith(result: String): ApiResult[String] = ApiResult.success(result)
  def failWith(error: ApiError): ApiResult[String] = ApiResult.failure(error)
}

class FutureValidationSpec extends Specification with Mockito with AwaitableFutureValidation {

  import TestApi._
  trait ApiScope extends Scope {
    val service = spy(new TestService)
    val error = ApiError("An error occurred")
  }

  "TypedFutureValidation" in {

    "constructor" should {

      "create a new future validation from a future" in new ApiScope {

        val actual = ApiResult {
          Future.successful("hello")
        }

        actual.await.result must beEqualTo("hello")

      }

      "create a future validation from a validation" in new ApiScope {

        val actual = ApiResult {
          Validation.success("hello")
        }

        actual.await.result must beEqualTo("hello")

      }

      "create a future validation from a future that contains a validation" in new ApiScope {

        val actual = ApiResult {
          Future.successful(Validation.success("hello"))
        }

        actual.await.result must beEqualTo("hello")

      }

    }

    "toFuture" should {

      "return the inner future" in new ApiScope {

        val future = Future.successful(Validation.success("hello"))

        val actual = ApiResult(future).toFuture

        Await.result(actual, 5 seconds).result must beEqualTo("hello")

      }

    }

    "toFutureEither" should {

      "return the inner future successful value and turn the validation into an either" in new ApiScope {

        val future = Future.successful(Validation.success("hello"))

        val actual = ApiResult(future).toFutureEither

        Await.result(actual, 5 seconds).right.get must beEqualTo("hello")

      }

      "return the inner future failure value and turn the validation into an either" in new ApiScope {

        val future = Future.successful(Validation.failure(error))

        val actual = ApiResult(future).toFutureEither

        Await.result(actual, 5 seconds).left.get must beEqualTo(error)

      }

    }

    "toFutureOption" should {

      "return the inner future successful value and turn the validation into an option" in new ApiScope {

        val future = Future.successful(Validation.success("hello"))

        val actual = ApiResult(future).toFutureOption

        Await.result(actual, 5 seconds) must beEqualTo(Some("hello"))

      }

      "return the inner future failure value and turn the validation into an option" in new ApiScope {

        val future: Future[Validation[ApiError, String]] = Future.successful(Validation.failure(error))

        val actual = ApiResult(future).toFutureOption

        Await.result(actual, 5 seconds) must beEqualTo(None)

      }

    }

    "fold" should {

      "fold the successful and failure values to a future containing the expected successful value" in new ApiScope {

        val actual = service.respondWith("hello").fold(error => 0, result => 1)

        Await.result(actual, 5 seconds) must beEqualTo(1)

      }

      "fold the successful and failure values to a future containing the expected failure value" in new ApiScope {

        val actual = service.failWith(error).fold(error => 0, result => 1)

        Await.result(actual, 5 seconds) must beEqualTo(0)

      }

    }

    "recover" should {

      "use an exception handler to map future exceptions to failures" in new ApiScope {

        val actual: ApiResult[String] = ApiResult {
          Future.failed(new Exception("An exception occurred"))
        } recover {
          case ex => Validation.failure(error)
        }

        actual.await.error must beEqualTo(error)

      }

      "use an exception handler to map future exceptions to successes" in new ApiScope {

        val actual: ApiResult[String] = ApiResult {
          Future.failed(new Exception("An error occurred"))
        } recover {
          case ex => Validation.success("hello")
        }

        actual.await.result must beEqualTo("hello")

      }

    }

    "orElse" should {

      "allow defining a default value for future validation failures" in new ApiScope {

        val actual = service.failWith(error).orElse(service.respondWith("default"))

        actual.await.result must beEqualTo("default")

      }

      "not be executed for successful future validation" in new ApiScope {

        val actual = service.respondWith("hello").orElse(service.respondWith("default"))

        actual.await.result must beEqualTo("hello")

      }

    }

    "map" should {

      "map the future validation successful values as expected" in new ApiScope {

        val actual = service.respondWith("1").map(_.toInt)

        actual.await.result must beEqualTo(1)

      }

      "not change the future validation failure values" in new ApiScope {

        val actual = service.failWith(error).map(_.toInt)

        actual.await.error must beEqualTo(error)

      }

    }

    "foreach" should {

      "execute the given function if the future validation is successful" in new ApiScope {

        var executed = false

        service.respondWith("1").foreach(_ => executed = true)

        executed must beEqualTo(true)

      }

      "not execute the given function if the future validation fails" in new ApiScope {

        var executed = false

        val actual = service.failWith(error).foreach(_ => executed = true)

        executed must beEqualTo(false)

      }

    }

    "flatMap" should {

      "map and flatten the future validation successful values as expected" in new ApiScope {

        val actual = service.respondWith("hello").flatMap { x =>
          service.respondWith(s"$x world")
        }

        actual.await.result must beEqualTo("hello world")

      }

      "map and flatten the future validation successful values into failures if needed" in new ApiScope {

        val actual = service.respondWith("hello").flatMap { x =>
          service.failWith(error)
        }

        actual.await.error must beEqualTo(error)

      }

      "not change the future validation failure values" in new ApiScope {

        val actual = service.failWith(error).flatMap { x =>
          service.respondWith(s"$x world")
        }

        actual.await.error must beEqualTo(error)

      }

    }

    "leftMap" should {

      "map the future validation failure values as expected" in new ApiScope {

        val actual = service.failWith(error).leftMap(_.copy(message = "Mapped error"))

        actual.await.error must beEqualTo(ApiError("Mapped error"))

      }

      "not change the future validation successful values" in new ApiScope {

        val actual = service.respondWith("hello").leftMap(_.copy(message = "Mapped error"))

        actual.await.result must beEqualTo("hello")

      }

    }

    "mapError" should {

      "map the future validation failure values as expected" in new ApiScope {

        val actual = service.failWith(error).mapError(_.copy(message = "Mapped error"))

        actual.await.error must beEqualTo(ApiError("Mapped error"))

      }

      "not change the future validation successful values" in new ApiScope {

        val actual = service.respondWith("hello").mapError(_.copy(message = "Mapped error"))

        actual.await.result must beEqualTo("hello")

      }

    }

    "leftFlatMap" should {

      "map and flatten the future validation failure values as expected" in new ApiScope {

        val actual = service.failWith(error).leftFlatMap { x =>
          service.failWith(ApiError(s"Whooops - ${x.message}"))
        }

        actual.await.error must beEqualTo(ApiError("Whooops - An error occurred"))

      }

      "map and flatten the future validation failures values into successes if needed" in new ApiScope {

        val actual = service.failWith(error).leftFlatMap { x =>
          service.respondWith("hello")
        }

        actual.await.result must beEqualTo("hello")

      }

      "not change the future validation successful values" in new ApiScope {

        val actual = service.respondWith("hello").leftFlatMap { x =>
          service.failWith(ApiError(s"Whooops - ${x.message}"))
        }

        actual.await.result must beEqualTo("hello")

      }

    }

    "recoverError" should {

      "map and flatten the future validation failure values as expected" in new ApiScope {

        val actual = service.failWith(error).recoverError { x =>
          service.failWith(ApiError(s"Whooops - ${x.message}"))
        }

        actual.await.error must beEqualTo(ApiError("Whooops - An error occurred"))

      }

      "map and flatten the future validation failures values into successes if needed" in new ApiScope {

        val actual = service.failWith(error).recoverError { x =>
          service.respondWith("hello")
        }

        actual.await.result must beEqualTo("hello")

      }

      "not change the future validation successful values" in new ApiScope {

        val actual = service.respondWith("hello").recoverError { x =>
          service.failWith(ApiError(s"Whooops - ${x.message}"))
        }

        actual.await.result must beEqualTo("hello")

      }

    }

    "in a for comprehension" should {

      "return the expected result when all future validations are successful" in new ApiScope {

        val actual = for {
          res1 <- service.respondWith("hello")
          res2 <- service.respondWith("world")
          res3 <- service.respondWith(s"$res1 $res2")
        } yield res3

        actual.await.result must beEqualTo("hello world")

        there were three(service).respondWith(any)

      }

      "fail fast" in new ApiScope {

        val actual = for {
          res1 <- service.respondWith("hello")
          res2 <- service.failWith(error)
          res3 <- service.respondWith(s"$res1 $res2")
        } yield res3

        actual.await.error must beEqualTo(error)

        there was one(service).respondWith(any)
        there was one(service).failWith(any)

      }

      "have a working filter function" in new ApiScope {

        val actual =
          for {
            a <- service.respondWith("hello") if a == "hello"
          } yield a

        actual.await.result must beEqualTo("hello")

        there were one(service).respondWith(any)

      }

      "return the failure zero value if the predicate of the filter is not met" in new ApiScope {

        val actual =
          for {
            a <- service.respondWith("hello") if a == "hell"
          } yield a

        actual.await.error must beEqualTo(error)

        there was one(service).respondWith(any)

      }

      "have a working filter function for future validation non empty list" in new ApiScope {

        val actual =
          for {
            a <- service.respondWith("hello").toFutureValidationNel if a == "hello"
          } yield a

        actual.await.result must beEqualTo("hello")

        there were one(service).respondWith(any)

      }

      "return the failure zero value if the predicate of the filter is not met for future validation non empty list" in new ApiScope {

        val actual =
          for {
            a <- service.respondWith("hello").toFutureValidationNel if a == "hell"
          } yield a

        actual.await.error must beEqualTo(NonEmptyList(error))

        there was one(service).respondWith(any)

      }

      "support deconstruction" in new ApiScope {

        val actual =
          for {
            (a, b, c) <- (service.respondWith("hello") |@| service.respondWith("world") |@| service.respondWith("again")) {
              case (a, b, c) => (a, b, c)
            }
          } yield (a, b, c)

        actual.await.result must beEqualTo(("hello", "world", "again"))

        there were three(service).respondWith(any)

      }

    }

    "zip" should {

      "return the expected result when zipped future validations are successful" in new ApiScope {

        val actual = service.respondWith("1") zip service.respondWith("2")

        actual.await.result must beEqualTo(("1", "2"))

      }

      "return the expected result when 2 zipped future validations are successful" in new ApiScope {

        val actual = ApiResult.zip(service.respondWith("1"), service.respondWith("2"))

        actual.await.result must beEqualTo(("1", "2"))

      }

      "return the expected result when 3 zipped future validations are successful" in new ApiScope {

        val actual = ApiResult.zip(service.respondWith("1"), service.respondWith("2"), service.respondWith("3"))

        actual.await.result must beEqualTo(("1", "2", "3"))

      }

      "return the expected result when 4 zipped future validations are successful" in new ApiScope {

        val actual = ApiResult.zip(service.respondWith("1"), service.respondWith("2"), service.respondWith("3"), service.respondWith("4"))

        actual.await.result must beEqualTo(("1", "2", "3", "4"))

      }

      "return the expected result when 5 zipped future validations are successful" in new ApiScope {

        val actual = ApiResult.zip(service.respondWith("1"), service.respondWith("2"), service.respondWith("3"), service.respondWith("4"), service.respondWith("5"))

        actual.await.result must beEqualTo(("1", "2", "3", "4", "5"))

      }

      "return the expected result when 6 zipped future validations are successful" in new ApiScope {

        val actual = ApiResult.zip(service.respondWith("1"), service.respondWith("2"), service.respondWith("3"), service.respondWith("4"), service.respondWith("5"), service.respondWith("6"))

        actual.await.result must beEqualTo(("1", "2", "3", "4", "5", "6"))

      }

      "return the expected result when 7 zipped future validations are successful" in new ApiScope {

        val actual = ApiResult.zip(service.respondWith("1"), service.respondWith("2"), service.respondWith("3"), service.respondWith("4"), service.respondWith("5"), service.respondWith("6"), service.respondWith("7"))

        actual.await.result must beEqualTo(("1", "2", "3", "4", "5", "6", "7"))

      }

      "return the expected result when 8 zipped future validations are successful" in new ApiScope {

        val actual = ApiResult.zip(service.respondWith("1"), service.respondWith("2"), service.respondWith("3"), service.respondWith("4"), service.respondWith("5"), service.respondWith("6"), service.respondWith("7"), service.respondWith("8"))

        actual.await.result must beEqualTo(("1", "2", "3", "4", "5", "6", "7", "8"))

      }

      "return the expected result when 9 zipped future validations are successful" in new ApiScope {

        val actual = ApiResult.zip(service.respondWith("1"), service.respondWith("2"), service.respondWith("3"), service.respondWith("4"), service.respondWith("5"), service.respondWith("6"), service.respondWith("7"), service.respondWith("8"), service.respondWith("9"))

        actual.await.result must beEqualTo(("1", "2", "3", "4", "5", "6", "7", "8", "9"))

      }

      "return a failure when any zipped future validation fails" in new ApiScope {

        val actual = ApiResult.zip(service.respondWith("hello"), service.failWith(error), service.respondWith(s"again"))

        actual.await.error must beEqualTo(error)

      }

    }

    "applicative expression" should {

      "return the expected result when all future validations are successful" in new ApiScope {

        val actual = (service.respondWith("hello") |@| service.respondWith("world") |@| service.respondWith("again")) {
          case (a, b, c) => (a, b, c)
        }

        actual.await.result must beEqualTo(("hello", "world", "again"))

        there were three(service).respondWith(any)

      }

      "return the first error when any future validation fails" in new ApiScope {

        val actual = (service.failWith(error) |@| service.respondWith("world") |@| service.failWith(error)) {
          case (a, b, c) => (a, b, c)
        }

        actual.await.error must beEqualTo(error)

        there was one(service).respondWith(any)
        there were two(service).failWith(any)

      }

      "accumulate errors for non empty list failures" in new ApiScope {

        val actual = (service.failWith(error).toFutureValidationNel |@| service.respondWith("world").toFutureValidationNel |@| service.failWith(error).toFutureValidationNel) {
          case (a, b, c) => (a, b, c)
        }

        actual.await.error must beEqualTo(NonEmptyList(error, error))

        there was one(service).respondWith(any)
        there were two(service).failWith(any)

      }

    }

    "sequence" should {

      "turn a list of future validations into a future validation of list" in new ApiScope {

        val responses = List(service.respondWith("hello"), service.respondWith("world"))

        val actual = ApiResult.sequence(responses)

        actual.await.result must beEqualTo(List("hello", "world"))

      }

      "return any failure" in new ApiScope {

        val responses = List(service.respondWith("hello"), service.failWith(error))

        val actual = ApiResult.sequence(responses)

        actual.await.error must beEqualTo(error)

      }

    }

    "traverse" should {

      "traverse a list of future validations" in new ApiScope {

        val actual = ApiResult.traverse(List("hello", "world")) { x =>
          service.respondWith(s"traversed $x")
        }

        actual.await.result must beEqualTo(List("traversed hello", "traversed world"))

      }

      "return any failure happened during the traversal" in new ApiScope {

        val actual = ApiResult.traverse(List("hello", "world")) { x =>
          if (x == "hello") service.respondWith(s"traversed $x")
          else service.failWith(error)
        }

        actual.await.error must beEqualTo(error)

      }

    }

  }
}