package com.example.shop

import android.text.Editable
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class PhoneNumberWatcherTest {

    @Test
    fun testApplyFormat() {

        this.testFormat("", "+7")
        this.testFormat("+", "+7")
        this.testFormat("+7", "+7")
        this.testFormat("+79", "+7 (9")
        this.testFormat("+7999", "+7 (999")
        this.testFormat("+79999", "+7 (999) 9")
        this.testFormat("+7999999", "+7 (999) 999")
        this.testFormat("+79999999", "+7 (999) 999 - 9")
        this.testFormat("+799999999", "+7 (999) 999 - 99")
        this.testFormat("+7999999999", "+7 (999) 999 - 99 - 9")
        this.testFormat("+79999999999", "+7 (999) 999 - 99 - 99")
        this.testFormat("+799999999999", "+7 (999) 999 - 99 - 99")
        this.testFormat("+799999999999999999", "+7 (999) 999 - 99 - 99")
        this.testFormat("kjh23y4gh", "+7 (234")
    }

    private fun testFormat(from: String, to: String) {
        val watcher = PhoneNumberWatcher(PhoneNumberWatcher.RUS_FORMAT)
        val editable = Editable.Factory.getInstance().newEditable(from)
        watcher.applyFormat(editable, PhoneNumberWatcher.RUS_FORMAT)
        assert(editable.toList() == to.toList())
    }
}
