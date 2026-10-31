package com.dpiconfig

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val FFM_PACKAGE = "com.dts.freefiremax"
    private val SHIZUKU_CODE = 100
    private var userService: IUserService? = null

    private val presets = linkedMapOf(
        "Nhe tay" to -2,
        "Can bang" to 0,
        "Bam tam" to 2,
        "Keo muot" to 3,
        "Sieu ba" to 5
    )

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, UserService::class.java.name)
    ).daemon(false).processNameSuffix("service")
     .debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            userService = IUserService.Stub.asInterface(binder)
            updateShizukuStatus(true)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            userService = null
            updateShizukuStatus(false)
        }
    }

    private val permListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == SHIZUKU_CODE && result == android.content.pm.PackageManager.PERMISSION_GRANTED)
            bindSvc()
        else
            toast("Shizuku: tu choi quyen")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Shizuku.addRequestPermissionResultListener(permListener)

        val seekBar    = findViewById<SeekBar>(R.id.seekBarDpi)
        val tvValue    = findViewById<TextView>(R.id.tvDpiValue)
        val tvStatus   = findViewById<TextView>(R.id.tvStatus)
        val btnShizuku = findViewById<Button>(R.id.btnShizuku)
        val btnApply   = findViewById<Button>(R.id.btnApply)
        val btnLaunch  = findViewById<Button>(R.id.btnLaunchFFM)
        val radioGroup = findViewById<RadioGroup>(R.id.radioPresets)

        seekBar.max = 14
        seekBar.progress = 7
        tvValue.text = "DPI Config: 0"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, f: Boolean) {
                tvValue.text = "DPI Config: ${p - 7}"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        radioGroup.setOnCheckedChangeListener { _, id ->
            val btn = findViewById<RadioButton>(id) ?: return@setOnCheckedChangeListener
            val sp = presets[btn.text.toString()] ?: 0
            seekBar.progress = sp + 7
            tvValue.text = "DPI Config: $sp"
        }

        btnShizuku.setOnClickListener {
            when {
                !isAvail() -> toast("Mo Shizuku app truoc")
                !hasPerm() -> Shizuku.requestPermission(SHIZUKU_CODE)
                else -> bindSvc()
            }
        }

        btnApply.setOnClickListener { applyConfig(seekBar.progress - 7, tvStatus) }

        btnLaunch.setOnClickListener {
            applyConfig(seekBar.progress - 7, tvStatus)
            val i = packageManager.getLaunchIntentForPackage(FFM_PACKAGE)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            } else {
                tvStatus.text = "Free Fire Max chua cai"
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            }
        }

        if (isAvail() && hasPerm()) bindSvc()
        updateShizukuStatus(userService != null)
    }

    private fun bindSvc() {
        try { Shizuku.bindUserService(serviceArgs, serviceConnection) }
        catch (e: Exception) { toast("Bind that bai: ${e.message}") }
    }

    private fun isAvail() = try { Shizuku.pingBinder() } catch (e: Exception) { false }

    private fun hasPerm() = try {
        Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    private fun applyConfig(speed: Int, tv: TextView) {
        val svc = userService ?: run {
            tv.text = "Shizuku chua ket noi"
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            return
        }
        if (svc.applyPointerSpeed(speed) == 0) {
            tv.text = "DPI Config $speed - ap dung thanh cong"
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        } else {
            tv.text = "Loi apply"
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
        }
    }

    private fun updateShizukuStatus(on: Boolean) {
        val tv = findViewById<TextView>(R.id.tvShizukuStatus)
        tv.text = if (on) "Shizuku: Da ket noi" else "Shizuku: Chua ket noi"
        tv.setTextColor(ContextCompat.getColor(this,
            if (on) android.R.color.holo_green_light else android.R.color.holo_red_light))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permListener)
        try { Shizuku.unbindUserService(serviceArgs, serviceConnection, true) } catch (_: Exception) {}
    }
}
