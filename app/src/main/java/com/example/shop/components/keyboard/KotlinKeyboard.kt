package com.example.shop.components.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.example.shop.R
import io.reactivex.subjects.PublishSubject
import com.example.shop.components.keyboard.interactors.BackSpace
import com.example.shop.components.keyboard.interactors.Coma
import com.example.shop.components.keyboard.interactors.KBType
import com.example.shop.components.keyboard.interactors.Num

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 09.06.18
 * Time: 15:17
 */
class KotlinKeyboard @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener {

    val subj: PublishSubject<KBType> = PublishSubject.create<KBType>()

    init {
        View.inflate(getContext(), R.layout.view_rx_num_keyboard, this)
        key_0.setOnClickListener(this)
        key_1.setOnClickListener(this)
        key_2.setOnClickListener(this)
        key_3.setOnClickListener(this)
        key_4.setOnClickListener(this)
        key_5.setOnClickListener(this)
        key_6.setOnClickListener(this)
        key_7.setOnClickListener(this)
        key_8.setOnClickListener(this)
        key_9.setOnClickListener(this)
        key_point.setOnClickListener(this)
        key_backspace.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.key_0 -> dispatchEvent(Num("0"))
            R.id.key_1 -> dispatchEvent(Num("1"))
            R.id.key_2 -> dispatchEvent(Num("2"))
            R.id.key_3 -> dispatchEvent(Num("3"))
            R.id.key_4 -> dispatchEvent(Num("4"))
            R.id.key_5 -> dispatchEvent(Num("5"))
            R.id.key_6 -> dispatchEvent(Num("6"))
            R.id.key_7 -> dispatchEvent(Num("7"))
            R.id.key_8 -> dispatchEvent(Num("8"))
            R.id.key_9 -> dispatchEvent(Num("9"))
            R.id.key_point -> dispatchEvent(Coma())
            R.id.key_backspace -> dispatchEvent(BackSpace())
        }
    }
    
    private fun dispatchEvent(type: KBType) {
        subj.onNext(type)
    }

    fun setPointBtnVisibility(b: Boolean) {
        key_point.visibility = if (b) View.VISIBLE else View.INVISIBLE
    }
}