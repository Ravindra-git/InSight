package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity3 extends AppCompatActivity {
    private Button bCapture,bCapture1,bCapture2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        bCapture = findViewById(R.id.bCapture);
        bCapture1 = findViewById(R.id.bCapture1);
        bCapture2 = findViewById(R.id.bCapture2);

        bCapture1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity3.this, "bills", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity3.this,MainActivity.class);

                startActivity(intent);
            }
        });


        bCapture2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity3.this, "meds", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity3.this,MainActivity2.class);

                startActivity(intent);
            }
        });
    }
}