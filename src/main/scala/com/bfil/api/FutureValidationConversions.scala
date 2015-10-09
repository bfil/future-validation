package com.bfil.api

import scala.concurrent.{ ExecutionContext, Future }

import scalaz._

trait FutureValidationConversions {
  
  implicit def resultToApiResult[F, S](result: S): FutureValidation[F, S] = FutureValidation.success[F, S](result)
  implicit def errorToApiError[F, S](error: F): FutureValidation[F, S] = FutureValidation.failure[F, S](error)

  implicit protected class RichFutureValidation[F, S](val result: FutureValidation[F, S]) {
    private type FV[S] = FutureValidation[_, S]
    def convertTo[FV[S]](implicit convert: FutureValidation[F, S] => FV[S]): FV[S] = convert(result)
    def convertAndMap[G, T](f: S => T)(implicit convert: FutureValidation[F, S] => FutureValidation[G, S], ec: ExecutionContext): FutureValidation[G, T] = convert(result) map f
    def convertAndFlatMap[G, T](f: S => FutureValidation[G, T])(implicit convert: FutureValidation[F, S] => FutureValidation[G, S], ec: ExecutionContext): FutureValidation[G, T] = convert(result) flatMap f
  }
    
  implicit protected def futureToSealedFutureValidation[F, T](future: Future[T])(implicit ec: ExecutionContext, exceptionHandler: PartialFunction[Throwable, Validation[F, T]]): FutureValidation[F, T] =
    FutureValidation(future map Validation.success) recover exceptionHandler
  
}

object FutureValidationConversions extends FutureValidationConversions