package com.example.shop.components

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.shop.R

/**
 * Компонент для ввода цифрового пароля
 */
class PasswordView : LinearLayout {

    companion object {
        private const val EMPTY_STRING = ""
        private const val COMMA = ","

        private const val DEFAULT_LENGTH_VALUE = 0
        private const val DEFAULT_CIRCLE_COLOR_VALUE = Color.BLACK
        private const val DEFAULT_SYMBOL_COLOR_VALUE = Color.BLACK
        private const val DEFAULT_MARGIN_BETWEEN_SYMBOLS_VALUE = 0
        private const val DEFAULT_CIRCLE_SIZE_VALUE = 10
        private const val DEFAULT_SYMBOL_SIZE_VALUE = 16
        private const val DEFAULT_DIVIDE_PARTS_VALUE = ""
        private const val DEFAULT_DIVIDER_STRING_VALUE = ""
    }

    //Список view для пустых символов пароля
    private lateinit var circlesList: List<ImageView>
    //Список view для не пустых символов пароля
    private lateinit var symbolsList: List<TextView>
    //Длина пароля
    var passwordLength: Int = 0
        set(value) {
            field = value
            this.init()
        }
    //Размер шрифта не пустого символа
    private var symbolSize: Int = 0
    //Цвет не пустого символа
    private var symbolsColor: Int = 0
    //Размер пустого символа
    private var circleSize: Int = 0
    //Цвет пустого символа
    private var circleColor: Int = 0
    //Расстояние между символами
    private var marginBetweenSymbols: Int = 0
    //Части, на которые нужно поделить разделителями пароль
    var divideParts: String = EMPTY_STRING
        set(value) {
            field = value
            this.init()
        }
    //Строка, которую нужно показывать в качестве разделителя
    private var dividerString: String = EMPTY_STRING
    //divideParts в виде списка
    private var dividersAmounts: List<Int> = listOf()
    //Текущий пароль
    var password: String = EMPTY_STRING
        set(value) {
            field = value
            this.applyPassword()
        }

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        this.initAttrs(attributeSet)
        this.init()
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {
        this.initAttrs(attributeSet)
        this.init()
    }

    /**
     * Заполняет поля атрибутов переданными значениями
     */
    private fun initAttrs(attributeSet: AttributeSet) {
        val attrs = context.obtainStyledAttributes(attributeSet, R.styleable.PasswordView)
        passwordLength = attrs.getInteger(R.styleable.PasswordView_passwordLength, DEFAULT_LENGTH_VALUE)
        circleColor = attrs.getColor(R.styleable.PasswordView_circleColor, DEFAULT_CIRCLE_COLOR_VALUE)
        symbolsColor = attrs.getColor(R.styleable.PasswordView_symbolsColor, DEFAULT_SYMBOL_COLOR_VALUE)
        marginBetweenSymbols = attrs.getDimensionPixelSize(R.styleable.PasswordView_marginBetweenSymbols, DEFAULT_MARGIN_BETWEEN_SYMBOLS_VALUE)
        circleSize = attrs.getDimensionPixelSize(R.styleable.PasswordView_circleSize, DEFAULT_CIRCLE_SIZE_VALUE)
        symbolSize = attrs.getDimensionPixelSize(R.styleable.PasswordView_symbolSize, DEFAULT_SYMBOL_SIZE_VALUE)
        divideParts = attrs.getString(R.styleable.PasswordView_divideParts) ?: DEFAULT_DIVIDE_PARTS_VALUE
        dividerString = attrs.getString(R.styleable.PasswordView_dividerString) ?: DEFAULT_DIVIDER_STRING_VALUE
        attrs.recycle()
    }

    /**
     * Заполняет [PasswordView]
     * Требуется вызывать при создании [PasswordView]
     */
    private fun init() {
        this.fillWithSymbols()
        this.fillWithCircles()
        this.initDividesInfo()

        this.initViews()

        this.applyPassword()
    }

