package com.example.punchthroughbleguide

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.punchthroughbleguide.R.*

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val RUNTIME_PERMISSION_REQUEST_CODE = 2

private const val TAG = "BLEComponent"

class MainActivity : AppCompatActivity() {
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)

        val scanButton = findViewById<Button>(R.id.scanButton)
        scanButton.setOnClickListener { startBleScan() }
    }

    override fun onResume() {
        super.onResume()

        //check prompt for bluetooth to be enabled
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    /**
     * startBleScan
     *
     */
    private fun startBleScan() {
        if (!hasRequiredRuntimePermissions()) {
            Log.i(TAG,"Starting BLE Scan is missing permissions")

            requestRelevantRuntimePermissions()
        } else {
            Log.i(TAG,"Starting BLE Scan")
            /* TODO: Actually perform scan */
        }
    }

    /***
     * Request runtime permissions
     *
     */
    private fun Activity.requestRelevantRuntimePermissions() {
        if (hasRequiredRuntimePermissions()) {
            return
        }
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                requestLocationPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestBluetoothPermissions()
            }
        }
    }

    private fun requestLocationPermission() {

        val builder = AlertDialog.Builder(this)
        with(builder) {
            setTitle("Location permission required")
            setMessage(
                "Starting from Android M (6.0), the system requires apps to be granted location access in order to scan for BLE devices."
            )
            setCancelable(false)
            setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { dialog, id ->
                    activityCompatReuqestPermissions(arrayOf(ACCESS_FINE_LOCATION))
                })
            show()
        }
    }

    private fun activityCompatReuqestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            RUNTIME_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestBluetoothPermissions() {
        val builder = AlertDialog.Builder(this)
        with(builder) {
            setTitle("Bluetooth permissions required")
            setMessage(
                "Starting from Android 12, the system requires apps to be granted Bluetooth access in order to scan for and connect to BLE devices."
            )
            setCancelable(false)
            setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, id ->
                activityCompatReuqestPermissions(arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT))
            })
            show()
        }
    }
    /***
     * Handle the request permission results
     *
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.i(TAG,"onRequestPermissionsResult handling")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RUNTIME_PERMISSION_REQUEST_CODE -> {
                val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
                    it.second == PackageManager.PERMISSION_DENIED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
                }
                val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                when {
                    containsPermanentDenial -> {
                        // TODO: Handle permanent denial (e.g., show AlertDialog with justification)
                        // Note: The user will need to navigate to App Settings and manually grant
                        // permissions that were permanently denied
                    }
                    containsDenial -> {
                        requestRelevantRuntimePermissions()
                    }
                    allGranted && hasRequiredRuntimePermissions() -> {
                        startBleScan()
                    }
                    else -> {
                        // Unexpected scenario encountered when handling permissions
                        recreate()
                    }
                }
            }
        }
    }

    /***
     *
     * prompt for enable bluetooth and react to it etc.
     */

    @SuppressLint("MissingPermission")
    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    //do something with the results from the prompt about enable BT
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }


    /****
     * extension methods to context
     * check for run time permissions
     */
    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun Context.hasRequiredRuntimePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(BLUETOOTH_SCAN) &&
                    hasPermission(BLUETOOTH_CONNECT)
        } else {
            hasPermission(ACCESS_FINE_LOCATION)
        }
    }
}