package com.websarva.wings.android.asynccoroutinesample

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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import kotlin.coroutines.CoroutineContext

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
		private const val APP_ID = "913136635cfa3182bbe18e34ffd44849"
	}

	/**
	 * リストビューに表示させるリストデータ。
	 */
	private val _list: MutableList<MutableMap<String, String>> = mutableListOf()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		var city = mutableMapOf("name" to "大阪", "q" to "Osaka")
		_list.add(city)
		city = mutableMapOf("name" to "神戸", "q" to "Kobe")
		_list.add(city)
		city = mutableMapOf("name" to "京都", "q" to "Kyoto")
		_list.add(city)
		city = mutableMapOf("name" to "大津", "q" to "Otsu")
		_list.add(city)
		city = mutableMapOf("name" to "奈良", "q" to "Nara")
		_list.add(city)
		city = mutableMapOf("name" to "和歌山", "q" to "Wakayama")
		_list.add(city)
		city = mutableMapOf("name" to "姫路", "q" to "Himeji")
		_list.add(city)

		val lvCityList = findViewById<ListView>(R.id.lvCityList)
		val from  = arrayOf("name")
		val to = intArrayOf(android.R.id.text1)
		val adapter = SimpleAdapter(applicationContext, _list, android.R.layout.simple_list_item_1, from, to)
		lvCityList.adapter = adapter
		lvCityList.onItemClickListener = ListItemClickListener()
	}

	@UiThread
	private fun weatherInfoReceiver(urlBase: String, q: String, appId: String) {
		lifecycleScope.launch {
			val result = weatherInfoBackgroundReceiver(urlBase, q, appId)
			showResult(result)
		}
	}

	@WorkerThread
	private suspend fun weatherInfoBackgroundReceiver(urlBase: String, q: String, appId: String): String = withContext(Dispatchers.IO) {
		val urlFull = "$urlBase&q=$q&appid=$appId"
		var result = ""
		try {
			val url = URL(urlFull)
			val con = url.openConnection() as? HttpURLConnection
			con?.run {
				connectTimeout = 1000
				readTimeout = 1000
				requestMethod = "GET"
				connect()
				result = is2String(inputStream)
				disconnect()
				inputStream.close()
			}
		} catch (ex: SocketTimeoutException) {
			Log.e(DEBUG_TAG, "通信タイムアウト", ex)
		}
		result
	}

	/**
	 * 取得したお天気情報を画面に表示するメソッド。
	 *
	 * @param result Web APIから取得したお天気情報JSON文字列。
	 */
	@UiThread
	private fun showResult(result: String) {
		val rootJSON = JSONObject(result)
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

	/**
	 * リストがタップされた時の処理が記述されたリスナクラス。
	 */
	private inner class ListItemClickListener: AdapterView.OnItemClickListener {
		override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
			val item = _list.get(position)
			val q = item.get("q")
			q?.let {
				weatherInfoReceiver(WEATHERINFO_URL, it, APP_ID)
			}
		}
	}
}
