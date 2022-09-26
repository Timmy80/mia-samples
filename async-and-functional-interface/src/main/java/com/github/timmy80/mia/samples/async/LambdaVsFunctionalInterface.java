package com.github.timmy80.mia.samples.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.timmy80.mia.core.Async;

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
