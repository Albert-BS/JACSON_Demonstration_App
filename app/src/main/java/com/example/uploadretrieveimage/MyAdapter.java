package com.example.uploadretrieveimage;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    public String fetchAndProcessPolicy() {
        List<Map<String, Object>> attributes = new ArrayList<>();
        final DatabaseReference[] usersRef = {database.child("Users")};
        final DatabaseReference[] imagesRef = {database.child("Images")};
        DatabaseReference policiesRef = database.child("Policies");

        CompletableFuture<String> captionFuture = new CompletableFuture<>();
        CompletableFuture<String> policyIdFuture = new CompletableFuture<>();
        CompletableFuture<Void> attributesFuture = new CompletableFuture<>();

        // Get the user key using the username
        Query userQuery = usersRef[0].orderByChild("username").equalTo(username);
        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String userKey = dataSnapshot.getChildren().iterator().next().getKey();
                    HashMap<String, Object> userData = (HashMap<String, Object>) dataSnapshot.getChildren().iterator().next().getValue();
                    Map<String, String> map = new HashMap<>();
                    for (Map.Entry<String, Object> entry : userData.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue().toString();
                        map.put(key, value);
                    }
                    Query imageQuery = imagesRef[0].orderByChild("imageURL").equalTo(imageURL);
                    imageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String imageId = dataSnapshot.getChildren().iterator().next().getKey();
                                DataSnapshot imageSnapshot = dataSnapshot.child(imageId);
                                if (imageSnapshot.hasChild("caption")) {
                                    String caption = imageSnapshot.child("caption").getValue(String.class);
                                    captionFuture.complete(caption);
                                }

                                // Get the policy ID for the image
                                DatabaseReference imageRef = imagesRef[0].child(imageId);
                                imageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists() && dataSnapshot.hasChild("policyId")) {
                                            String policyId = dataSnapshot.child("policyId").getValue(String.class);
                                            policyIdFuture.complete(policyId);

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
                                                        processJSON(policyJson, attributes, map);
                                                        attributesFuture.complete(null);
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

        try {
            CompletableFuture.allOf(captionFuture, policyIdFuture, attributesFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        String caption = captionFuture.join();
        String policyId = policyIdFuture.join();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("returnPolicyIdList", false);
        request.put("schemaLocation", "http://json-schema.org/draft-06/schema");
        request.put("desc", "Request to access image " + (caption != null ? caption : ""));
        String policyIdValue = (policyId != null ? policyId : "");
        String modifiedPolicyIdValue = policyIdValue.replace("policy", "request");
        request.put("requestId", modifiedPolicyIdValue);
        request.put("attributes", attributes);

        Map<String, Object> wrappedRequest = new LinkedHashMap<>();
        wrappedRequest.put("Request", request);

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(wrappedRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return requestJson;
    }

    private void processJSON(String policyString, List<Map<String, Object>> attributes, Map<String, String> map) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            JsonNode policyNode = objectMapper.readTree(policyString);
            processConditions(policyNode, attributes, map);
        } catch (JsonProcessingException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void processConditions(JsonNode policyNode, List<Map<String, Object>> attributes, Map<String, String> map) throws ParseException {
        JsonNode rulesNode = policyNode.path("Policy").path("rule");
        for (JsonNode ruleNode : rulesNode) {
            JsonNode conditionNode = ruleNode.path("condition");
            if (conditionNode.isArray()) {
                for (JsonNode condition : conditionNode) {
                    processCondition(condition, attributes, map);
                }
            }
        }
    }

    private void processCondition(JsonNode conditionNode, List<Map<String, Object>> attributes, Map<String, String> map) throws ParseException {
        processApply(conditionNode.path("apply"), attributes, map);
    }

    private void processApply(JsonNode applyNode, List<Map<String, Object>> attributes, Map<String, String> map) throws ParseException {
        if (applyNode.isArray()) {
            for (JsonNode nestedApply : applyNode) {
                JsonNode attributeValueNode = nestedApply.path("attributeValue");
                JsonNode attributeDesignatorNode = nestedApply.path("attributeDesignator");

                String dataType = attributeValueNode.path("dataType").asText();
                String category = attributeDesignatorNode.path("category").asText();
                String attributeId = attributeDesignatorNode.path("attributeId").asText();

                Map<String, Object> attribute = new LinkedHashMap<>();
                attribute.put("attributeId", attributeId);

                Map<String, Object> attributeValue = new LinkedHashMap<>();
                attributeValue.put("dataType", dataType);

                if (attributeId.equals("location")) {
                    attributeValue.put("value", country);
                }
                else if (attributeId.equals("age")) {
                    if (map.containsKey("birthday")) {
                        String birthdayString = map.get("birthday");
                        LocalDate birthday = LocalDate.parse(birthdayString);
                        LocalDate currentDate = LocalDate.now();
                        int age = Period.between(birthday, currentDate).getYears();
                        attributeValue.put("value", age);
                    }
                }
                else if (map.containsKey(attributeId)) {
                        String attributeVal = map.get(attributeId);
                        Object attributeMap = convertAttributeValue(attributeVal, dataType);
                        attributeValue.put("value", attributeMap);
                }

                attribute.put("attributeValue", attributeValue);

                List<Map<String, Object>> attributeList = new ArrayList<>();
                attributeList.add(attribute);

                Map<String, Object> categoryMap = new LinkedHashMap<>();
                categoryMap.put("category", category);
                categoryMap.put("attribute", attributeList);

                attributes.add(categoryMap);

                processApply(nestedApply.path("apply"), attributes, map);
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



