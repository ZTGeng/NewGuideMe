package edu.sfsu.geng.newguideme.login;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.MyRequest;
import edu.sfsu.geng.newguideme.helper.HelperHomeActivity;
import edu.sfsu.geng.newguideme.blind.BlindHomeActivity;
import edu.sfsu.geng.newguideme.http.ServerRequest;

/**
 * Created by geng on 7/15/16.
 */
public class LoginActivity extends AppCompatActivity {

    AppCompatEditText email, password, res_email, code, newpass;
    AppCompatButton login, cont, cont_code, cancel ,cancel1, register, forpass;
    String emailText, passwordText, email_res_txt, code_txt, npass_txt;
    SharedPreferences pref;
    Dialog reset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = (AppCompatEditText)findViewById(R.id.email);
        password = (AppCompatEditText)findViewById(R.id.password);
        login = (AppCompatButton)findViewById(R.id.loginbtn);
        register = (AppCompatButton)findViewById(R.id.register);
        forpass = (AppCompatButton)findViewById(R.id.forgotpass);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);

        /* If already login in, go to home activity */
        if (pref.getBoolean("logged", false)) {
            String id = pref.getString("id", "");
//            String grav = pref.getString("grav", "");
            String username = pref.getString("username", "");
            String role = pref.getString("role", "");
            String rate = pref.getString("rate", "");
            if (!id.isEmpty() &&
//                !grav.isEmpty() &&
                !username.isEmpty() &&
                !role.isEmpty() &&
                !rate.isEmpty()) {
                // login directly
                Intent homeActivity;
                if (role.equals("helper")) {
                    homeActivity = new Intent(LoginActivity.this, HelperHomeActivity.class);
                } else {// if (role.equals("blind")) {
                    homeActivity = new Intent(LoginActivity.this, BlindHomeActivity.class);
                }
                startActivity(homeActivity);
                finish();
            }
        }
        /* Always run code above when open the app */

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent regactivity = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(regactivity);
                finish();
            }
        });


        login.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {
                emailText = email.getText().toString();
                passwordText = password.getText().toString();

                MyRequest myRequest = new MyRequest();
                myRequest.add("email", emailText);
                myRequest.add("password", passwordText);

                myRequest.getJSON("/login", new ServerRequest.DataListener() {
                    @Override
                    public void onReceiveData(String data) {
                        try{
                            JSONObject json = new JSONObject(data);
                            String jsonstr = json.getString("response");
                            if(json.getBoolean("res")){
                                String id = json.getString("token"); // token is id
                                String grav = json.getString("grav");
                                String username = json.getString("username");
                                String role = json.getString("role");
                                String rate = json.getString("rate");
                                SharedPreferences.Editor edit = pref.edit();
                                //Storing Data using SharedPreferences
                                edit.putString("id", id);
                                edit.putString("grav", grav);
                                edit.putString("username", username);
                                edit.putString("role", role);
                                edit.putString("rate", rate);
                                edit.commit();
                                Intent homeActivity;
                                if (role.equals("helper")) {
                                    homeActivity = new Intent(LoginActivity.this, HelperHomeActivity.class);
                                } else {// if (role.equals("blind")) {
                                    homeActivity = new Intent(LoginActivity.this, BlindHomeActivity.class);
                                }
                                startActivity(homeActivity);
                                finish();
                            } else {
                                Toast.makeText(getApplication(), jsonstr, Toast.LENGTH_SHORT).show();
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

        forpass.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                reset = new Dialog(LoginActivity.this);
                reset.setTitle("Reset Password");
                reset.setContentView(R.layout.reset_pass_init);
                cont = (AppCompatButton)reset.findViewById(R.id.resbtn);
                cancel = (AppCompatButton)reset.findViewById(R.id.cancelbtn);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        reset.dismiss();
                    }
                });
                res_email = (AppCompatEditText)reset.findViewById(R.id.email);

                cont.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        email_res_txt = res_email.getText().toString();

                        MyRequest myRequest = new MyRequest();
                        myRequest.add("email", email_res_txt);

                        myRequest.getJSON("/api/resetpass", new ServerRequest.DataListener() {
                            @Override
                            public void onReceiveData(String data) {
                                try {
                                    JSONObject json = new JSONObject(data);
                                    String jsonStr = json.getString("response");
                                    if(json.getBoolean("res")){
                                        Log.d("JSON", jsonStr);
                                        Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_LONG).show();
                                        reset.setContentView(R.layout.reset_pass_code);
                                        cont_code = (AppCompatButton)reset.findViewById(R.id.conbtn);
                                        code = (AppCompatEditText)reset.findViewById(R.id.code);
                                        newpass = (AppCompatEditText)reset.findViewById(R.id.npass);
                                        cancel1 = (AppCompatButton)reset.findViewById(R.id.cancel);
                                        cancel1.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                reset.dismiss();
                                            }
                                        });
                                        cont_code.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                code_txt = code.getText().toString();
                                                npass_txt = newpass.getText().toString();
                                                Log.d("Code", code_txt);
                                                Log.d("New pass",npass_txt);

                                                MyRequest myRequest = new MyRequest();
                                                myRequest.add("email", email_res_txt);
                                                myRequest.add("code", code_txt);
                                                myRequest.add("newpass", npass_txt);

                                                myRequest.getJSON("/api/resetpass/chg", new ServerRequest.DataListener() {
                                                    @Override
                                                    public void onReceiveData(String data) {
                                                        try {
                                                            JSONObject json = new JSONObject(data);
                                                            String jsonstr = json.getString("response");
                                                            if(json.getBoolean("res")){
                                                                reset.dismiss();
                                                            }
                                                            Toast.makeText(getApplication(),jsonstr,Toast.LENGTH_SHORT).show();
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }

                                                    @Override
                                                    public void onClose() {}
                                                });
                                            }
                                        });
                                    } else {
                                        Toast.makeText(getApplication(),jsonStr,Toast.LENGTH_SHORT).show();
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


                reset.show();
            }
        });
    }

}
