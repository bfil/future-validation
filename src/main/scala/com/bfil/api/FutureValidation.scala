package com.bfil.api

import scala.{ Left, Right }
import scala.concurrent.{ ExecutionContext, Future }

import scalaz._
import scalaz.Scalaz._

class FutureValidation[F, S](private val inner: Future[Validation[F, S]]) {
  import FutureValidation._

  def map[T](fn: S => T)(implicit ec: ExecutionContext): FutureValidation[F, T] =
    FutureValidation(inner map { validation => validation map fn })

  def flatMap[T](fn: S => FutureValidation[F, T])(implicit ec: ExecutionContext): FutureValidation[F, T] =
    FutureValidation {
      inner.flatMap {
        case Success(s) => fn(s).toFuture
        case Failure(f) => Future.successful(f.failure[T])
      }
    }

  def leftMap[G](fn: F => G)(implicit ec: ExecutionContext): FutureValidation[G, S] =
    FutureValidation(inner map { validation => validation leftMap fn })

  def leftFlatMap[G](fn: F => FutureValidation[G, S])(implicit ec: ExecutionContext): FutureValidation[G, S] =
    FutureValidation {
      inner.flatMap {
        case Success(s) => Future.successful(s.success[G])
        case Failure(f) => fn(f).toFuture
      }
    }

  def mapError[G](fn: F => G)(implicit ec: ExecutionContext): FutureValidation[G, S] = leftMap(fn)
  def recoverError[G](fn: F => FutureValidation[G, S])(implicit ec: ExecutionContext): FutureValidation[G, S] = leftFlatMap(fn)

  def filter(f: S => Boolean)(implicit ec: ExecutionContext, FM: Monoid[F]): FutureValidation[F, S] =
    flatMap { s => if (f(s)) FutureValidation.success(s) else FutureValidation.failure(FM.zero) }

  def withFilter(f: S => Boolean)(implicit ec: ExecutionContext, FM: Monoid[F]): FutureValidation[F, S] = filter(f)

  def fold[T](fail: F => T = identity[F] _, succ: (S) => T = identity[S] _)(implicit ec: ExecutionContext): Future[T] =
    inner map { validation => validation fold (fail = fail, succ = succ) }

  def foreach[T](fn: S => T)(implicit ec: ExecutionContext): Unit = map(fn)

  def recover(fn: PartialFunction[Throwable, Validation[F, S]])(implicit ec: ExecutionContext): FutureValidation[F, S] =
    FutureValidation[F, S](inner.recover(fn))

  def orElse(x: => FutureValidation[F, S])(implicit ec: ExecutionContext): FutureValidation[F, S] =
    FutureValidation {
      inner.flatMap {
        case Success(s) => Future.successful(s.success)
        case Failure(f) => x.toFuture
      }
    }

  def ap[T](f: => FutureValidation[F, S => T])(implicit ec: ExecutionContext, FS: Semigroup[F]): FutureValidation[F, T] =
    FutureValidation {
      (toFuture zip f.toFuture).map {
        case (Success(a), Success(f))   => Validation.success(f(a))
        case (Failure(e), Success(_))   => Validation.failure(e)
        case (Success(_), Failure(e))   => Validation.failure(e)
        case (Failure(e1), Failure(e2)) => Validation.failure(FS.append(e1, e2))
      }
    }

  def zip[T](t: FutureValidation[F, T])(implicit ec: ExecutionContext): FutureValidation[F, (S, T)] = FutureValidation.zip(this, t)

  def toFuture: Future[Validation[F, S]] = inner
  def toFutureEither(implicit ec: ExecutionContext): Future[Either[F, S]] = fold(Left(_), Right(_))
  def toFutureOption(implicit ec: ExecutionContext): Future[Option[S]] = fold(_ => None, Some(_))
  def toFutureValidationNel(implicit ec: ExecutionContext): FutureValidationNel[F, S] = leftMap { error => NonEmptyList(error) }
}

