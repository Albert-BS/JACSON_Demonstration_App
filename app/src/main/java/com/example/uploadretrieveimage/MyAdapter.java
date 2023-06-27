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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class MyAdapter {
    private final DatabaseReference database;
    String username, imageURL;
    private final String country;

    public MyAdapter(String username, String imageURL, String country) {
        this.database = FirebaseDatabase.getInstance().getReference();
        this.username = username;
        this.imageURL = imageURL;
        this.country = country;
    }

    public String fetchAndProcessPolicy() throws IOException, JSONException {
        List<Map<String, Object>> attributes = new ArrayList<>();
        final DatabaseReference[] usersRef = {database.child("Users")};
        final DatabaseReference[] imagesRef = {database.child("Images")};
        DatabaseReference policiesRef = database.child("Policies");

        CompletableFuture<String> captionFuture = new CompletableFuture<>();
        CompletableFuture<String> policyIdFuture = new CompletableFuture<>();
        CompletableFuture<Void> attributesFuture = new CompletableFuture<>();

        final String[] policyJson = new String[1];
        Query userQuery = usersRef[0].orderByChild("username").equalTo(username);
        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
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
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String imageId = dataSnapshot.getChildren().iterator().next().getKey();
                                DataSnapshot imageSnapshot = dataSnapshot.child(imageId);
                                if (imageSnapshot.hasChild("caption")) {
                                    String caption = imageSnapshot.child("caption").getValue(String.class);
                                    captionFuture.complete(caption);
                                }
                                DatabaseReference imageRef = imagesRef[0].child(imageId);
                                imageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists() && dataSnapshot.hasChild("policyId")) {
                                            String policyId = dataSnapshot.child("policyId").getValue(String.class);
                                            policyIdFuture.complete(policyId);
                                            DatabaseReference policyRef = policiesRef.child(policyId);
                                            policyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if (dataSnapshot.exists()) {
                                                        policyJson[0] = dataSnapshot.getValue(String.class);
                                                        processJSON(policyJson[0], attributes, map);
                                                        attributesFuture.complete(null);
                                                    }
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {}
                                            });
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                                });
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
        try {
            CompletableFuture.allOf(captionFuture, policyIdFuture, attributesFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        String caption = captionFuture.join();
        String policyId = policyIdFuture.join();
        DatabaseReference requestsRef = database.child("Requests");
        final long[] requestsCount = {0};
        CountDownLatch latch = new CountDownLatch(1);
        String requestJson;
        requestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestsCount[0] = snapshot.getChildrenCount();
                latch.countDown();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("returnPolicyIdList", false);
        request.put("schemaLocation", "http://json-schema.org/draft-06/schema");
        request.put("desc", "Request to access image " + (caption != null ? caption : ""));
        String policyIdValue = (policyId != null ? policyId : "");
        request.put("requestId", "request" + (requestsCount[0]+1));
        request.put("attributes", attributes);

        Map<String, Object> wrappedRequest = new LinkedHashMap<>();
        wrappedRequest.put("Request", request);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            requestJson = objectMapper.writeValueAsString(wrappedRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String requestId = "request" + (requestsCount[0] + 1);
        requestsRef.child(requestId).setValue(requestJson);

        String response;
        if (requestJson.isEmpty()) {
            response = "{\"results\":[{\"result\":\"Permit\"}]}";
        }
        else {
            response = Validator.checkPolicy(requestJson, policyJson[0]);
        }
        JSONObject responseJson = new JSONObject(response);
        JSONArray resultsArray = responseJson.getJSONArray("results");
        if (resultsArray.length() > 0) {
            JSONObject resultObject = resultsArray.getJSONObject(0);
            String resultValue = resultObject.getString("result");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
            dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
            String currentDateAndTime = dateFormat.format(new Date());

            String logMessage = "Access to " + caption + " image by user " + username + ": " +
                    resultValue + ", Policy: " + policyIdValue + ", Request: " + requestId;
            String logEntryId = currentDateAndTime + "_" + System.currentTimeMillis();

            DatabaseReference loggersRef = database.child("Loggers");

            loggersRef.child(logEntryId).setValue(logMessage);
        }
        return response;
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
            boolean attributeIdExists = false;
            for (JsonNode nestedApply : applyNode) {
                JsonNode attributeValueNode = nestedApply.path("attributeValue");
                JsonNode attributeDesignatorNode = nestedApply.path("attributeDesignator");
                String dataType = attributeValueNode.path("dataType").asText();
                String category = attributeDesignatorNode.path("category").asText();
                String attributeId = attributeDesignatorNode.path("attributeId").asText();
                if (!map.containsKey(attributeId)) {
                    processApply(nestedApply.path("apply"), attributes, map);
                    continue;
                }

                for (Map<String, Object> existingAttribute : attributes) {
                    String existingAttributeId = (String) existingAttribute.get("attributeId");
                    if (attributeId.equals(existingAttributeId)) {
                        attributeIdExists = true;
                        break;
                    }
                }
                if (attributeIdExists) {
                    attributeIdExists = false;
                    processApply(nestedApply.path("apply"), attributes, map);
                    continue;
                }

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



