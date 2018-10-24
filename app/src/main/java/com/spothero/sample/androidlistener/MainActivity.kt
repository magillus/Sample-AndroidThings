package com.spothero.sample.androidlistener

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import com.google.android.things.contrib.driver.apa102.Apa102
import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.pwmservo.Servo
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import timber.log.Timber

/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {

    var servoAngle: Double = 90.0

    lateinit var sensor: Bmx280
    lateinit var butA: Button
    lateinit var butB: Button
    lateinit var butC: Button

    lateinit var servo: Servo

    val handler = Handler()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)

        butA = RainbowHat.openButtonA()
        butB = RainbowHat.openButtonB()
        butC = RainbowHat.openButtonC()
        servo = RainbowHat.openServo()
        servo.setEnabled(true)
        servo.setAngleRange(0.0, 90.0)
        servoAngle = servo.angle

        butA.setOnButtonEventListener { button, pressed ->
            if (pressed) updateServo(servo.minimumAngle)
        }
        butB.setOnButtonEventListener { button, pressed ->
            if (pressed) updateServo((servo.minimumAngle + servo.maximumAngle) / 2.0)
        }
        butC.setOnButtonEventListener { button, pressed ->
            if (pressed) updateServo(servo.maximumAngle)
        }
        sensor = RainbowHat.openSensor()
        sensor.temperatureOversampling = Bmx280.OVERSAMPLING_1X
        sensor.pressureOversampling = Bmx280.OVERSAMPLING_1X

        updateTemp()

    }

    private fun updateServo(newAngle: Double) {
        servoAngle = if (newAngle > servo.maximumAngle) {
            servo.maximumAngle
        } else if (newAngle < servo.minimumAngle) {
            servo.minimumAngle
        } else {
            newAngle
        }
        servo.angle = servoAngle
        Timber.i("Update servo: $servoAngle angle")
    }

    private fun updateTemp() {
        if (isDestroyed) {
            return
        }
        RainbowHat.openDisplay().use { display ->
            display.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX)
            val displayText = "T${sensor.readTemperature()}"
            Timber.i("Temperature= ${sensor.readTemperature()}")
            display.display(displayText)
            display.setEnabled(true)
        }
        handler.postDelayed(this::updatePressure, 3000L)
    }

    private fun updatePressure() {
        if (isDestroyed) {
            return
        }
        RainbowHat.openDisplay().use { display ->
            display.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX)
            val displayText = "p${sensor.readPressure().toInt()}"
            Timber.i("Pressure= ${sensor.readPressure()}")
            display.display(displayText)
            display.setEnabled(true)
        }
        handler.postDelayed(this::updateTemp, 3000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        butA.close()
        butB.close()
        butC.close()
        servo.setEnabled(false)
        servo.close()
        sensor.close()
    }

    fun enableStrip(on: Boolean) {// not used
        RainbowHat.openLedStrip().direction = Apa102.Direction.REVERSED
        RainbowHat.openLedStrip().use { strip ->
            if (on) {
                strip.write(listOf(0x330000, 0x888800, 0x878845, 0xff00ff, 0x660066, 0x440044, 0x220022).toIntArray())
                strip.brightness = 1
            } else {
                strip.brightness = 0
                strip.write(listOf(0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000).toIntArray())
            }
        }

    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onMenuItemSelected(featureId, item)
    }
}
