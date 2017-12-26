package io.bfil.api

import scala.concurrent.{ ExecutionContext, Future }

import scalaz._

trait TypedFutureValidation[F] {

  def apply[S](inner: Future[Validation[F, S]])(implicit ec: ExecutionContext): FutureValidation[F, S] = FutureValidation(inner)

  def apply[S](inner: Validation[F, S])(implicit ec: ExecutionContext): FutureValidation[F, S] = FutureValidation(Future.successful(inner))

  def apply[S](inner: => Future[S])(implicit ec: ExecutionContext): FutureValidation[F, S] = FutureValidation(inner)

  def success[S](s: S): FutureValidation[F, S] = FutureValidation.success(s)

  def failure[S](f: F): FutureValidation[F, S] = FutureValidation.failure(f)

  def sequence[T](seq: List[FutureValidation[F, T]])(implicit ec: ExecutionContext, S: Semigroup[F]): FutureValidation[F, List[T]] =
    FutureValidation.sequence(seq)

  def traverse[T1, T2](seq: List[T1])(fn: T1 => FutureValidation[F, T2])(implicit ec: ExecutionContext, S: Semigroup[F]): FutureValidation[F, List[T2]] =
    FutureValidation.traverse(seq)(fn)

  def zip[T1, T2](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2)] =
    FutureValidation.zip(t1, t2)

  def zip[T1, T2, T3](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3)] =
    FutureValidation.zip(t1, t2, t3)

  def zip[T1, T2, T3, T4](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4)] =
    FutureValidation.zip(t1, t2, t3, t4)

  def zip[T1, T2, T3, T4, T5](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5)] =
    FutureValidation.zip(t1, t2, t3, t4, t5)

  def zip[T1, T2, T3, T4, T5, T6](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5], t6: FutureValidation[F, T6])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5, T6)] =
    FutureValidation.zip(t1, t2, t3, t4, t5, t6)

  def zip[T1, T2, T3, T4, T5, T6, T7](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5], t6: FutureValidation[F, T6], t7: FutureValidation[F, T7])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5, T6, T7)] =
    FutureValidation.zip(t1, t2, t3, t4, t5, t6, t7)

  def zip[T1, T2, T3, T4, T5, T6, T7, T8](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5], t6: FutureValidation[F, T6], t7: FutureValidation[F, T7], t8: FutureValidation[F, T8])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5, T6, T7, T8)] =
    FutureValidation.zip(t1, t2, t3, t4, t5, t6, t7, t8)

  def zip[T1, T2, T3, T4, T5, T6, T7, T8, T9](t1: FutureValidation[F, T1], t2: FutureValidation[F, T2], t3: FutureValidation[F, T3], t4: FutureValidation[F, T4], t5: FutureValidation[F, T5], t6: FutureValidation[F, T6], t7: FutureValidation[F, T7], t8: FutureValidation[F, T8], t9: FutureValidation[F, T9])(implicit ec: ExecutionContext): FutureValidation[F, (T1, T2, T3, T4, T5, T6, T7, T8, T9)] =
    FutureValidation.zip(t1, t2, t3, t4, t5, t6, t7, t8, t9)

}