package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Message
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.androimads.retrolin.*
import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {
    var isDarkTheme: Boolean = false

    var baseUrl = "https://api.openweathermap.org/"
    var apiKey = "5b02059971f9bdd9d9f2bc3918b5bf67"
    var lang = "ru"
    var lat = "59.92285455155549"
    var lon = "30.29312550684746"

    var weatherResponse: WeatherResponse? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        if (savedInstanceState != null) {
            isDarkTheme = savedInstanceState.getBoolean("theme")
        }

        if (isDarkTheme) {
            theme.applyStyle(R.style.AppThemeDark, true)
        } else {
            theme.applyStyle(R.style.AppTheme, true)
        }

        setContentView(R.layout.activity_main)

        doWork()
    }

    var viewModelJob = Job()
    val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    fun doWork() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getWeekDataCoroutines()
            }
        }

    }

    fun changeTheme(view: View) {
        isDarkTheme = !isDarkTheme
        recreate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("theme", isDarkTheme)
    }

    fun getWeekData() {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(WeatherService::class.java)
        val call = service.getForecastWeatherData(lat, lon, apiKey, lang)
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.code() == 200) {
                    weatherResponse = response.body()!!

                    setDailyList(weatherResponse!!)
                    saveDailyList(weatherResponse!!)
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                println(t.message)

                weatherResponse = getDailyList()
                if (weatherResponse != null) {
                    setDailyList(weatherResponse!!)
                }
            }
        })
    }

    fun getWeekDataRX() {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        val weatherApi: WeatherServiceRX = retrofit.create(WeatherServiceRX::class.java)

        weatherApi.getForecastWeatherData(lat, lon, apiKey, lang)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableSingleObserver<WeatherResponse?>() {
                override fun onSuccess(@NonNull response: WeatherResponse) {
                    weatherResponse = response

                    setDailyList(weatherResponse!!)
                    saveDailyList(weatherResponse!!)
                }

                override fun onError(@NonNull e: Throwable) {
                    println("onError $e")

                    weatherResponse = getDailyList()
                    if (weatherResponse != null) {
                        setDailyList(weatherResponse!!)
                    }
                }
            })
    }

    suspend fun getWeekDataCoroutines() {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

        val weatherApi: WeatherServiceCoroutine = retrofit.create(WeatherServiceCoroutine::class.java)

        val result = weatherApi
            .getForecastWeatherData(lat, lon, apiKey, lang)
            .await()

        weatherResponse = result

        if (weatherResponse != null) {
            runOnUiThread {
                setDailyList(weatherResponse!!)
                saveDailyList(weatherResponse!!)
            }
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun saveDailyList(weatherResponse: WeatherResponse) {
        val prefs = getSharedPreferences("saved_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val jsonString = Gson().toJson(weatherResponse)
        editor.putString("dailyJSON", jsonString)
        editor.apply()
    }

    fun getDailyList(): WeatherResponse? {
        val prefs = getSharedPreferences("saved_data", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("dailyJSON","")
        if (jsonString != "") {
            return Gson().fromJson(jsonString, WeatherResponse::class.java)
        } else {
            return null
        }
    }

    @SuppressLint("SetTextI18n")
    fun setDailyList(weatherResponse: WeatherResponse) {
        val imgDays = listOf(imageViewMO, imageViewTU, imageViewWE, imageViewTH, imageViewFR, imageViewSA, imageViewSU)
        val labelDays = listOf(textViewMO, textViewTU, textViewWE, textViewTH, textViewFR, textViewSA, textViewSU)

        for (i in 0..6) {
            val weather = weatherResponse.daily[i]
            val image = when (weather.weather[0].id) {
                in 500..700 -> R.drawable.rainy
                else -> R.drawable.sun
            }
            imgDays[i].setImageResource(image)

            val cTemp = weather.temp!!.day - 273
            labelDays[i].text = "%.2f".format(cTemp) + "℃"
        }

        selectDay(0)
    }

    @SuppressLint("SetTextI18n")
    fun setDay(weatherResponse: DailyWeatherResponse) {
        val temp = weatherResponse.temp!!.day - 273
        textView5.text = "%.2f".format(temp) + "℃"
        textView6.text = weatherResponse.wind_speed.toString() + " м/c"
        textView7.text = weatherResponse.pressure.toString() + " hPa"
        textView8.text = weatherResponse.humidity.toString() + " %"
    }

    @SuppressLint("ResourceAsColor")
    fun selectDay(view: View) {
        val id = view.getTag().toString().toInt()
        val weather = weatherResponse ?: return
        setDay(weather.daily[id])
    }

    fun selectDay(id: Int) {
        val weather = weatherResponse ?: return
        setDay(weather.daily[id])
    }
}