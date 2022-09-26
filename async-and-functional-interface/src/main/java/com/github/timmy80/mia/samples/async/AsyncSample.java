package com.github.timmy80.mia.samples.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.timmy80.mia.core.Async;
import com.github.timmy80.mia.core.Task;
import com.github.timmy80.mia.core.TimeLimit;
import com.github.timmy80.mia.core.TimeLimitExceededException;

/**
 * A class with strongly typed callback methods that will be called
 * asynchronously by an executor and a MiA Task
 */
public class AsyncSample {
	public static class MiaTask extends Task {

		public MiaTask(String name) throws IllegalArgumentException {
			super(name);
		}

		@Override
		public void eventStartTask() {
		} // nothing to be done

	}

	static MiaTask miaTask = new MiaTask("mia-task");

	//************************************************************************
	// Runnable methods (void)
	//************************************************************************
	public void runMeWithNoArgs() throws RuntimeException {
		if (Thread.currentThread() != miaTask)
			throw new RuntimeException("Called by wrong Thread");
	}

	public void runMeWith4Args(int intArg, String strArg, long longArg, float floatArg) {
	}

	//************************************************************************
	// Callable methods (non void)
	//************************************************************************
	public long makeMeSleep(long time_ms) throws InterruptedException {
		long begin = System.currentTimeMillis();
		Thread.sleep(time_ms);
		return System.currentTimeMillis() - begin;
	}

	public static void main(String[] args) throws Throwable {
		ExecutorService ex = Executors.newFixedThreadPool(1); // a java standard Executor
		miaTask.start(); // A MiA Executor
		AsyncSample obj = new AsyncSample();

		try {
			// Simple case: no args, Runnable
			miaTask.runBefore(TimeLimit.in(5000), obj::runMeWithNoArgs).get();

			// Harder case: multiple strongly typed arguments.
			// You must respect the types of the method to make it work
			Async.runLater(ex, obj::runMeWith4Args, 12, "Hello world!", 1658996565L, 115689.3f).get();

			// Timeout case: call a method but it does not end in time.
			try {
				Async.callBefore(miaTask, TimeLimit.in(5000), obj::makeMeSleep, 5500L).get(); // sleep will be
																								// interrupted
			} catch (ExecutionException e) {
				if (!(e.getCause() instanceof TimeLimitExceededException))
					throw e;
			}
		} finally {
			System.exit(0); // stop all the Threads (ExecutorService, MiaTask, HashedTimerWheel...)
		}
	}
}
