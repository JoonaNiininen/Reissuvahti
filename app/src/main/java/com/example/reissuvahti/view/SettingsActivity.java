package com.example.reissuvahti.view;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.reissuvahti.R;
import com.example.reissuvahti.async.GetUserHome;

import java.lang.ref.WeakReference;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        GetUserHome task = new GetUserHome(new WeakReference<>(SettingsActivity.this));
        task.execute("pottala");
        TextView currentUserView = findViewById(R.id.currentUserText);
        Button setHomeButton = findViewById(R.id.setHomeBtn);
        setHomeButton.setOnClickListener(setUserHomeCoordinates);
        currentUserView.setText("pottala");
    }

    View.OnClickListener setUserHomeCoordinates = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            System.out.println("TODO");
        }
    };
}
