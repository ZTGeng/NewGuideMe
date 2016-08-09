package edu.sfsu.geng.newguideme.helper;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.RatingBar;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.MyRequest;

public class HelperRateActivity extends AppCompatActivity {

    SharedPreferences pref;

    private AppCompatButton submitBtn, cancelBtn;
    private RatingBar ratingBar;
    private float rateFloat;
    private String blindName, blindId, id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_rate);
        blindName = getIntent().getStringExtra("blindName");
        blindId = getIntent().getStringExtra("blindId");

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        id = pref.getString("id", "");

        ratingBar = (RatingBar) findViewById(R.id.helper_ratingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                rateFloat = rating;
            }
        });
        ratingBar.setRating(5);

        submitBtn = (AppCompatButton) findViewById(R.id.helper_rate_btn);
        assert submitBtn != null;
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit(v);
            }
        });

        cancelBtn = (AppCompatButton) findViewById(R.id.helper_rate_cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel(v);
            }
        });

    }

    public void onSubmit(View v) {
//        Toast.makeText(this, String.valueOf(rateFloat), Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format(getResources().getString(R.string.helper_rate_message), blindName, String.valueOf(rateFloat)));
        builder.setPositiveButton(R.string.vi_rate_ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MyRequest myRequest = new MyRequest();
                myRequest.add("id", id);
                myRequest.add("ratee_id", blindId);
                myRequest.add("rate", String.valueOf(rateFloat));
                myRequest.getJSON("/api/rate", null);

                Intent homeActivity = new Intent(HelperRateActivity.this, HelperHomeActivity.class);
                startActivity(homeActivity);
                finish();
            }
        });
        builder.setNegativeButton(R.string.helper_rate_change_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    public void onCancel(View v) {
        Intent homeActivity = new Intent(HelperRateActivity.this, HelperHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }

}
