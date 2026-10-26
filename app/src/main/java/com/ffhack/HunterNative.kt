package com.ffhack

object HunterNative {
    init { System.loadLibrary("ffhack") }

    external fun nInit()
    external fun nConfirmCode(key: String): Int
    external fun nGetKey(): String
    external fun nSign(key: String): String
    external fun nVersion(): String
    external fun nTelegram(): String
    external fun nYoutube(): String

    external fun nInitOverlay(w: Int, h: Int)
    external fun nDraw(width: Int, height: Int,
                       box: IntArray, line: FloatArray, target: FloatArray,
                       dist: FloatArray, hp: FloatArray): Int

    external fun nGetControlCount(): Int
    external fun nGetControlLabel(i: Int): String
    external fun nGetControlMode(i: Int): Int
    external fun nGetSliderMin(i: Int): Int
    external fun nGetSliderMax(i: Int): Int
    external fun nGetSliderDefault(i: Int): Int
    external fun nGetSliderUnit(i: Int): String
    external fun nGetStatus(i: Int): Boolean
    external fun nSendToggle(i: Int, on: Boolean)
    external fun nSendSlider(i: Int, value: Int)
    external fun nSetLicense(key: String)
}
