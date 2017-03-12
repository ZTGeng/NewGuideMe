package edu.sfsu.geng.newguideme.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.utils.ErrorCleanTextWatcher;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private AppCompatEditText emailEditText, passwordEditText;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private AppCompatDialog resetDialog;

    private LoginListener loginListener;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        emailInputLayout = (TextInputLayout) view.findViewById(R.id.email_inputlayout);
        emailEditText = (AppCompatEditText) view.findViewById(R.id.email_edittext);
        if (emailEditText != null && emailInputLayout != null) {
            emailEditText.addTextChangedListener(new ErrorCleanTextWatcher(emailInputLayout));
        }

        passwordInputLayout = (TextInputLayout) view.findViewById(R.id.password_inputlayout);
        passwordEditText = (AppCompatEditText) view.findViewById(R.id.password_edittext);
        if (passwordEditText != null && passwordInputLayout != null) {
            passwordEditText.addTextChangedListener(new ErrorCleanTextWatcher(passwordInputLayout));
        }

        AppCompatButton loginButton = (AppCompatButton) view.findViewById(R.id.login_button);
        AppCompatTextView forgotPasswordButton = (AppCompatTextView) view.findViewById(R.id.forgot_password_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailText = emailEditText.getText().toString();
                if (emailText.isEmpty()) {
                    emailInputLayout.setError(getString(R.string.login_email_empty_error));
                }

                String passwordText = passwordEditText.getText().toString();
                if (passwordText.isEmpty()) {
                    passwordInputLayout.setError(getString(R.string.login_password_empty_error));
                    return;
                }

                if (loginListener == null) {
                    Log.d(TAG, "loginListener is empty");
                    return;
                }

                ServerApi.login(emailText, passwordText, new ServerApi.DataListener() {
                    @Override
                    public void onReceiveData(String data) {
                        try{
                            JSONObject json = new JSONObject(data);
                            String message = json.getString("response");

                            if (json.getBoolean("res")) {
                                if (loginListener != null) {
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
        });

        forgotPasswordButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                createResetDialog();
                resetDialog.show();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.loginListener = (WelcomeActivity) getActivity();
    }

    private void createResetDialog() {
        resetDialog = new AppCompatDialog(getContext());
        resetDialog.setTitle(R.string.reset_password_title);
        resetDialog.setContentView(R.layout.reset_pass_init);

        AppCompatButton cancelButton = (AppCompatButton) resetDialog.findViewById(R.id.reset_cancel_button);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    resetDialog.dismiss();
                }
            });
        }

        final AppCompatEditText resetEmailEditText = (AppCompatEditText) resetDialog.findViewById(R.id.reset_email_edittext);
        final TextInputLayout resetEmailInputLayout = (TextInputLayout) resetDialog.findViewById(R.id.reset_email_inputlayout);
        if (resetEmailEditText != null && resetEmailInputLayout != null) {
            resetEmailEditText.addTextChangedListener(new ErrorCleanTextWatcher(resetEmailInputLayout));
        }

        AppCompatButton continueButton = (AppCompatButton) resetDialog.findViewById(R.id.reset_continue_button);
        if (continueButton != null) {
            continueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (resetEmailEditText == null || resetEmailInputLayout == null) {
                        return;
                    }

                    final String email = resetEmailEditText.getText().toString();
                    if (email.isEmpty()) {
                        resetEmailInputLayout.setError(getString(R.string.login_email_empty_error));
                        return;
                    }

                    ServerApi.requestResetCode(email, createRequestCodeDataListener(email, resetEmailInputLayout));
                }
            });
        }
    }

    private ServerApi.DataListener createRequestCodeDataListener(
            @NonNull final String email,
            @NonNull final TextInputLayout resetEmailInputLayout) {
        return new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    String message = json.getString("response");
                    if (!json.getBoolean("res")) {
                        String field = json.getString("field");
                        if ("email".equals(field)) {
                            resetEmailInputLayout.setError(message);
                        } else {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    resetDialog.setContentView(R.layout.reset_pass_code);

                    AppCompatButton cancelButton = (AppCompatButton) resetDialog.findViewById(R.id.reset_cancel_button);
                    if (cancelButton != null) {
                        cancelButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                resetDialog.dismiss();
                            }
                        });
                    }

                    final AppCompatEditText codeEditText = (AppCompatEditText) resetDialog.findViewById(R.id.reset_code_edittext);
                    final TextInputLayout codeInputLayout = (TextInputLayout) resetDialog.findViewById(R.id.reset_code_inputlayout);
                    if (codeEditText != null && codeInputLayout != null) {
                        codeEditText.addTextChangedListener(new ErrorCleanTextWatcher(codeInputLayout));
                    }

                    final AppCompatEditText newPasswordEditText = (AppCompatEditText) resetDialog.findViewById(R.id.reset_new_password_edittext);
                    final TextInputLayout newPasswordInputLayout = (TextInputLayout) resetDialog.findViewById(R.id.reset_new_password_inputlayout);
                    if (newPasswordEditText != null && newPasswordInputLayout != null) {
                        newPasswordEditText.addTextChangedListener(new ErrorCleanTextWatcher(newPasswordInputLayout));
                    }

                    AppCompatButton okButton = (AppCompatButton) resetDialog.findViewById(R.id.reset_ok_button);
                    if (okButton != null) {
                        okButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (codeEditText == null
                                        || codeInputLayout == null
                                        || newPasswordEditText == null
                                        || newPasswordInputLayout == null) {
                                    return;
                                }

                                String code = codeEditText.getText().toString();
                                if (code.isEmpty()) {
                                    codeInputLayout.setError(getString(R.string.reset_password_code_empty_error));
                                    return;
                                }

                                String newPassword = newPasswordEditText.getText().toString();
                                if (newPassword.isEmpty()) {
                                    newPasswordInputLayout.setError(getString(R.string.login_password_empty_error));
                                    return;
                                }

                                ServerApi.resetPasswordWithCode(email, code, newPassword,
                                        createChangePasswordDataListener(codeInputLayout, newPasswordInputLayout));
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {
            }
        };
    }

    private ServerApi.DataListener createChangePasswordDataListener(
            @NonNull final TextInputLayout codeInputLayout,
            @NonNull final TextInputLayout newPasswordInputLayout) {
        return new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    String message = json.getString("response");
                    if (json.getBoolean("res")) {
                        resetDialog.dismiss();
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String field = json.getString("field");
                    switch (field) {
                        case "code":
                            codeInputLayout.setError(message);
                            break;
                        case "password":
                            newPasswordInputLayout.setError(message);
                            break;
                        default:
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {
            }
        };
    }

}
