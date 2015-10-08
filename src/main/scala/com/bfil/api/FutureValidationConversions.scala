package com.bfil.api

import scala.concurrent.{ ExecutionContext, Future }

import scalaz._

trait FutureValidationConversions {
  
  implicit def resultToApiResult[F, S](result: S): FutureValidation[F, S] = FutureValidation.success[F, S](result)
  implicit def errorToApiError[F, S](error: F): FutureValidation[F, S] = FutureValidation.failure[F, S](error)

  implicit protected class RichFutureValidation[F, T](val result: FutureValidation[F, T]) {
    def convertAndFlatMap[F2, T2](f: T => FutureValidation[F2, T2])(implicit convert: FutureValidation[F, T] => FutureValidation[F2, T], ec: ExecutionContext): FutureValidation[F2, T2] = convert(result) flatMap f
  }
    
  implicit protected def futureToSealedFutureValidation[F, T](future: Future[T])(implicit ec: ExecutionContext, exceptionHandler: PartialFunction[Throwable, Validation[F, T]]): FutureValidation[F, T] =
    FutureValidation(future map Validation.success) recover exceptionHandler
  
}

object FutureValidationConversions extends FutureValidationConversions