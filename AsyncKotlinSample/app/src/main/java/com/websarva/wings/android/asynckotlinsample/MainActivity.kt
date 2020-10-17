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
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
	companion object {
		private const val DEBUG_TAG = "AsyncTest"
		private const val WEATHERINFO_URL = "https://api.openweathermap.org/data/2.5/weather?lang=ja"
		private const val APP_ID = "913136635cfa3182bbe18e34ffd44849"
	}
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
	private fun showResult(telop: String, desc: String) {
		val tvWeatherTelop = findViewById<TextView>(R.id.tvWeatherTelop)
		val tvWeatherDesc = findViewById<TextView>(R.id.tvWeatherDesc)
		tvWeatherTelop.text = telop
		tvWeatherDesc.text = desc
	}

	private inner class ListItemClickListener: AdapterView.OnItemClickListener {
		override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
			val item = _list.get(position)
			val q = item.get("q")
			q?.let {
				val receiver = WeatherInfoReceiver()
				receiver.execute(WEATHERINFO_URL, it, APP_ID)
			}
		}
	}

	private inner class WeatherInfoReceiver {
		@UiThread
		fun execute(urlBase: String, q: String, appId: String) {
			val handler = HandlerCompat.createAsync(mainLooper)
			val backgroundReceiver = WeatherInfoBackgroundReceiver(handler, urlBase, q, appId)
			val executeService = Executors.newSingleThreadExecutor()
			executeService.submit(backgroundReceiver)
		}
	}

	private inner class WeatherInfoBackgroundReceiver(handler: Handler, urlBase: String, q: String, appId: String): Runnable {
		val _handler = handler
		val _urlFull = urlBase + "&q=" + q + "&appid=" + appId
		@WorkerThread
		override fun run() {
			var result = ""
			try {
				val url = URL(_urlFull)
				val con = url.openConnection() as HttpURLConnection
				con.connectTimeout = 1000
				con.readTimeout = 1000
				con.requestMethod = "GET"
				con.connect()
				val stream = con.inputStream
				result = is2String(stream)
				con.disconnect()
				stream.close()
			}
			catch(ex: SocketTimeoutException) {
				Log.e(DEBUG_TAG, "通信タイムアウト", ex)
			}
			val postExecutor = WeatherInfoPostExecutor(result)
			_handler.post(postExecutor)
		}

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

	private inner class WeatherInfoPostExecutor(result: String): Runnable {
		val _result = result
		@UiThread
		override fun run() {
			val rootJSON = JSONObject(_result)
			val cityName = rootJSON.getString("name")
			val weatherJSONArray = rootJSON.getJSONArray("weather")
			val weatherJSON = weatherJSONArray.getJSONObject(0)
			val description = weatherJSON.getString("description")
			val telop = cityName + "の天気"
			val desc = "現在は" + description + "です。"
			showResult(telop, desc)
		}

	}
}
