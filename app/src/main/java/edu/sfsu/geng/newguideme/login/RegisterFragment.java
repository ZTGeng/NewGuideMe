package edu.sfsu.geng.newguideme.login;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.Role;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;
import edu.sfsu.geng.newguideme.utils.ErrorCleanTextWatcher;

/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {

    private static final String TAG = "registerFragment";

    private AppCompatEditText emailEditText, usernameEditText, passwordEditText;
    private TextInputLayout emailInputLayout, usernameInputLayout, passwordInputLayout;

    private LoginListener loginListener;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        emailInputLayout = (TextInputLayout) view.findViewById(R.id.register_email_inputlayout);
        emailEditText = (AppCompatEditText) view.findViewById(R.id.register_email_edittext);
        if (emailEditText != null && emailInputLayout != null) {
            emailEditText.addTextChangedListener(new ErrorCleanTextWatcher(emailInputLayout));
        }

        usernameInputLayout = (TextInputLayout) view.findViewById(R.id.register_username_inputlayout);
        usernameEditText = (AppCompatEditText) view.findViewById(R.id.register_username_edittext);
        if (usernameEditText != null && usernameInputLayout != null) {
            usernameEditText.addTextChangedListener(new ErrorCleanTextWatcher(usernameInputLayout));
        }

        passwordInputLayout = (TextInputLayout) view.findViewById(R.id.register_password_inputlayout);
        passwordEditText = (AppCompatEditText) view.findViewById(R.id.register_password_edittext);
        if (passwordEditText != null && passwordInputLayout != null) {
            passwordEditText.addTextChangedListener(new ErrorCleanTextWatcher(passwordInputLayout));
        }

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
        if (registerButtonH != null) {
            registerButtonH.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    register(Role.helper);
                }
            });
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.loginListener = (WelcomeActivity) getActivity();
    }

    private void register(Role role) {
        final String emailText = emailEditText.getText().toString();
        if (emailText.isEmpty()) {
            emailInputLayout.setError(getString(R.string.login_email_empty_error));
            return;
        }

        String usernameText = usernameEditText.getText().toString();
        if (usernameText.isEmpty()) {
            usernameInputLayout.setError(getString(R.string.login_username_empty_error));
            return;
        }

        final String passwordText = passwordEditText.getText().toString();
        if (passwordText.isEmpty()) {
            passwordInputLayout.setError(getString(R.string.login_password_empty_error));
            return;
        }

        ServerApi.register(emailText, usernameText, passwordText, role.name(), new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try{
                    JSONObject json = new JSONObject(data);
                    String message = json.getString("response");

                    if (json.getBoolean("res")) {
                        if (loginListener != null) {
//                            loginListener.login(emailText, passwordText);
                            loginListener.login(json.getString("token"),
                                    json.getString("username"),
                                    json.getString("role"),
                                    json.getString("rate"),
                                    json.getString("invite_code"));
                        } else {
                            Log.d(TAG, "loginListener is empty");
                        }
                    } else {
                        String field = json.getString("field");
                        switch (field) {
                            case "email":
                                emailInputLayout.setError(message);
                                break;
                            case "username":
                                usernameInputLayout.setError(message);
                                break;
                            case "password":
                                passwordInputLayout.setError(message);
                                break;
                            default:
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

}
