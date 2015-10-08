package com.bfil.api

import scala.concurrent.Await
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

import scalaz._

trait AwaitableFutureValidation {

  implicit class AwaitableFutureValidationResult[F, S](result: FutureValidation[F, S]) {
    def await(implicit futureValidationTimeout: FiniteDuration = 5 seconds) =
      Await.result(result.toFuture, futureValidationTimeout)
  }

  implicit class InspectableValidation[F, T](validation: Validation[F, T]) {
    def result: T =
      if (validation.toEither.isRight) validation.toEither.right.get
      else throw new RuntimeException(s"Failed to get the result, error found: ${validation.toEither.left.get}")

    def error: F =
      if (validation.toEither.isLeft) validation.toEither.left.get
      else throw new RuntimeException(s"Failed to get the error, result found: ${validation.toEither.right.get}")
  }
}