object FutureValidation {

  def apply[F, S](inner: Future[Validation[F, S]]): FutureValidation[F, S] = new FutureValidation[F, S](inner)

  def apply[F, S](inner: => Future[S])(implicit ec: ExecutionContext): FutureValidation[F, S] = FutureValidation(inner map Validation.success)

  def success[F, S](s: S): FutureValidation[F, S] = FutureValidation(Future.successful(Validation.success(s)))

  def failure[F, S](f: F): FutureValidation[F, S] = FutureValidation(Future.successful(Validation.failure(f)))

  private def flattenValidations[F, T](list: List[Validation[F, T]])(implicit FS: Semigroup[F]): Validation[F, List[T]] = {
    type L[S] = Validation[F, S]
    list.sequence[L, T]
  }

  def sequence[F, T](seq: List[FutureValidation[F, T]])(implicit ec: ExecutionContext, FS: Semigroup[F]): FutureValidation[F, List[T]] =
    FutureValidation {
      Future.sequence(seq.map(_.toFuture)).map(x => flattenValidations(x))
    }

  def traverse[F, T1, T2](seq: List[T1])(fn: T1 => FutureValidation[F, T2])(implicit ec: ExecutionContext, FS: Semigroup[F]): FutureValidation[F, List[T2]] =
    FutureValidation {
      Future.traverse(seq)(x => fn(x).toFuture).map(x => flattenValidations(x))
    }

  def zip[F, T1, T2](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2)] =
    for { r1 <- t1; r2 <- t2 } yield (r1, r2)

  def zip[F, T1, T2, T3](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3)] =
    for { r1 <- t1; r2 <- t2; r3 <- t3 } yield (r1, r2, r3)

  def zip[F, T1, T2, T3, T4](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4)] =
    for { r1 <- t1; r2 <- t2; r3 <- t3; r4 <- t4 } yield (r1, r2, r3, r4)

  def zip[F, T1, T2, T3, T4, T5](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5)] =
    for { r1 <- t1; r2 <- t2; r3 <- t3; r4 <- t4; r5 <- t5 } yield (r1, r2, r3, r4, r5)

  def zip[F, T1, T2, T3, T4, T5, T6](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5], t6: FutureValidation[F, T6])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5, T6)] =
    for { r1 <- t1; r2 <- t2; r3 <- t3; r4 <- t4; r5 <- t5; r6 <- t6 } yield (r1, r2, r3, r4, r5, r6)

  def zip[F, T1, T2, T3, T4, T5, T6, T7](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5], t6: FutureValidation[F, T6], t7: FutureValidation[F, T7])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5, T6, T7)] =
    for { r1 <- t1; r2 <- t2; r3 <- t3; r4 <- t4; r5 <- t5; r6 <- t6; r7 <- t7 } yield (r1, r2, r3, r4, r5, r6, r7)

  def zip[F, T1, T2, T3, T4, T5, T6, T7, T8](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5], t6: FutureValidation[F, T6], t7: FutureValidation[F, T7], t8: FutureValidation[F, T8])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5, T6, T7, T8)] =
    for { r1 <- t1; r2 <- t2; r3 <- t3; r4 <- t4; r5 <- t5; r6 <- t6; r7 <- t7; r8 <- t8 } yield (r1, r2, r3, r4, r5, r6, r7, r8)

  def zip[F, T1, T2, T3, T4, T5, T6, T7, T8, T9](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5], t6: FutureValidation[F, T6], t7: FutureValidation[F, T7], t8: FutureValidation[F, T8], t9: FutureValidation[F, T9])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5, T6, T7, T8, T9)] =
    for { r1 <- t1; r2 <- t2; r3 <- t3; r4 <- t4; r5 <- t5; r6 <- t6; r7 <- t7; r8 <- t8; r9 <- t9 } yield (r1, r2, r3, r4, r5, r6, r7, r8, r9)

}