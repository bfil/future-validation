package com.bfil

import com.bfil.api.FutureValidation

import scalaz._

package object api {
  type FutureValidationNel[F, T] = FutureValidation[NonEmptyList[F], T]
  type ExceptionHandler[F, S] = PartialFunction[Throwable, Validation[F, S]]
}