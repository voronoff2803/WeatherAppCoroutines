package com.androimads.retrolin
import com.google.gson.annotations.SerializedName
import java.util.ArrayList

class WeatherResponse {
    @SerializedName("daily")
    var daily = ArrayList<DailyWeatherResponse>()
}

class DailyWeatherResponse {
    @SerializedName("dt")
    var dt: Float = 0.toFloat()
    @SerializedName("temp")
    var temp: Temp? = null
    @SerializedName("humidity")
    var humidity: Float = 0.toFloat()
    @SerializedName("pressure")
    var pressure: Float = 0.toFloat()
    @SerializedName("wind_speed")
    var wind_speed: Float = 0.toFloat()
    @SerializedName("weather")
    var weather = ArrayList<Weather>()
}

class Temp {
    @SerializedName("day")
    var day: Float = 0.toFloat()
}

class Weather {
    @SerializedName("id")
    var id: Int = 0
    @SerializedName("main")
    var main: String? = null
    @SerializedName("description")
    var description: String? = null
    @SerializedName("icon")
    var icon: String? = null
}
