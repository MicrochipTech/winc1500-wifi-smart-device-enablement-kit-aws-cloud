package com.amazonaws.mchp.awsprovisionkit.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MyThreadPool {
	private static ExecutorService FULL_TASK_EXECUTOR = null;

	static {
		FULL_TASK_EXECUTOR = (ExecutorService) Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				// TODO Auto-generated method stub
				if (r == null)
					return null;

				Thread task = new Thread(r);
				task.setPriority(Thread.MIN_PRIORITY + 1);
				return task;
			}
		});
	}

	public static ExecutorService getExecutor() {
		return FULL_TASK_EXECUTOR;
	}
}
