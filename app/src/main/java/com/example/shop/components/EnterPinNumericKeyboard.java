package com.example.shop.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import com.example.shop.R;

import java.util.ArrayList;


/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 13.07.15
 * Time: 14:03
 */
public class EnterPinNumericKeyboard extends LinearLayout {

    public static final Integer MAX_PIN_SIZE = 4;

    private ArrayList<View> roundsArr = new ArrayList<>();
    private String enteredPin = "";

    private Context context;

    public EnterPinNumericKeyboard(Context context) {
        super(context);
        this.context = context;

    }

    public EnterPinNumericKeyboard(Context context, AttributeSet set) {
        super(context, set);
        this.context = context;
        final TypedArray attrs = getContext().obtainStyledAttributes(set, R.styleable.CheckRegistrationCircleLayout);
        init(attrs.getDimensionPixelSize(R.styleable.CheckRegistrationCircleLayout_circle_size, -1),
                attrs.getDimensionPixelSize(R.styleable.CheckRegistrationCircleLayout_circle_padding, -1));
    }


    public EnterPinNumericKeyboard(Context context, AttributeSet set, int defStyleAttr) {
        super(context, set, defStyleAttr);
        this.context = context;
        final TypedArray attrs = getContext().obtainStyledAttributes(set, R.styleable.CheckRegistrationCircleLayout);
        init(attrs.getDimensionPixelSize(R.styleable.CheckRegistrationCircleLayout_circle_size, -1),
                attrs.getDimensionPixelSize(R.styleable.CheckRegistrationCircleLayout_circle_padding, -1));
    }


    @SuppressWarnings("unchecked cast")
    public <V extends View> V $(View container, @IdRes int res) {
        return (V) container.findViewById(res);
    }


    private void init(final int circleSize, final int circlePadding) {

        fillIndicator(circleSize, circlePadding);
    }


    private void fillIndicator(final int circleSize, final int circlePadding) {

        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 55, getResources().getDisplayMetrics());

        LinearLayout pinIndicatorLL = findViewById(R.id.pin_indicator);

        int dps = circleSize;
        final float scale = getContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (dps * scale + 0.5f);

        LayoutParams marginLayoutParams = new LayoutParams(pixels, pixels);
        marginLayoutParams.setMargins(0, 0, pixels + circlePadding, 0);

        LayoutParams noneLayoutParams = new LayoutParams(pixels, pixels);
        noneLayoutParams.setMargins(0, 0, 0, 0);

        for (int i = 0; i < MAX_PIN_SIZE; i++) {
            View singlePinCharView = new View(context);
            if (i == MAX_PIN_SIZE - 1) {
                singlePinCharView.setLayoutParams(noneLayoutParams);
            } else {
                singlePinCharView.setLayoutParams(marginLayoutParams);
            }

            singlePinCharView.setBackgroundResource(R.drawable.pin_circle);
            pinIndicatorLL.addView(singlePinCharView);
            roundsArr.add(singlePinCharView);
        }

        updateUI();
    }

    //заливка перерисовывается в селекторе при дизабле
    private void updateUI() {
        if (enteredPin.isEmpty()) {
            for (int i = 0; i < roundsArr.size(); i++) {
                roundsArr.get(i).setEnabled(false);
            }
        } else {
            for (int i = 0; i < MAX_PIN_SIZE; i++) {
                roundsArr.get(i).setEnabled(i < enteredPin.length());
            }
        }
    }

    public void reset() {
        enteredPin = "";
        updateUI();
    }

    public void updateUI(String enteredPin) {
        if (enteredPin.isEmpty()) {
            for (int i = 0; i < roundsArr.size(); i++) {
                roundsArr.get(i).setEnabled(false);
            }
        } else {
            for (int i = 0; i < MAX_PIN_SIZE; i++) {
                roundsArr.get(i).setEnabled(i < enteredPin.length());
            }
        }
    }
}
