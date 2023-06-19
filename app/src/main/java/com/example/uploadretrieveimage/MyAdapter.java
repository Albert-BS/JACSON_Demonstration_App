package com.example.uploadretrieveimage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.provider.ContactsContract;
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MyAdapter {
    private DatabaseReference database;
    String username, imageURL;
    private String country;

    public MyAdapter(String username, String imageURL, String country) {
        this.database = FirebaseDatabase.getInstance().getReference();
        this.username = username;
        this.imageURL = imageURL;
        this.country = country;
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
                            if (attributeId.equals("age")) {
                                String birthdayString = null;
                                DataSnapshot birthdaySnapshot = dataSnapshot.child("birthday");
                                if (birthdaySnapshot.exists()) {
                                    birthdayString = birthdaySnapshot.getValue(String.class);
                                }
                                if (birthdayString != null) {
                                    LocalDate birthday = LocalDate.parse(birthdayString);
                                    LocalDate currentDate = LocalDate.now();
                                    int attributeReq = Period.between(birthday, currentDate).getYears();

                                    System.out.println("Age: " + attributeReq);
                                }
                            }
                            else if (attributeId.equals("location")) {
                                String attributeReq = country;
                            }
                            else {
                                if (dataSnapshot.hasChild(attributeId)) {
                                    String attributeValue = dataSnapshot.child(attributeId).getValue(String.class);
                                    try {
                                        Object attributeReq = convertAttributeValue(attributeValue, dataType);
                                    } catch (ParseException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
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

    private static Object convertAttributeValue(String attribute, String dataType) throws ParseException {
        switch (dataType.toLowerCase()) {
            case "boolean":
                return Boolean.parseBoolean(attribute);
            case "integer":
                return Integer.parseInt(attribute);
            case "double":
                return Double.parseDouble(attribute);
            case "float":
                return Float.parseFloat(attribute);
            case "time":
                try {
                    @SuppressLint("SimpleDateFormat") DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                    return timeFormat.parse(attribute);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
            case "date":
                try {
                    @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    return dateFormat.parse(attribute);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
            case "datetime":
                try {
                    @SuppressLint("SimpleDateFormat") DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    return dateTimeFormat.parse(attribute);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            default:
                return attribute;
        }
        return null;
    }
}



