package ca.canadianradar.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = RadarEngine()

    inner class RadarEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var radar: Bitmap? = null
        private var forecast = "Loading precipitation forecast..."
        private var lastUpdated = "Waiting for radar data"
        private var visible = false
        private var loading = false

        private val refreshTask = object : Runnable {
            override fun run() {
                if (visible) {
                    refreshWeather()
                    handler.postDelayed(this, 6 * 60 * 1000L)
                }
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (visible) {
    drawWallpaper()
    refreshWeather()
    handler.removeCallbacks(refreshTask)
                handler.postDelayed(refreshTask, 6 * 60 * 1000L)
            } else {
                handler.removeCallbacks(refreshTask)
            }
        }

        
override fun onSurfaceChanged(
    holder: SurfaceHolder,
    format: Int,
    width: Int,
    height: Int
) {
    super.onSurfaceChanged(holder, format, width, height)
    drawWallpaper()
}

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(refreshTask)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            handler.removeCallbacksAndMessages(null)
            super.onDestroy()
        }

        private fun refreshWeather() {
            if (loading) return
            loading = true

            Thread {
                var newRadar: Bitmap? = null
                var newForecast = forecast
                var newUpdated = lastUpdated

                try {
                    val radarUrl =
                        "https://geo.weather.gc.ca/geomet/?" +
                        "SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap" +
                        "&LAYERS=RADAR_1KM_RRAI&STYLES=" +
                        "&SRS=EPSG:4326" +
                        "&BBOX=-116,49,-109,55" +
                        "&WIDTH=900&HEIGHT=1100" +
                        "&FORMAT=image/png&TRANSPARENT=true"

                    newRadar = downloadBitmap(radarUrl)

                    val forecastUrl =
                        "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=51.29&longitude=-114.01" +
                        "&hourly=precipitation,precipitation_probability" +
                        "&forecast_days=1&timezone=America%2FEdmonton"

                    val json = JSONObject(downloadText(forecastUrl))
                    val hourly = json.getJSONObject("hourly")
                    val times = hourly.getJSONArray("time")
                    val amounts = hourly.getJSONArray("precipitation")
                    val probabilities =
                        hourly.getJSONArray("precipitation_probability")

                    val now = System.currentTimeMillis()
                    val dateFormat =
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)

                    var first = -1
                    for (i in 0 until times.length()) {
                        val time = dateFormat.parse(times.getString(i))
                        if (time != null && time.time >= now) {
                            first = i
                            break
                        }
                    }

                    if (first >= 0) {
                        val end = minOf(first + 3, times.length())
                        var total = 0.0
                        var maxChance = 0

                        for (i in first until end) {
                            total += amounts.optDouble(i, 0.0)
                            maxChance = maxOf(
                                maxChance,
                                probabilities.optInt(i, 0)
                            )
                        }

                        newForecast =
                            "Next 3h: ${"%.1f".format(Locale.US, total)} mm" +
                            "  |  Precipitation chance: $maxChance%"
                    }

                    newUpdated = "Updated " +
                        SimpleDateFormat(
                            "HH:mm",
                            Locale.US
                        ).format(Date())

                } catch (_: Exception) {
                    newForecast = "Forecast temporarily unavailable"
                    newUpdated = "Waiting for weather data"
                }

                handler.post {
                    if (newRadar != null) radar = newRadar
                    forecast = newForecast
                    lastUpdated = newUpdated
                    loading = false
                    drawWallpaper()
                }
            }.start()
        }

        private fun downloadBitmap(address: String): Bitmap? {
            val connection =
                URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty(
                "User-Agent",
                "CanadianRadarLiveWallpaper/1.0"
            )

            return try {
                connection.inputStream.use {
                    BitmapFactory.decodeStream(it)
                }
            } finally {
                connection.disconnect()
            }
        }

        private fun downloadText(address: String): String {
            val connection =
                URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty(
                "User-Agent",
                "CanadianRadarLiveWallpaper/1.0"
            )

            return try {
                connection.inputStream.bufferedReader().use {
                    it.readText()
                }
            } finally {
                connection.disconnect()
            }
        }


private fun drawWallpaper() {
    val holder = surfaceHolder
    var canvas: Canvas? = null

    try {
        canvas = holder.lockCanvas() ?: return

        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        canvas.drawColor(Color.rgb(12, 20, 29))

        radar?.let {
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(
                it,
                null,
                Rect(0, 0, width.toInt(), height.toInt()),
                paint
            )

            val shade = Paint().apply {
                color = Color.argb(65, 5, 12, 20)
            }
            canvas.drawRect(0f, 0f, width, height, shade)
        }

        val panel = Paint().apply {
            color = Color.argb(220, 10, 20, 31)
        }

        canvas.drawRoundRect(
            28f, 65f, width - 28f, 250f,
            24f, 24f, panel
        )

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        canvas.drawText(
            "CANADIAN RADAR",
            48f, 115f, textPaint
        )

        textPaint.textSize = 24f
        textPaint.typeface = android.graphics.Typeface.DEFAULT

        canvas.drawText(
            forecast,
            48f, 160f, textPaint
        )

        textPaint.textSize = 20f
        textPaint.color = Color.LTGRAY

        canvas.drawText(
            lastUpdated,
            48f, 205f, textPaint
        )

        textPaint.textSize = 18f

        canvas.drawText(
            "Radar: Environment Canada",
            32f, height - 45f, textPaint
        )

    } catch (e: Exception) {
        android.util.Log.e(
            "CanadianRadar",
            "Wallpaper drawing failed",
            e
        )
    } finally {
        if (canvas != null) {
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (_: Exception) {
            }
        }
    }
}

    }
}

