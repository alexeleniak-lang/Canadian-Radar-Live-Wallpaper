
package ca.canadianradar.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(16, 24, 32))
        }

        val title = TextView(this).apply {
            text = "CANADIAN RADAR"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "\nLive Canadian radar\nThree-hour precipitation forecast\n\nChoose the live wallpaper below."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val button = Button(this).apply {
            text = "Preview and set wallpaper"
            setOnClickListener {
                val intent = Intent(
                    WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
                ).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(
                            this@MainActivity,
                            WeatherWallpaperService::class.java
                        )
                    )
                }

                try {
                    startActivity(intent)
                } catch (_: Exception) {
                    startActivity(
                        Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                    )
                }
            }
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(button)
        setContentView(layout)
    }
}
