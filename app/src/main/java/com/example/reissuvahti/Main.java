package com.example.reissuvahti;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class Main extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startTripActivity(View view) {
        Intent intent = new Intent(this, TripActivity.class);
        startActivity(intent);
    }

}
