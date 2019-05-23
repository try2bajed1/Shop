package com.example.shop.components

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.example.shop.R

class VatSelector(context: Context,attrs: AttributeSet? = null) : LinearLayout(context, attrs), View.OnClickListener {
    private var value: Int? = null

    init {
        View.inflate(getContext(), R.layout.view_vat_selector, this)
        vat_none.setOnClickListener(this)
        vat_0.setOnClickListener(this)
        vat_10.setOnClickListener(this)
        vat_20.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val newValue = when (v.id) {
            R.id.vat_none -> VAlUE_NONE
            R.id.vat_0 -> VALUE_0
            R.id.vat_10 -> VALUE_10
            R.id.vat_20 -> VALUE_20
            else -> this.value
        }

        setValue(newValue)
    }

    fun setValue(value: Int?){
        val oldBtn = findViewById<Button>(getIdOf(this.value))
        oldBtn.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        oldBtn.setTextColor(ContextCompat.getColor(context, R.color.black))

        this.value = value

        val newBtn = findViewById<Button>(getIdOf(this.value))
        newBtn.setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
        newBtn.setTextColor(ContextCompat.getColor(context, R.color.white))

    }

    fun getValue(): Int? {
        return value
    }

    private fun getIdOf(value: Int?): Int {
        return when(value){
            VALUE_0 -> R.id.vat_0
            VALUE_10 -> R.id.vat_10
            VALUE_20 -> R.id.vat_20
            else -> R.id.vat_none
        }
    }

    companion object {
        val VAlUE_NONE: Int? = null
        const val VALUE_0 = 0
        const val VALUE_10 = 10
        const val VALUE_20 = 20
    }
}