package com.example.mocklocationserver.web

import android.Manifest
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mocklocationserver.web.databinding.ActivityMainBinding
import com.example.mocklocationserver.web.service.MockLocationService
import com.example.mocklocationserver.web.service.WifiInfoCollectService
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String?>

    private var alertDialog: AlertDialog? = null

    val apiKey = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startService.setOnClickListener { startWebServer() }
        binding.stopService.setOnClickListener { stopWebServer() }


        // 获取当前设备的 IP 地址
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress

        // 使用位移操作分离 IP 地址的各个字节
        val i1 = ip and 0xff
        val i2 = (ip ushr 8) and 0xff
        val i3 = (ip ushr 16) and 0xff
        val i4 = (ip ushr 24) and 0xff

        // 格式化并打印 IP 地址
        val ipAddress = "$i1.$i2.$i3.$i4"
        binding.ipTextView.text="$ipAddress:8080"

        // 长按复制 IP 地址和 URL 到粘贴板
        binding.ipTextView.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val textToCopy = "$ipAddress:8080/?key=$apiKey"
            val clip = ClipData.newPlainText("IP Address", textToCopy)
            clipboard.setPrimaryClip(clip)

            // 显示提示消息（可选）
            Toast.makeText(this, "IP 地址和 URL 已复制到剪贴板", Toast.LENGTH_SHORT).show()

            // 检查服务是否已经启动，若没有启动则先启动
            if (!isMockLocationServiceRunning()) {
                startWebServer()  // 启动服务
            }

            // 打开浏览器
            val url = "http://$ipAddress:8080/?key=$apiKey"  // 拼接 URL
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)  // 启动浏览器打开 URL
        }


        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                onCheckLocationPermission(isGranted)
            }
    }


    override fun onResume() {
        super.onResume()

        alertDialog?.let {
            it.dismiss()
            alertDialog = null
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            onCheckLocationPermission(true)
        } else {
            onCheckLocationPermission(false)
        }
    }

    private fun onCheckLocationPermission(isGranted: Boolean) {
        if (isGranted) {
            // 現状、とくにすることがない
            return
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // パーミッションの許可の際に "今後、表示しない" にチェックを付けた場合
            val alert = AlertDialog.Builder(this)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                .create()
            alert.show()

            alertDialog = alert
            return
        }

        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }


    private fun startWebServer() {
        val i = Intent(this, MockLocationService::class.java)
        startService(i)
    }


    private fun stopWebServer() {
        val i = Intent(this, MockLocationService::class.java)
        stopService(i)
    }

    /**
     * 检查 MockLocationService 是否正在运行
     */
    private fun isMockLocationServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (MockLocationService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

}