    /**
     * Заново заполняет [PasswordView]
     * Требуется вызывать каждый раз при смене [divideParts]
     * Не требуется вызывать при смене пароля, всю работу для этого случая выполняет [applyPassword]
     */
    private fun initViews() {
        this.removeAllViews()

        var divided = 0
        var dividedAmount = 0
        for (i in 0 until passwordLength) {
            if (this.dividersAmounts.getOrNull(dividedAmount) == this.childCount - divided) {
                this.addView(this.getDivider(false))
                divided = this.childCount
                dividedAmount++
            }
            this.addView(RelativeLayout(context).apply {
                this.layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    if (i != passwordLength - 1)
                        this.setMargins(0, 0, marginBetweenSymbols, 0)
                }
                this.addView(symbolsList[i])
                this.addView(circlesList[i])
            })

        }
        if (this.dividersAmounts.lastOrNull() == 0)
            this.addView(this.getDivider(true))
    }

    /**
     * Возвращает view разделителя
     * @param last - true, если разделитель - последний элемент
     */
    private fun getDivider(last: Boolean) = TextView(context).apply {
        this.layoutParams = LinearLayout.LayoutParams(circleSize, LayoutParams.WRAP_CONTENT).apply {
            if (!last)
                this.setMargins(0, 0, marginBetweenSymbols, 0)
            else
                this.setMargins(marginBetweenSymbols, 0, 0, 0)
        }
        this.setTextColor(symbolsColor)
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, symbolSize.toFloat())
        this.text = dividerString
    }

    /**
     * Конвертирует [divideParts] в список чисел
     * @throws Exception если формат [divideParts] неверный
     */
    private fun initDividesInfo() {
        this.dividersAmounts = mutableListOf<Int>().apply {
            if (!TextUtils.isEmpty(divideParts)) {
                try {
                    divideParts.split(COMMA).forEach { this.add(it.toInt()) }

                    if (this.sum() != passwordLength)
                        throw Exception()

                } catch (exception: Exception) {
                    throw Exception("Wrong dividersAfter format")
                }
            }
        }
    }

    /**
     * Генерирует список view для не пустых символов
     * Длина списка равна длине пароля
     */
    private fun fillWithSymbols() {
        this.symbolsList = List(passwordLength) {
            TextView(context).apply {
                this.layoutParams = RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    this.addRule(RelativeLayout.CENTER_IN_PARENT)
                }
                this.setTextColor(symbolsColor)
                this.text = password.getOrNull(it)?.toString()
                this.setTextSize(TypedValue.COMPLEX_UNIT_PX, symbolSize.toFloat())
                this.typeface = Typeface.create("monospace", Typeface.NORMAL)
            }
        }
    }

    /**
     * Генерирует список view для пустых символов
     * Длина списка равна длине пароля
     */
    private fun fillWithCircles() {
        this.circlesList = List(passwordLength) {
            ImageView(context).apply {
                this.layoutParams = RelativeLayout.LayoutParams(circleSize, circleSize).apply {
                    this.addRule(RelativeLayout.CENTER_IN_PARENT)
                }
                this.setColorFilter(circleColor, PorterDuff.Mode.SRC_ATOP)
                this.setImageResource(R.drawable.empty_circle)
            }
        }
    }

    /**
     * Устанавливает введённый пароль во view
     */
    private fun applyPassword() {
        this.symbolsList.forEachIndexed { index, textView -> textView.text = password.getOrNull(index)?.toString() ?: " " }
        for (i in 0 until passwordLength) {
            this.circlesList[i].visibility = if (this.password.getOrNull(i) == null) View.VISIBLE else View.INVISIBLE
            this.symbolsList[i].visibility = if (this.password.getOrNull(i) == null) View.INVISIBLE else View.VISIBLE
        }
    }

    override fun invalidate() {
        super.invalidate()
        this.init()
    }
}