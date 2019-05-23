package com.example.shop.components

import android.content.Context
import android.support.v4.widget.SwipeRefreshLayout
import android.util.AttributeSet
import android.view.View

/**
 * [SwipeRefreshLayout] для исправления блокировки прокрутки вверх вложенной скролящейся вьюшки
 * Для правильной работы следует указать скролящуюся вьюшку в [childView]
 */
class CustomSwipeRefreshLayout : SwipeRefreshLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    var childView: View? = null

    override fun canChildScrollUp(): Boolean =
            childView?.canScrollVertically(-1) ?: super.canChildScrollUp()
}