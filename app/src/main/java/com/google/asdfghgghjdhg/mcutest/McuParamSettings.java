package com.google.asdfghgghjdhg.mcutest;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class McuParamSettings extends McuParamView implements View.OnClickListener {
    private OnClickListener decOnClickListener = null;
    private OnClickListener incOnClickListener = null;

    public McuParamSettings(Context context) {
        super(context);
    }
    public McuParamSettings(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public McuParamSettings(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void init(AttributeSet attrs, int defStyle) {
        super.init(attrs, defStyle);

        this.removeAllViews();
        inflate(getContext(), R.layout.mcu_param_settings_layout, this);
        TextView tv = findViewById(R.id.paramName);
        tv.setText(String.format("%s:", getParamName()));
        tv = findViewById(R.id.paramValue);
        tv.setText(getParamValue());

        findViewById(R.id.btnDec).setOnClickListener(this);
        findViewById(R.id.btnInc).setOnClickListener(this);
    }

    public void onClick(View view) {
        View btnDec = findViewById(R.id.btnDec);
        if (view == btnDec) {
            if (decOnClickListener != null) decOnClickListener.onClick(this);
        }
        View btnInc = findViewById(R.id.btnInc);
        if (view == btnInc) {
            if (incOnClickListener != null) incOnClickListener.onClick(this);
        }
    }

    public void setDecOnClickListener(OnClickListener value) {
        decOnClickListener = value;
    }
    public void setIncOnClickListener(OnClickListener value) {
        incOnClickListener = value;
    }
}
