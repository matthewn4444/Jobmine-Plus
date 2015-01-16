package com.jobmineplus.mobile.widgets;

import java.util.Locale;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.jobmineplus.mobile.R;

public class TextViewPlus extends TextView {
    private boolean isUpperCase;
    private CharSequence originalText;

    public TextViewPlus(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Typeface.createFromAsset doesn't work in the layout editor. Skipping...
        if (isInEditMode()) {
            return;
        }
        TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.TextViewPlus);
        isUpperCase = (styledAttrs != null && styledAttrs.getBoolean(R.styleable.TextViewPlus_uppercase, false));
        styledAttrs.recycle();
        setText(getText());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (isUpperCase) {
            originalText = text;
            text = text.toString().toUpperCase(Locale.getDefault());
        }
        super.setText(text, type);
    }

    @Override
    public CharSequence getText() {
        if (originalText == null) {
            return super.getText();
        }
        return originalText;
    }
}
