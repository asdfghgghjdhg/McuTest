package com.google.asdfghgghjdhg.mcutest;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

public class McuParamView extends FrameLayout {
    private String mParamName = getResources().getString(R.string.defaultParamName);
    private String mParamValue = getResources().getString(R.string.defaultParamValue);

    public McuParamView(Context context) {
        super(context);
        init(null, 0);
    }

    public McuParamView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public McuParamView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    protected void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.McuParamView, defStyle, 0);

        mParamName = a.getString(R.styleable.McuParamView_paramName);
        mParamValue = a.getString(R.styleable.McuParamView_paramValue);

        a.recycle();

        inflate(getContext(), R.layout.mcu_param_view, this);
        TextView tv = findViewById(R.id.paramName);
        tv.setText(String.format("%s:", mParamName));
        tv = findViewById(R.id.paramValue);
        tv.setText(mParamValue);
    }

    public String getParamName() { return mParamName; }
    public void  setParamName(String paramName) {
        mParamName = paramName;

        TextView tv = findViewById(R.id.paramName);
        tv.setText(String.format("%s:", mParamName));
    }
    public String getParamValue() { return mParamValue; }
    public void  setParamValue(String paramValue) {
        mParamValue = paramValue;

        TextView tv = findViewById(R.id.paramValue);
        tv.setText(mParamValue);
    }
}
