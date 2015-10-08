package com.bfil.api

import scala.concurrent.ExecutionContext

import scalaz._

trait TypedFutureValidationMonoidInstances[F] {
  self =>

  def zero: F
  def append(f1: F, f2: => F) = f1

  implicit val failureMonoid = new Monoid[F] {
    def zero = self.zero
    def append(f1: F, f2: => F) = self.append(f1, f2)
  }
  implicit val failureNelMonoid = new Monoid[NonEmptyList[F]] {
    def zero = NonEmptyList(self.zero)
    def append(f1: NonEmptyList[F], f2: => NonEmptyList[F]) = f1.append(f2)
  }

}

trait TypedFutureValidationApplyInstances[F] {

  type FV[T] = FutureValidation[F, T]
  type FVNel[T] = FutureValidationNel[F, T]

  implicit def FutureValidationApply[T](implicit ec: ExecutionContext, FS: Semigroup[F]): Apply[FV] =
    new Apply[FV] {
      def ap[A, B](fa: => FV[A])(f: => FV[A => B]): FV[B] = fa.ap(f)
      def map[A, B](fa: FV[A])(f: A => B): FV[B] = fa map f
    }
  implicit def FutureValidationNelApply[T](implicit ec: ExecutionContext, FS: Semigroup[NonEmptyList[F]]): Apply[FVNel] =
    new Apply[FVNel] {
      def ap[A, B](fa: => FVNel[A])(f: => FVNel[A => B]): FVNel[B] = fa.ap(f)
      def map[A, B](fa: FVNel[A])(f: A => B): FVNel[B] = fa map f
    }

}

trait TypedFutureValidationInstances[F]
  extends TypedFutureValidationMonoidInstances[F]
  with TypedFutureValidationApplyInstances[F]