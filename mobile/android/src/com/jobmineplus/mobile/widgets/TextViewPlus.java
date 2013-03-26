package com.jobmineplus.mobile.widgets;

import java.util.Locale;

import com.jobmineplus.mobile.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextViewPlus extends TextView {
    private TypedArray styledAttrs;
    private CharSequence originalText;

    public TextViewPlus(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Typeface.createFromAsset doesn't work in the layout editor. Skipping...
        if (isInEditMode()) {
            return;
        }
        styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.TextViewPlus);
        styledAttrs.recycle();
        setText(getText());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (styledAttrs != null && styledAttrs.getBoolean(R.styleable.TextViewPlus_uppercase, false)) {
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
