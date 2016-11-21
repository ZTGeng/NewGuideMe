package edu.sfsu.geng.newguideme.login;


import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.Role;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;

import static edu.sfsu.geng.newguideme.Config.PASSWORDLIMIT;

/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {

    private AppCompatEditText emailEditText, usernameEditText, passwordEditText, confirmPasswordEditText;
    private TextInputLayout emailInputLayout, usernameInputLayout, passwordInputLayout, confirmPasswordInputLayout;

    private LoginListener loginListener;

    public RegisterFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        emailEditText = (AppCompatEditText) view.findViewById(R.id.register_email_edittext);
        emailInputLayout = (TextInputLayout) view.findViewById(R.id.register_email_inputlayout);
        usernameEditText = (AppCompatEditText) view.findViewById(R.id.register_username_edittext);
        usernameInputLayout = (TextInputLayout) view.findViewById(R.id.register_username_inputlayout);
        passwordEditText = (AppCompatEditText) view.findViewById(R.id.register_password_edittext);
        passwordInputLayout = (TextInputLayout) view.findViewById(R.id.register_password_inputlayout);
        confirmPasswordEditText = (AppCompatEditText) view.findViewById(R.id.register_confirm_password_edittext);
        confirmPasswordInputLayout = (TextInputLayout) view.findViewById(R.id.register_confirm_password_inputlayout);

        AppCompatButton registerButtonV = (AppCompatButton) view.findViewById(R.id.register_button_v);
        if (registerButtonV != null) {
            registerButtonV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    register(Role.blind);
                }
            });
        }

        AppCompatButton registerButtonH = (AppCompatButton) view.findViewById(R.id.register_button_h);
        if (registerButtonV != null) {
            registerButtonV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    register(Role.helper);
                }
            });
        }

        return view;
    }

    void setListener(LoginListener listener) {
        this.loginListener = listener;
    }

    private void register(Role role) {
        if (!isValidEmail(emailEditText.getText())) {
            emailInputLayout.setError(getString(R.string.login_email_invalid_error));
            return;
        }
        final String emailText = emailEditText.getText().toString();

        String usernameText = usernameEditText.getText().toString();
        if (usernameText.isEmpty()) {
            usernameInputLayout.setError(getString(R.string.login_username_empty_error));
            return;
        }

        if (!isValidPassword(passwordEditText.getText())) {
            passwordInputLayout.setError(String.format(Locale.getDefault(),
                    getString(R.string.login_password_short_error), PASSWORDLIMIT));
            return;
        }
        final String passwordText = passwordEditText.getText().toString();

        if (!passwordText.equals(confirmPasswordEditText.getText().toString())) {
            confirmPasswordInputLayout.setError(getString(R.string.login_password_not_match_error));
            return;
        }

        ServerApi.register(emailText, usernameText, passwordText, role.name(), new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try{
                    JSONObject json = new JSONObject(data);
                    String jsonStr = json.getString("response");

                    if (json.getBoolean("res")) {
                        loginListener.login(emailText, passwordText);
                    } else {
                        Toast.makeText(getContext(), jsonStr, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    private boolean isValidPassword(CharSequence target) {
        return !TextUtils.isEmpty(target) && target.length() > PASSWORDLIMIT;
    }

}
