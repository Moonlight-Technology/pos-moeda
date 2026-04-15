package com.firhat.webviewapp

import android.content.Context
import android.webkit.JavascriptInterface

class WebAppInterface(
    private val context: Context,
    private val printerHelper: PrinterHelper
) {

    @JavascriptInterface
    fun print(text: String) {
        printerHelper.print(text)
    }
}