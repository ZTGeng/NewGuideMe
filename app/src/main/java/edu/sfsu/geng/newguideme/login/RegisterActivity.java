package edu.sfsu.geng.newguideme.login;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.MyRequest;
import edu.sfsu.geng.newguideme.http.ServerRequest;

/**
 * Created by geng on 7/15/16.
 */
public class RegisterActivity extends AppCompatActivity {

    final static int PASSWORDLIMIT = 8;

    AppCompatEditText email,username,password;
    AppCompatButton login,registerV, registerH;
    String emailText, usernameText, passwordText;
    boolean emailOK, usernameOK, passwordOK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        email = (AppCompatEditText)findViewById(R.id.email);
        username = (AppCompatEditText)findViewById(R.id.username);
        password = (AppCompatEditText)findViewById(R.id.password);
        registerV = (AppCompatButton)findViewById(R.id.registerbtnv);
        registerH = (AppCompatButton)findViewById(R.id.registerbtnh);
        login = (AppCompatButton)findViewById(R.id.login);

        emailOK = false;
        usernameOK = false;
        passwordOK = false;

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent regactivity = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(regactivity);
                finish();
            }
        });


        registerV.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                register(true);
            }
        });

        registerH.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                register(false);
            }
        });

        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailOK = (s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordOK = (s.length() >= PASSWORDLIMIT);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                for (int i = 0; i < s.length(); i++) {
                    if (!Character.isLetterOrDigit(s.charAt(i))) {
                        username.setTextColor(Color.RED);
                        usernameOK = false;
                        return;
                    }
                }
                username.setTextColor(Color.BLACK);
                usernameOK = (s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void register(boolean isV) {
        if (!isInputValid()) return;

        emailText = email.getText().toString();
        usernameText = username.getText().toString();
        passwordText = password.getText().toString();

        MyRequest myRequest = new MyRequest();
        myRequest.add("email", emailText);
        myRequest.add("username", usernameText);
        myRequest.add("password", passwordText);
        myRequest.add("role", isV ? "blind" : "helper");
        myRequest.getJSON("/register", new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try{
                    JSONObject json = new JSONObject(data);
                    String jsonStr = json.getString("response");

                    Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
                    Log.d("Hello", jsonStr);

                    Intent regactivity = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(regactivity);
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    private boolean isInputValid() {
        if (!emailOK) {
            Toast.makeText(getApplication(), R.string.email_not_ok_hint, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!usernameOK) {
            Toast.makeText(getApplication(), R.string.username_not_ok_hint, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!passwordOK) {
            Toast.makeText(getApplication(), String.format(getResources().getString(R.string.password_not_ok_hint), PASSWORDLIMIT), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

}
