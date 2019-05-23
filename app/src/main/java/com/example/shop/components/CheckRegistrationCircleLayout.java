package com.example.shop.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import com.example.shop.R;

import java.util.ArrayList;



public class CheckRegistrationCircleLayout extends LinearLayout {

    public static final Integer MAX_PIN_SIZE = 6;

    private ArrayList<View> roundsArr = new ArrayList<>();

    private Context context;

    public CheckRegistrationCircleLayout(Context context) {
        super(context);
        this.context = context;

    }

    public CheckRegistrationCircleLayout(Context context, AttributeSet set) {
        super(context, set);
        this.context = context;
        final TypedArray attrs = getContext ( ).obtainStyledAttributes ( set, R.styleable.CheckRegistrationCircleLayout );
        init(attrs.getDimensionPixelSize ( R.styleable.CheckRegistrationCircleLayout_circle_size, -1 ),
                attrs.getDimensionPixelSize ( R.styleable.CheckRegistrationCircleLayout_circle_padding, -1 ));

    }

    public CheckRegistrationCircleLayout(Context context, AttributeSet set, int defStyleAttr) {
        super(context, set, defStyleAttr);
        this.context = context;
        final TypedArray attrs = getContext ( ).obtainStyledAttributes ( set, R.styleable.CheckRegistrationCircleLayout );
        init(attrs.getDimensionPixelSize ( R.styleable.CheckRegistrationCircleLayout_circle_size, -1 ),
                attrs.getDimensionPixelSize ( R.styleable.CheckRegistrationCircleLayout_circle_padding, -1 ));
    }

    private void init(final int circleSize, final int circlePadding) {

        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 55, getResources().getDisplayMetrics());

        int dps = circleSize;
        final float scale = getContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (dps * scale * 0.9 + 0.5f);

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
            this.addView(singlePinCharView);
            roundsArr.add(singlePinCharView);
        }
    }

    //заливка перерисовывается в селекторе при дизабле
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