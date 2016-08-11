package com.bfil.api

import scala.concurrent.{ ExecutionContext, Future }

import scalaz._

trait SealedFutureValidation[F] extends TypedFutureValidation[F] {

  def exceptionHandler[S]: ExceptionHandler[F, S]

  def identityExceptionHandler[S](implicit FM: Monoid[F]): ExceptionHandler[F, S] = {
    case ex => Validation.failure(FM.zero)
  }

  override def apply[S](inner: Future[Validation[F, S]])(implicit ec: ExecutionContext): FutureValidation[F, S] =
    super.apply(inner) recover exceptionHandler[S]

  override def apply[S](inner: Validation[F, S])(implicit ec: ExecutionContext): FutureValidation[F, S] =
    super.apply(inner) recover exceptionHandler[S]

  override def apply[S](inner: => Future[S])(implicit ec: ExecutionContext): FutureValidation[F, S] =
    super.apply(inner) recover exceptionHandler[S]

}