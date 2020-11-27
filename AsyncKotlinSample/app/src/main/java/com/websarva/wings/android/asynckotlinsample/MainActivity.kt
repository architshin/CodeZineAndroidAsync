package com.websarva.wings.android.asynckotlinsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.HandlerCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

/**
 * CodeZine
 * Web API連携サンプル
 * Kotlin版
 *
 * アクティビティクラス。
 *
 * @author Shinzo SAITO
 */
class MainActivity : AppCompatActivity() {
	companion object {
		/**
		 * ログに記載するタグ用の文字列。
		 */
		private const val DEBUG_TAG = "AsyncTest"
		/**
		 * お天気情報のURL。
		 */
		private const val WEATHERINFO_URL = "https://api.openweathermap.org/data/2.5/weather?lang=ja"
		/**
		 * お天気APIにアクセスすするためのAPI Key。
		 * ※※※※※この値は各自のものに書き換える!!※※※※※
		 */
		private const val APP_ID = ""
	}

	/**
	 * リストビューに表示させるリストデータ。
	 */
	private var _list: MutableList<MutableMap<String, String>> = mutableListOf()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		_list = createList()

		val lvCityList = findViewById<ListView>(R.id.lvCityList)
		val from  = arrayOf("name")
		val to = intArrayOf(android.R.id.text1)
		val adapter = SimpleAdapter(applicationContext, _list, android.R.layout.simple_list_item_1, from, to)
		lvCityList.adapter = adapter
		lvCityList.onItemClickListener = ListItemClickListener()
	}

	/**
	 * リストビューに表示させる天気ポイントリストデータを生成するメソッド。
	 *
	 * @return 生成された天気ポイントリストデータ。
	 */
	private fun createList(): MutableList<MutableMap<String, String>> {
		var list: MutableList<MutableMap<String, String>> = mutableListOf()

		var city = mutableMapOf("name" to "大阪", "q" to "Osaka")
		list.add(city)
		city = mutableMapOf("name" to "神戸", "q" to "Kobe")
		list.add(city)
		city = mutableMapOf("name" to "京都", "q" to "Kyoto")
		list.add(city)
		city = mutableMapOf("name" to "大津", "q" to "Otsu")
		list.add(city)
		city = mutableMapOf("name" to "奈良", "q" to "Nara")
		list.add(city)
		city = mutableMapOf("name" to "和歌山", "q" to "Wakayama")
		list.add(city)
		city = mutableMapOf("name" to "姫路", "q" to "Himeji")
		list.add(city)

		return list;
	}

	/**
	 * リストがタップされた時の処理が記述されたリスナクラス。
	 */
	private inner class ListItemClickListener: AdapterView.OnItemClickListener {
		override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
			val item = _list.get(position)
			val q = item.get("q")
			q?.let {
				val url = "$WEATHERINFO_URL&q=$q&appid=$APP_ID"
				asyncExecute(url)
			}
		}
	}

	/**
	 * お天気情報の取得処理を行うメソッド。
	 *
	 * @param url お天気情報を取得するURL。
	 */
	@UiThread
	fun asyncExecute(url: String) {
		val handler = HandlerCompat.createAsync(mainLooper)
		val backgroundTask = BackgroundTask(handler, url)
		val executeService = Executors.newSingleThreadExecutor()
		executeService.submit(backgroundTask)
	}

	/**
	 * 非同期でお天気情報APIにアクセスするためのクラス。
	 *
	 * @param handler ハンドラオブジェクト。
	 * @param url お天気情報を取得するURL。
	 */
	private inner class BackgroundTask(handler: Handler, url: String): Runnable {
		/**
		 * ハンドラオブジェクト。
		 */
		private val _handler = handler
		/**
		 * お天気情報を取得するURL。
		 */
		private val _url = url

		@WorkerThread
		override fun run() {
			var result = ""
			val url = URL(_url)
			val con = url.openConnection() as? HttpURLConnection
			con?.run {
				requestMethod = "GET"
				connect()
				result = is2String(inputStream)
				disconnect()
				inputStream.close()
			}
			val postExecutor = PostExecutor(result)
			_handler.post(postExecutor)
		}

		/**
		 * InputStreamオブジェクトを文字列に変換するメソッド。 変換文字コードはUTF-8。
		 *
		 * @param stream 変換対象のInputStreamオブジェクト。
		 * @return 変換された文字列。
		 */
		private fun is2String(stream: InputStream): String {
			val sb = StringBuilder()
			val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
			var line = reader.readLine()
			while(line != null) {
				sb.append(line)
				line = reader.readLine()
			}
			reader.close()
			return sb.toString()
		}
	}

	/**
	 * 非同期でお天気情報を取得した後にUIスレッドでその情報を表示するためのクラス。
	 *
	 * @param result Web APIから取得したお天気情報JSON文字列。
	 */
	private inner class PostExecutor(result: String): Runnable {
		/**
		 * 取得したお天気情報JSON文字列。
		 */
		private val _result = result

		@UiThread
		override fun run() {
			val rootJSON = JSONObject(_result)
			val cityName = rootJSON.getString("name")
			val weatherJSONArray = rootJSON.getJSONArray("weather")
			val weatherJSON = weatherJSONArray.getJSONObject(0)
			val description = weatherJSON.getString("description")
			val telop = cityName + "の天気"
			val desc = "現在は" + description + "です。"

			val tvWeatherTelop = findViewById<TextView>(R.id.tvWeatherTelop)
			val tvWeatherDesc = findViewById<TextView>(R.id.tvWeatherDesc)
			tvWeatherTelop.text = telop
			tvWeatherDesc.text = desc
		}
	}
}
