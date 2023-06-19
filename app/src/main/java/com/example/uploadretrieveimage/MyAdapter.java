package com.example.uploadretrieveimage;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MyAdapter {
    private DatabaseReference database;
    String username, imageURL;
    private String country;

    public MyAdapter(String username, String imageURL) {
        this.database = FirebaseDatabase.getInstance().getReference();
        this.username = username;
        this.imageURL = imageURL;
    }

    public void setCountry(String country){
        this.country = country;
    }

    public String fetchAndProcessPolicy() {
        DatabaseReference usersRef = database.child("Users");
        DatabaseReference imagesRef = database.child("Images");
        DatabaseReference policiesRef = database.child("Policies");

        // Get the user key using the username
        Query userQuery = usersRef.orderByChild("username").equalTo(username);
        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String userKey = dataSnapshot.getChildren().iterator().next().getKey();

                    // Get the image ID using the imageURL
                    Query imageQuery = imagesRef.orderByChild("imageURL").equalTo(imageURL);
                    imageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String imageId = dataSnapshot.getChildren().iterator().next().getKey();

                                // Get the policy ID for the image
                                DatabaseReference imageRef = imagesRef.child(imageId);
                                imageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists() && dataSnapshot.hasChild("policyId")) {
                                            String policyId = dataSnapshot.child("policyId").getValue(String.class);

                                            // Get the policy using the policy ID
                                            DatabaseReference policyRef = policiesRef.child(policyId);
                                            policyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    if (dataSnapshot.exists()) {
                                                        // Process the policy JSON
                                                        String policyJson = dataSnapshot.getValue(String.class);
                                                        // Parse the JSON into a Java object (you can use a JSON library like Jackson or Gson)
                                                        // Process the conditions in the policy
                                                        processJSON(policyJson);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                    // Handle the error
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        // Handle the error
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // Handle the error
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle the error
            }
        });
        return "result";
    }

    private void processJSON(String policyString) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            // Parse the policy JSON string into a Map
            JsonNode policyNode = objectMapper.readTree(policyString);
            processConditions(policyNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void processConditions(JsonNode policyNode) {
        JsonNode rulesNode = policyNode.path("Policy").path("rule");
        for (JsonNode ruleNode : rulesNode) {
            JsonNode conditionNode = ruleNode.path("condition");
            if (conditionNode.isArray()) {
                for (JsonNode condition : conditionNode) {
                    processCondition(condition);
                }
            }
        }
    }

    private void processCondition(JsonNode conditionNode) {
        processApply(conditionNode.path("apply"));
    }

    private void processApply(JsonNode applyNode) {
        if (applyNode.isArray()) {
            for (JsonNode nestedApply : applyNode) {
                JsonNode attributeValueNode = nestedApply.path("attributeValue");
                JsonNode attributeDesignatorNode = nestedApply.path("attributeDesignator");

                String dataType = attributeValueNode.path("dataType").asText();
                String category = attributeDesignatorNode.path("category").asText();
                String attributeId = attributeDesignatorNode.path("attributeId").asText();

                System.out.println("dataType: " + dataType);
                System.out.println("category: " + category);
                System.out.println("attributeId: " + attributeId);

                DatabaseReference userRef = database.child("Users").child(username);
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            if (dataSnapshot.hasChild(attributeId)) {
                                String attributeValue = dataSnapshot.child(attributeId).getValue(String.class);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                processApply(nestedApply.path("apply"));
            }
        }
    }
}



