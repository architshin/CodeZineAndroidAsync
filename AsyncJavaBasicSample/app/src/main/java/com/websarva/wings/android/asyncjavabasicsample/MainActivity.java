package com.websarva.wings.android.asyncjavabasicsample;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CodeZine
 * 非同期処理サンプル
 * Java版
 *
 * アクティビティクラス。
 *
 * @author Shinzo SAITO
 */
public class MainActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		asyncExecute();
	}

	/**
	 * 非同期処理を開始するためのメソッド。
	 */
	@UiThread
	public void asyncExecute() {
		Looper mainLooper = Looper.getMainLooper();
		Handler handler = HandlerCompat.createAsync(mainLooper);
		BackgroundTask backgroundTask = new BackgroundTask(handler);
		ExecutorService executorService  = Executors.newSingleThreadExecutor();
		executorService.submit(backgroundTask);
	}

	/**
	 * バックグラウンド処理用クラス。
	 */
	private class BackgroundTask implements Runnable {
		/**
		 * UIスレッドを表すハンドラオブジェクト。
		 */
		private final Handler _handler;

		/**
		 * コンストラクタ。
		 *
		 * @param handler UIスレッドを表すハンドラオブジェクト。
		 */
		public BackgroundTask(Handler handler) {
			_handler = handler;
		}

		@WorkerThread
		@Override
		public void run() {
			Log.i("Async-BackgroundTask", "ここに非同期処理を記述する");
			PostExecutor postExecutor = new PostExecutor();
			_handler.post(postExecutor);
		}
	}

	/**
	 * バックグラウンドスレッドの終了後にUIスレッドで行う処理用クラス。
	 */
	private class PostExecutor implements Runnable {
		@UiThread
		@Override
		public void run() {
			Log.i("Async-PostExecutor", "ここにUIスレッドで行いたい処理を記述する");
		}
	}
}
