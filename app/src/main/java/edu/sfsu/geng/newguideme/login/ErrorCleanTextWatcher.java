package edu.sfsu.geng.newguideme.login;

import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;

/**
 * Created by gengz on 12/6/16.
 */

public class ErrorCleanTextWatcher implements TextWatcher {

    @NonNull private TextInputLayout textInputLayout;

    public ErrorCleanTextWatcher(@NonNull TextInputLayout textInputLayout) {
        this.textInputLayout = textInputLayout;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        textInputLayout.setError("");
    }

    @Override
    public void afterTextChanged(Editable s) {}
}
