package com.example.uploadretrieveimage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button logout;
    FloatingActionButton fab;
    private android.widget.GridView gridView;
    private ArrayList<DataClass> dataList;
    private GridActivity adapter;
    final private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Images");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //fab = findViewById(R.id.fab);

        gridView = findViewById(R.id.gridView);
        dataList = new ArrayList<>();
        adapter = new GridActivity(this, dataList);
        gridView.setAdapter(adapter);
        logout = findViewById(R.id.logout_button);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String userUsername = getIntent().getStringExtra("userUsername");
                String country = getIntent().getStringExtra("location");
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    DataClass dataClass = dataSnapshot.getValue(DataClass.class);
                    dataClass.setUsername(userUsername);
                    dataClass.setLocation(country);
                    dataList.add(dataClass);
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        /*fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UploadActivity.class);
                startActivity(intent);
                finish();
            }
        });*/

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}