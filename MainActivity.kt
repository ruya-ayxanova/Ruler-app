package com.example.rulerapp

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.atan2
import kotlin.math.tan

// Ruler App: egim sensoru ile mesafe ve yukseklik hesaplar.
// Yontem: telefonu dik tut, once duvarin dibine, sonra tepesine nisan al.
class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var heightInput: EditText
    private lateinit var angleText: TextView
    private lateinit var resultText: TextView

    private var currentAngle = 0.0   // derece, yukari pozitif
    private var baseAngle: Double? = null
    private var gravity = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        heightInput = EditText(this).apply {
            hint = "Telefonun yerden yuksekligi (metre), orn: 1.5"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        angleText = TextView(this).apply { textSize = 20f; text = "Aci: 0°" }
        resultText = TextView(this).apply { textSize = 18f; text = "Once duvarin dibine nisan al." }

        val baseBtn = Button(this).apply {
            text = "1) Duvarin DIBINI kaydet"
            setOnClickListener { saveBase() }
        }
        val topBtn = Button(this).apply {
            text = "2) Duvarin TEPESINI kaydet"
            setOnClickListener { saveTop() }
        }

        listOf(heightInput, angleText, baseBtn, topBtn, resultText).forEach { root.addView(it) }
        setContentView(root)
    }

    private fun phoneHeight(): Double? = heightInput.text.toString().replace(',', '.').toDoubleOrNull()

    private fun saveBase() {
        // Dibe bakarken aci negatif (asagi) olmali
        baseAngle = currentAngle
        resultText.text = "Dip kaydedildi: %.1f°. Simdi tepeye nisan al.".format(currentAngle)
    }

    private fun saveTop() {
        val h = phoneHeight()
        val base = baseAngle
        if (h == null || base == null || base >= 0) {
            resultText.text = "Once yuksekligi gir ve dibe asagi dogru nisan alip kaydet."
            return
        }
        val distance = h / tan(Math.toRadians(-base))          // duvara uzaklik
        val wallHeight = h + distance * tan(Math.toRadians(currentAngle))
        resultText.text = "Uzaklik: %.2f m\nDuvar yuksekligi: %.2f m".format(distance, wallHeight)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Titremeyi azaltmak icin basit filtre
        val a = 0.9f
        for (i in 0..2) gravity[i] = a * gravity[i] + (1 - a) * event.values[i]
        // Telefon dikken: y = yukari, z = ekrandan disari. Kamera -z yonune bakar.
        currentAngle = Math.toDegrees(atan2(-gravity[2].toDouble(), gravity[1].toDouble()))
        angleText.text = "Aci: %.1f°".format(currentAngle)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
