package com.example.shop.utils

import android.view.View
import android.widget.EditText
import android.widget.TextView

var View.isVisibleOrGone: Boolean
    inline get() = visibility == View.VISIBLE
    inline set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

var View.isVisibleOrInvisible: Boolean
    inline get() = visibility == View.VISIBLE
    inline set(value) {
        visibility = if (value) View.VISIBLE else View.INVISIBLE
    }

var TextView.textOrGone: CharSequence?
    get() = text
    set(value) {
        text = value
        isVisibleOrGone = !value.isNullOrEmpty()
    }

var EditText.textOrGone: CharSequence?
    get() = text
    set(value) {
        setText(value)
        isVisibleOrGone = !value.isNullOrBlank()
    }