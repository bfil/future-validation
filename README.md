Future Validation
=================

A library that provides a `FutureValidation` type that wraps `scala.concurrent.Future[scalaz.Validation[Failure, Result]]`.

It allows to design strictly typed APIs, and take advantage of to the monadic nature of `FutureValidation` to perform serial or parallel computations, errors accumulations and other functional programming tasks using features provided by [Scalaz](https://github.com/scalaz/scalaz).

#### A quick example

A simple API contract, using a database client as an example, can be defined as follows:

```scala
package object db {
	type DatabaseResult[T] extends FutureValidation[DatabaseError, T]
	object DatabaseResult extends TypedFutureValidation[DatabaseError]
}
case class DatabaseError(code: Int, message: String)
```

Then an example database client would look like the following:

```scala
import db._
class DatabaseClient {
	
	// Handles future exceptions and converts them to the defined error for the API
	def exceptionHandler[T]: ExceptionHandler[DatabaseError, T] = {
		case ex: DuplicateKeyException => Validation.failure(DatabaseError(11000, ex.getMessage))
		case ex => Validation.failure(DatabaseError(0, ex.getMessage))
	}

	def findById(id: String): DatabaseResult[Entity] =
		DatabaseResult {
			Future(???)
		} recover exceptionHandler

	def insert(entity: Entity): DatabaseResult[Unit] = 
		DatabaseResult {
			Future(???)
		} recover exceptionHandler
}
```

`DatabaseResult` can be used in for comprehensions and supports various operations:

```scala
val client = new DatabaseClient
val result = for {
	a <- findById("1234")
	b <- findById(a)
} yield b
val resultWithMappedError = result.leftMap {
	case DatabaseError(0, _) => DatabaseError(0, "Something went wrong")
}
val insertResult = result.flatMap { entity =>
	client.insert(entity)
}
```

For more details on the supported features check the **Usage** section below.

Setting up the dependencies
---------------------------

__Future Validation__ is available on `Maven Central` (since version `0.3.0`), and it is cross compiled and published for Scala 2.12, 2.11 and 2.10.

*Older artifacts versions are not available anymore due to the shutdown of my self-hosted Nexus Repository in favour of Bintray*

Using SBT, add the following dependency to your build file:

```scala
libraryDependencies ++= Seq(
  "io.bfil" %% "future-validation" % "0.3.0"
)
```

If you have issues resolving the dependency, you can add the following resolver:

```scala
resolvers += Resolver.bintrayRepo("bfil", "maven")
```

Usage
-----

### Defining an API

APIs can be defined by simply choosing a name for the async result to use. F example, for a database client it would make sense to have a result type called `DatabaseResult`, that can either be successful and containing the expected result or it could return a `DatabaseError`.

Using the library, a simple definition would be defined as follows:

```scala
package object db {
	type DatabaseResult[T] extends FutureValidation[DatabaseError, T]
	object DatabaseResult extends TypedFutureValidation[DatabaseError]
}
case class DatabaseError(code: Int, message: String)
```

We defined a custom type that is a type alias for `FutureValidation[FailureType, T]`, and we define the companion object, usually keeping the same name is a good idea to avoid confusion.

The companion object can extend two different traits: `TypedFutureValidation` and `SealedFutureValidation`, they both take a type parameter, which is basically the failure type, in our example it's a simple case class `DatabaseError`.

Extending `TypedFutureValidation` leaves the exception handling up to the user, it is useful when the async operations require different exception handlers depending on the operations to perform.

If that's the case we would wrap the asynchronous client with something like the following:

```scala
import db._
class DatabaseClient {
	
	def readExceptionHandler[T]: ExceptionHandler[DatabaseError, T] = {
		case ex: QueryParseException => Validation.failure(DatabaseError(500, ex.getMessage))
		case ex => Validation.failure(DatabaseError(0, ex.getMessage))
	}

	def findById(id: String): DatabaseResult[Entity] =
		DatabaseResult {
			Future(???)
		} recover readExceptionHandler

	def writeExceptionHandler[T]: ExceptionHandler[DatabaseError, T] = {
		case ex: DuplicateKeyException => Validation.failure(DatabaseError(11000, ex.getMessage))
		case ex => Validation.failure(DatabaseError(0, ex.getMessage))
	}

	def insert(entity: Entity): DatabaseResult[Unit] = 
		DatabaseResult {
			Future(???)
		} recover writeExceptionHandler
}
```

In our example case let's assume we are wrapping a database library that returns well defined exceptions that are common for all operations, in this case we can rewrite the above API definition like this:

```scala
package object db {
	type DatabaseResult[T] extends FutureValidation[DatabaseError, T]
	object DatabaseResult extends SealedFutureValidation[DatabaseError] {
		def exceptionHandler[T]: ExceptionHandler[DatabaseError, T] = {
			case ex: DuplicateKeyException => Validation.failure(DatabaseError(11000, ex.getMessage))
			case ex: QueryParseException => Validation.failure(DatabaseError(500, ex.getMessage))
			case ex => Validation.failure(DatabaseError(0, ex.getMessage))
		}
	}
}
case class DatabaseError(code: Int, message: String)
```

The above just emplies that wrapping a `Future[T]` within a `DatabaseResult` will always catch the exceptions and turn them into the custom error type `DatabaseError`.

### Documentation

The methods documentation uses as an example the `DatabaseResult` API type defined above.

#### map

Maps the successful value of the future validation

```scala
val result = client.findById("1234") map { entity =>
	entity.copy(???)
}
```

#### flatMap

FlatMaps the successful value of the future validation into a successful/failure future validation

```scala
val stringResult: DatabaseResult[String] = 
	client.findById("1234") flatMap { entity =>
		DatabaseResult.success("stubbed")
	}
val errorResult: DatabaseResult[String] = 
	client.findById("1234") flatMap { entity =>
		DatabaseResult.failure(DatabaseError(100, "Some error"))
	}
```

#### leftMap / mapError

Maps the failure value of the future validation

```scala
val result = client.findById("1234") leftMap { error =>
	DatabaseError(200, "Mapped error")
}
val result2 = client.findById("1234") mapError { error =>
	DatabaseError(200, "Mapped error")
}
```

#### leftFlatMap / recoverError

FlatMaps the failure value of the future validation into a successful/failure future validation

```scala
val errorResult = client.findById("1234") leftFlatMap { error =>
	DatabaseResult.failure(DatabaseError(200, "Mapped error"))
}
val stringResult = client.findById("1234") leftFlatMap { error =>
	DatabaseResult.success("stubbed")
}
val stringResult = client.findById("1234") recoverError { error =>
	DatabaseResult.success("stubbed")
}
```

#### fold

Folds the future validation into a future using the provided failure and success mapping functions

```scala
val result: Future[Int] = client.findById("1234").fold(error => 0, result => 1)
```

#### recover

Handles the future exceptions

```scala
DatabaseResult {
  Future.failed(new Exception("An exception occurred"))
} recover {
  case ex => Validation.failure(DatabaseError(0, ex.getMessage))
}
```

#### orElse

Allows to define a default future validation value to use instead of the future validation failure

```scala
val result = client.findById("1234").orElse(DatabaseResult.success(???))
```

#### zip

Zips future validations together

```scala
val result: DatabaseResult[(Entity, Entity)] = client.findById("1234") zip client.findById("5678")
```

To zip more than 2 future validations together us the singleton object:

```scala
val result: DatabaseResult[(Entity, Entity, Entity)] = 
	DatabaseResult.zip(
		client.findById("1234"),
		client.findById("3456"),
		client.findById("5678")
	)
```

#### sequence

Turns a `List[FutureValidation[F, T]]` into a `FutureValidation[F, List[T]]`

```scala
val result: DatabaseResult[List[Entity]] = 
	DatabaseResult.sequence(
		List(client.findById("1234"), client.findById("5678"))
	)
```

#### traverse

Can be used to traverse lists to turn into future validations

```scala
val result: DatabaseResult[List[Entity]] = 
	DatabaseResult.traverse(List("1234", "5678")) { id =>
      client.findById(id)
    }
```

#### toFutureValidationNel

Transforms a future validation error type to a `NonEmptyList` type, useful to do error accumulation

```scala
val result = client.findById("1234").toFutureValidationNel
```

#### error accumulation

To be able to use Scalaz applicative building for error accumulation we need to have an instance of the `Apply` type for our `FutureValidation` type in place.

To facilitate this use case, the object defining the API can be extended with `TypedFutureValidationApplyInstances` like this:

```scala
package object db extends TypedFutureValidationApplyInstances[DatabaseError] {
	type DatabaseResult[T] extends FutureValidation[DatabaseError, T]
	type DatabaseResultNel[T] extends FutureValidationNel[DatabaseError, T] // this is just to alias the complex type
	object DatabaseResult extends TypedFutureValidation[DatabaseError]
}
```

So an `import db._` will also provide the instance of `Apply` for our API type, so that we can accumulate errors like so:

```scala
import scalaz._
import Scalaz._

val result1 = client.findById("1234").toFutureValidationNel
val result2 = client.findById("3456").toFutureValidationNel
val result3 = client.findById("5678").toFutureValidationNel

val result: DatabaseResultNel[(Entity, Entity, Entity)] = result1 |@| result2 |@| result3 {
	case (res1, res2, res3) => (res1, res2, res3)
}
```

If at least one of the operations fails, the errors will be accumulated in a `NonEmptyList[DatabaseError]`.

#### Monoid instances

An instance of the Scalaz `Monoid` can be included in the API by extending `TypedFutureValidationMonoidInstances`:

```scala
package object db extends TypedFutureValidationMonoidInstances[DatabaseError] {
	type DatabaseResult[T] extends FutureValidation[DatabaseError, T]
	object DatabaseResult extends TypedFutureValidation[DatabaseError]

	def zero = DatabaseError(0, "Default error")
}
```

Please note a zero/identity value should be provided, having the instance in scope would allow the use of if conditions or destructuring in for comprehensions. The `append` method can also be overridden if needed, by default it always uses the first semigroup.

#### All instances

Both instances of `Apply` and `Monoid` can be provided by extending a single trait, called `TypedFutureValidationInstances`.

License
-------

This software is licensed under the Apache 2 license, quoted below.

Copyright © 2015-2017 Bruno Filippone <http://bfil.io>

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    [http://www.apache.org/licenses/LICENSE-2.0]

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
