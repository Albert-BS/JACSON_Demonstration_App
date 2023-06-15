package com.example.uploadretrieveimage;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MyAdapter {
    private String username, imageURL;
    private DatabaseReference policiesRef;
    private DatabaseReference requestsRef;

    public MyAdapter(String username, String imageURL) {
        this.username = username;
        this.imageURL = imageURL;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        policiesRef = database.getReference("Images");
        requestsRef = database.getReference("Requests");
    }

    // Getter and setter methods for username, policiesRef, and requestsRef

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public DatabaseReference getPoliciesRef() {
        return policiesRef;
    }

    public DatabaseReference getRequestsRef() {
        return requestsRef;
    }

    public void uploadRequest() {
        policiesRef.orderByChild("imageURL").equalTo(imageURL).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    DataSnapshot firstSnapshot = dataSnapshot.getChildren().iterator().next();
                    String policyId = firstSnapshot.child("policyId").getValue(String.class);
                    if (policyId != null) {
                        // Access the policy using the obtained policyId
                        DatabaseReference policyRef = policiesRef.child("policy" + policyId);
                        policyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                LoginActivity loginActivity = new LoginActivity();
                                String country = loginActivity.getCountry();
                                // Retrieve the policy data
                                Object policyData = snapshot.getValue();

                                // Convert the policy data to ObjectMapper
                                ObjectMapper objectMapper = new ObjectMapper();
                                try {
                                    Policy policy = objectMapper.readValue(policyData.toString(), Policy.class);

                                } catch (Exception e) {
                                    // Handle the conversion error
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Handle the error
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error
            }
        });
    }

}

