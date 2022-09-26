# MiA Async and @FunctionalInterface

This sample shows you how to use `@FunctionalInterface` to execute callbacks asynchronously in MiA.

MiA offers `@FunctionalInterface`s for Void (Runnable) and NonVoid (Callable) methods up to 4 arguments.  
These interfaces support throwing exceptions and are exploited through the methods of the various execution stages (`Task`, `Terminal`, `TerminalState`) and the `Async` class.

Why `@FunctionalInterface` ? To avoid using lambdas which are not always easy to read. For Example:

```java
public class LambdaVsFunctionalInterface {

	void synchronousMethod(String arg1, int arg2, Long arg3) {
		// ...
	}
	
	Void handleResult(Void a1, Throwable t) {
		// Handle success or failure
		return null;
	}

	public static void main(String[] args) {
		ExecutorService ex = Executors.newFixedThreadPool(1);
		try{
			LambdaVsFunctionalInterface obj = new LambdaVsFunctionalInterface();
					
			// lambda
			CompletableFuture<Void> f1 = Async.runLater(ex, ()-> obj.synchronousMethod("Hello world", 1,  12568L));
			f1.handle((r, t)-> obj.handleResult(r, t));
			
			// @FunctionalInterface
			CompletableFuture<Void> f2 = Async.runLater(ex, obj::synchronousMethod, "Hello world", 2, 12568L);
			f2.handle(obj::handleResult);
			
		} finally {
			ex.shutdownNow();
		}
	}

}
```

The `@FunctionalInterface`s of MiA provided for creating callbacks are subclasses of `Async`.

`Async` also provides all the methods you need to use these interfaces with an asynchronous `Executor` (methods also implemented by `Task`, `Terminal` and `TerminalState`). The methods are named using the follwing principles:

- starts with <strong>run</strong> for void returning methods and <strong>call</strong> for non void methods.
- ends with <strong>Before</strong> when the execution is time constrained and <strong>Later</strong> otherwise.

See [AsyncSample.java](./src/main/java/com/github/timmy80/mia/samples/async/AsyncSample.java)

## Run this Sample with maven

```bash
mvn compile exec:java
```
