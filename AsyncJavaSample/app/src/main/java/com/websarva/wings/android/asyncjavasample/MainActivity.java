package com.websarva.wings.android.asyncjavasample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

/**
 * CodeZine
 * Web API連携サンプル
 * Java版
 *
 * アクティビティクラス。
 *
 * @author Shinzo SAITO
 */
public class MainActivity extends AppCompatActivity {
	/**
	 * ログに記載するタグ用の文字列。
	 */
	private static final String DEBUG_TAG = "AsyncTest";
	/**
	 * お天気情報のURL。
	 */
	private static final String WEATHERINFO_URL = "https://api.openweathermap.org/data/2.5/weather?lang=ja";
	/**
	 * お天気APIにアクセスすするためのAPI Key。
	 * ※※※※※この値は各自のものに書き換える!!※※※※※
	 */
	private static final String APP_ID = "";
	/**
	 * リストビューに表示させるリストデータ。
	 */
	private List<Map<String, String>> _list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		_list  = createList();

		ListView lvCityList = findViewById(R.id.lvCityList);
		String[] from = {"name"};
		int[] to = {android.R.id.text1};
		SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), _list, android.R.layout.simple_expandable_list_item_1, from, to);
		lvCityList.setAdapter(adapter);
		lvCityList.setOnItemClickListener(new ListItemClickListener());
	}

	/**
	 * リストビューに表示させる天気ポイントリストデータを生成するメソッド。
	 *
	 * @return 生成された天気ポイントリストデータ。
	 */
	private List<Map<String, String>> createList() {
		List<Map<String, String>> list = new ArrayList<>();

		Map<String, String> map = new HashMap<>();
		map.put("name", "大阪");
		map.put("q", "Osaka");
		list.add(map);
		map = new HashMap<>();
		map.put("name", "神戸");
		map.put("q", "Kobe");
		list.add(map);
		map = new HashMap<>();
		map.put("name", "京都");
		map.put("q", "Kyoto");
		list.add(map);
		map = new HashMap<>();
		map.put("name", "大津");
		map.put("q", "Otsu");
		list.add(map);
		map = new HashMap<>();
		map.put("name", "奈良");
		map.put("q", "Nara");
		list.add(map);
		map = new HashMap<>();
		map.put("name", "和歌山");
		map.put("q", "Wakayama");
		list.add(map);
		map = new HashMap<>();
		map.put("name", "姫路");
		map.put("q", "Himeji");
		list.add(map);

		return list;
	}

	/**
	 * リストがタップされた時の処理が記述されたリスナクラス。
	 */
	private class ListItemClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Map<String, String> item = _list.get(position);
			String q = item.get("q");
			String url = WEATHERINFO_URL + "&q=" + q + "&appid=" + APP_ID;

			asyncExecute(url);
		}
	}

	/**
	 * お天気情報の取得処理を行うメソッド。
	 *
	 * @param url お天気情報を取得するURL。
	 */
	@UiThread
	public void asyncExecute(final String url) {
		Looper mainLooper = Looper.getMainLooper();
		Handler handler = HandlerCompat.createAsync(mainLooper);
		BackgroundTask backgroundTask = new BackgroundTask(handler, url);
		ExecutorService executorService  = Executors.newSingleThreadExecutor();
		executorService.submit(backgroundTask);
	}

	/**
	 * 非同期でお天気情報APIにアクセスするためのクラス
	 */
	private class BackgroundTask implements Runnable {
		/**
		 * UIスレッドを表すハンドラオブジェクト。
		 */
		private final Handler _handler;
		/**
		 * お天気情報を取得するURL。
		 */
		private final String _url;

		/**
		 * コンストラクタ。
		 *
		 * @param handler UIスレッドを表すハンドラオブジェクト。
		 */
		public BackgroundTask(Handler handler, String url) {
			_handler = handler;
			_url = url;
		}

		@WorkerThread
		@Override
		public void run() {
			HttpURLConnection con = null;
			InputStream is = null;
			String result = "";

			try {
				URL url = new URL(_url);
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				con.connect();
				is = con.getInputStream();

				result = is2String(is);
			}
			catch(MalformedURLException ex) {
				Log.e(DEBUG_TAG, "URL変換失敗", ex);
			}
			catch(IOException ex) {
				Log.e(DEBUG_TAG, "通信失敗", ex);
			}
			finally {
				if(con != null) {
					con.disconnect();
				}
				if(is != null) {
					try {
						is.close();
					}
					catch(IOException ex) {
						Log.e(DEBUG_TAG, "InputStream解放失敗", ex);
					}
				}
			}
			PostExecutor postExecutor = new PostExecutor(result);
			_handler.post(postExecutor);
		}

		/**
		 * InputStreamオブジェクトを文字列に変換するメソッド。 変換文字コードはUTF-8。
		 *
		 * @param is 変換対象のInputStreamオブジェクト。
		 * @return 変換された文字列。
		 * @throws IOException 変換に失敗した時に発生。
		 */
		private String is2String(InputStream is) throws IOException {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			StringBuffer sb = new StringBuffer();
			char[] b = new char[1024];
			int line;
			while(0 <= (line = reader.read(b))) {
				sb.append(b, 0, line);
			}
			return sb.toString();
		}
	}

	/**
	 * 非同期でお天気情報を取得した後にUIスレッドでその情報を表示するためのクラス。
	 */
	private class PostExecutor implements Runnable {
		/**
		 * 取得したお天気情報JSON文字列。
		 */
		private final String _result;

		/**
		 * コンストラクタ。
		 *
		 * @param result Web APIから取得したお天気情報JSON文字列。
		 */
		public PostExecutor(String result) {
			_result = result;
		}

		@UiThread
		@Override
		public void run() {
			String cityName = "";
			String weather = "";
			try {
				JSONObject rootJSON = new JSONObject(_result);
				cityName = rootJSON.getString("name");
				JSONArray weatherJSONArray = rootJSON.getJSONArray("weather");
				JSONObject weatherJSON = weatherJSONArray.getJSONObject(0);
				weather = weatherJSON.getString("description");
			}
			catch(JSONException ex) {
				Log.e(DEBUG_TAG, "JSON解析失敗", ex);
			}

			String telop = cityName + "の天気";
			String desc = "現在は" + weather + "です。";
			TextView tvWeatherTelop = findViewById(R.id.tvWeatherTelop);
			TextView tvWeatherDesc = findViewById(R.id.tvWeatherDesc);
			tvWeatherTelop.setText(telop);
			tvWeatherDesc.setText(desc);
		}
	}
}
