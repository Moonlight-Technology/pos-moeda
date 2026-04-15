package com.firhat.webviewapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.OutputStream
import java.util.UUID

class PrinterHelper(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val handler = Handler(Looper.getMainLooper())

    // 🔥 LOCK PRINTER - Support multiple printers
    private val printerNames = listOf(
        "58 Printer",
        "58",
        "58Printer",
        "RPP-02",
        "RPP-01",
        "RP-01",
        "RP-02",
        "TP-01",
        "PRJ-58",
        "ZJ-5802",        // 🔥 contoh Zjiang
        "ZJ-5890",
        "BlueTooth Printer",
        "BT-Printer"
    )

    // ==========================
    // CONNECT
    // ==========================
    private fun connect(): Boolean {
        return try {
            // Check Bluetooth availability
            if (bluetoothAdapter == null) {
                show("❌ Bluetooth tidak tersedia di device ini")
                return false
            }

            if (!bluetoothAdapter!!.isEnabled) {
                show("❌ Bluetooth tidak diaktifkan")
                return false
            }

            val device = getPrinterDevice()

            if (device == null) {
                show("❌ Printer tidak ditemukan: ${printerNames.joinToString(", ")}")
                show("💡 Pastikan printer sudah dipairing dan Bluetooth aktif")
                return false
            }

            bluetoothAdapter.cancelDiscovery()

            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()

            outputStream = socket?.outputStream

            show("✅ Connected: ${device.name}")
            true
        } catch (e: SecurityException) {
            show("❌ Permission Bluetooth ditolak. Mohon izinkan akses Bluetooth")
            false
        } catch (e: java.io.IOException) {
            show("❌ Gagal koneksi ke printer. Pastikan printer aktif dan dalam jangkauan")
            false
        } catch (e: Exception) {
            show("❌ Error: ${e.message}")
            false
        }
    }

    // ==========================
    // PRINT AUTO RETRY
    // ==========================
    fun print(text: String) {
        Thread {
            var attempt = 0
            val maxRetry = 3

            while (attempt < maxRetry) {
                try {
                    if (outputStream == null) {
                        val connected = connect()
                        if (!connected) return@Thread
                    }

                    outputStream?.write(text.toByteArray(Charsets.UTF_8))
                    outputStream?.flush()

                    show("🖨️ Print sukses")
                    return@Thread

                } catch (e: Exception) {
                    e.printStackTrace()

                    attempt++
                    show("⚠️ Retry ($attempt/$maxRetry)")

                    reconnect()
                    Thread.sleep(1000)
                }
            }

            show("❌ Print gagal")
        }.start()
    }

    // ==========================
    // RECONNECT
    // ==========================
    private fun reconnect() {
        try {
            close()
            Thread.sleep(500)
            connect()
        } catch (_: Exception) {}
    }

    // ==========================
    // GET DEVICE (MULTIPLE PRINTERS)
    // ==========================
    private fun getPrinterDevice(): BluetoothDevice? {
        val devices = bluetoothAdapter?.bondedDevices ?: return null

        for (device in devices) {
            if (device.name in printerNames) {
                return device
            }
        }

        return null
    }

    // ==========================
    // CLOSE
    // ==========================
    private fun close() {
        try {
            outputStream?.close()
            socket?.close()
            outputStream = null
            socket = null
        } catch (_: Exception) {}
    }

    private fun show(msg: String) {
        handler.post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}