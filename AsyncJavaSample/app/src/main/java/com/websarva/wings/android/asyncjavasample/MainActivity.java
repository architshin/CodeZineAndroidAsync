package com.websarva.wings.android.asyncjavasample;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

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

/**
 * 『Androidアプリ開発の教科書』
 * 第11章
 * Web API連携サンプル
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
	private static final String APP_ID = "913136635cfa3182bbe18e34ffd44849";
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

	private class ListItemClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Map<String, String> item = _list.get(position);
			String q = item.get("q");

			WeatherInfoReceiver receiver = new WeatherInfoReceiver();
			receiver.execute(WEATHERINFO_URL, q, APP_ID);
		}
	}

	private class WeatherInfoReceiver {
		@UiThread
		public void execute(String urlBase, String q, String appId) {
			Looper mainLooper = Looper.getMainLooper();
			WeatherInfoBackgroundReceiver backgroundReceiver = new WeatherInfoBackgroundReceiver(mainLooper, urlBase, q, appId);
			Log.i(DEBUG_TAG, "非同期処理開始前");
			ExecutorService executorService  = Executors.newSingleThreadExecutor();
			executorService.submit(backgroundReceiver);
			Log.i(DEBUG_TAG, "非同期処理開始後");
		}
	}

	@WorkerThread
	private class WeatherInfoBackgroundReceiver implements Runnable {
		Handler _handler;
		String _urlFull;
		public WeatherInfoBackgroundReceiver(Looper mainLooper, String urlBase, String q, String appId) {
			_handler = new Handler(mainLooper);
			_urlFull = urlBase + "&q=" + q + "&appid=" + appId;
		}
		@Override
		public void run() {
			Log.i(DEBUG_TAG, "バックグラウンド処理中");
			HttpURLConnection con = null;
			InputStream is = null;
			String result = "";

			try {
				URL url = new URL(_urlFull);
				con = (HttpURLConnection) url.openConnection();
				con.setConnectTimeout(1000);
				con.setReadTimeout(1000);
				con.setRequestMethod("GET");
				con.connect();
				is = con.getInputStream();

				result = is2String(is);
			}
			catch(MalformedURLException ex) {
				Log.e(DEBUG_TAG, "URL変換失敗", ex);
			}
			catch(SocketTimeoutException ex) {
				Log.w(DEBUG_TAG, "通信タイムアウト", ex);
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
			Log.i(DEBUG_TAG, "バックグラウンド処理終了\n" + result);
			WeatherInfoPostExecuter postExecuter = new WeatherInfoPostExecuter();
			_handler.post(postExecuter);
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

	@UiThread
	private class WeatherInfoPostExecuter implements Runnable {
		@Override
		public void run() {
			Log.i(DEBUG_TAG, "PostExecute中");
		}
	}
}
