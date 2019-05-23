package com.example.shop.utils

import android.hardware.usb.UsbDevice
import com.example.shop.app.AppSingleton

/**
 * Принтеры
 */

fun UsbDevice.isAtol() = isVendorAtol() && isProductAtol()

private fun UsbDevice.isVendorAtol() = vendorId == FptrImpl.VENDOR_ID_1 || vendorId == FptrImpl.VENDOR_ID_2

private fun UsbDevice.isProductAtol() = productId == FptrImpl.PRODUCT_ID

/**
 * Терминалы
 */

fun UsbDevice.isInpas() = isVendorInpas() && isProductInpas()

private fun UsbDevice.isVendorInpas() =
        when (this.vendorId) {
            Inpas.TERMINAL_VENDOR_VERIFONE_ID -> true
            Inpas.TERMINAL_VENDOR_PAX_ID -> true
            else -> false
        }

private fun UsbDevice.isProductInpas() = isProductInpasVerifone520C() || isProductInpasVerifone820() || isProductInpasPaxD210()

fun UsbDevice.isProductInpasVerifone520C() = this.productId == Inpas.TERMINAL_VERIFONE_520C_PRODUCT_ID

fun UsbDevice.isProductInpasVerifone820() = this.productId == Inpas.TERMINAL_VERIFONE_820_PRODUCT_ID

fun UsbDevice.isProductInpasPaxD210() = this.productId == Inpas.TERMINAL_PAX_D210_PRODUCT_ID

fun UsbDevice.isIngenico() = isVendorIngenico() && isProductIngenico()

private fun UsbDevice.isVendorIngenico() =
        when (this.vendorId) {
            Ingenico.TERMINAL_VENDOR_ID -> true
            else -> false
        }

private fun UsbDevice.isProductIngenico() =
        when (this.productId) {
            Ingenico.TERMINAL_320_PRODUCT_ID -> true
            else -> false
        }

/**
 * Весы
 */
fun UsbDevice.isMassaK() = isVendorMassaK() && isProductMassaK()

private fun UsbDevice.isVendorMassaK() =
        when (this.vendorId) {
            AppSingleton.MASSA_K_VENDOR_ID -> true
            else -> false
        }

private fun UsbDevice.isProductMassaK() =
        when (this.productId) {
            AppSingleton.MASSA_K_PRODUCT_ID -> true
            else -> false
        }


fun UsbDevice.isScanner() = isHoneywellScanner() || isViotehScanner()

private fun UsbDevice.isViotehScanner() = isVendorViotehScanner() && isProductViotehScanner()

private fun UsbDevice.isHoneywellScanner() = isVendorHoneywellScanner() && isProductHoneywellScanner()

private fun UsbDevice.isVendorHoneywellScanner() = vendorId == AppSingleton.SCANNER_HONEYWELL_VENDOR_ID_1 || vendorId == AppSingleton.SCANNER_HONEYWELL_VENDOR_ID_2

private fun UsbDevice.isProductHoneywellScanner() = isHoneywellYjHH360Scanner() || isHoneyWellEclipseScanner()

private fun UsbDevice.isHoneyWellEclipseScanner() = productId == AppSingleton.SCANNER_HONEYWELL_ECLIPSE_PRODUCT_ID

private fun UsbDevice.isHoneywellYjHH360Scanner() = productId == AppSingleton.SCANNER_HONEYWELL_YJ_HH360_PRODUCT_ID

private fun UsbDevice.isVendorViotehScanner() = vendorId == AppSingleton.SCANNER_VIOTEH_VENDOR_ID

private fun UsbDevice.isProductViotehScanner() = isProductViotehVT1110Scanner()

private fun UsbDevice.isProductViotehVT1110Scanner() = productId == AppSingleton.SCANNER_VIOTEH_VT_1110_PRODUCT_ID

