package com.apidemo.util.stepchain

typealias Lambda<T> = ((T) -> Unit)

class StepAction<T>(
   val title: String,

   var beforeAction: Lambda<T>? = null,
   var afterAction: Lambda<T>? = null,

   var needToSkip: ((T) -> Boolean) = {false},
   var assertLambda: (() -> Unit)? = null,

   var retryCount: Int = 0,
   var retryLambda: ((T) -> Boolean)? = null,
   var failOnError: Boolean = true,

   var action: Lambda<T>
) {
   fun assert(lambda: (() -> Unit)) : StepAction<T> {
      assertLambda = lambda
      return this
   }

   fun afterAction(lambda: Lambda<T>) : StepAction<T> {
      afterAction = lambda
      return this
   }

   fun beforeAction(lambda: Lambda<T>) : StepAction<T> {
      beforeAction = lambda
      return this
   }

   /**
    * Enable retry logic when step completed with error
    *
    * [retryCount] - number of retries
    * [lambda] - should return *true* when no need for retry
    */
   fun retryCheck(
      retryCount: Int = 1,
      failOnError: Boolean = true,
      lambda: (T) -> Boolean,
   ) : StepAction<T> {
      this.retryCount = retryCount
      this.retryLambda = lambda
      this.failOnError = failOnError

      return this
   }
}