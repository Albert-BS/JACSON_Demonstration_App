package com.example.uploadretrieveimage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Validator {

    public Validator() {
    }

    public static String checkPolicy(String requestJson, String policyJson) throws IOException {
        boolean isMatch;
        boolean conditionAnd;
        Map<String, String> requestMap = parseXacmlRequest(requestJson);
        ObjectMapper objectMapper = new ObjectMapper();
        String topRuleEffect = null, bottomRuleEffect = null;
        JsonNode policyNode = objectMapper.readTree(policyJson);
        try {

            topRuleEffect = policyNode.at("/Policy/rule/0/effect").asText();
            bottomRuleEffect = policyNode.at("/Policy/rule/1/effect").asText();

            JsonNode targetNode = policyNode.path("Policy").path("rule").get(0).path("target").get(0)
                    .path("AnyOf").get(0).path("AllOf");
            JsonNode conditionNode = policyNode.path("Policy").path("rule").get(0).path("condition");
            JsonNode conditionFunction = policyNode.path("Policy").path("rule").get(0).path("condition");
            String functionAndOr = conditionFunction.at("/0/functionId").asText();
            conditionAnd = (functionAndOr.endsWith(":and")); // conditionAnd = true if ends with ":and", else false
            isMatch = evaluateCondition(conditionNode, requestMap, "empty");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        JsonNode requestNode = objectMapper.readTree(requestJson);
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policyIdList", false);
        result.put("policyId", policyNode.at("/Policy/policyId").asText());
        result.put("requestId", requestNode.at("/Request/requestId").asText());
        result.put("desc", isMatch ? "Access allowed" : "Access denied");
        result.put("result", isMatch ? topRuleEffect : bottomRuleEffect);
        response.put("schemaLocation", "http://json-schema.org/draft-06/schema");
        response.put("results", new Object[]{result});

        return objectMapper.writeValueAsString(response);
    }

    private static boolean evaluateCondition(JsonNode conditionNode, Map<String, String> requestMap, String prevFunctionId) throws ParseException {
        List<Boolean> evaluations = new ArrayList<>();

        for (JsonNode element : conditionNode) {
            String functionIdText = element.at("/functionId").asText();
            String functionId = functionIdText.substring(functionIdText.lastIndexOf(":") + 1);

            boolean isMatch;

            if (functionId.equals("and") || functionId.equals("or")) {
                JsonNode applyNode = element.at("/apply");
                boolean applyResult = evaluateCondition(applyNode, requestMap, functionId);
                evaluations.add(applyResult);
            } else {
                String value = element.at("/attributeValue/value").asText();
                String dataType = element.at("/attributeValue/dataType").asText();
                String category = element.at("/attributeDesignator/category").asText();
                String attributeId = element.at("/attributeDesignator/attributeId").asText();
                boolean mustBePresent = element.at("/attributeDesignator/mustBePresent").asBoolean();

                String key = category + "_" + attributeId;

                if (!requestMap.containsKey(key)) {
                    if (mustBePresent) {
                        isMatch = false;
                        evaluations.add(isMatch);
                        break;
                    } else {
                        continue;
                    }
                }

                String req = requestMap.get(key);
                String[] reqValues = req.split(",");
                String reqValue = reqValues[0];
                String reqDataType = reqValues[1];

                if (!Objects.equals(dataType, reqDataType)) {
                    isMatch = false;
                    evaluations.add(isMatch);
                    break;
                }

                Object polParsedAttribute = convertAttributeValue(value, dataType);
                Object reqParsedAttribute = convertAttributeValue(reqValue, reqDataType);

                isMatch = compareAttributes(reqParsedAttribute, polParsedAttribute, functionId);
                evaluations.add(isMatch);
            }
        }

        boolean isMatch;
        if (prevFunctionId.equals("and")) {
            isMatch = !evaluations.contains(false);
        } else { // "or"
            isMatch = evaluations.contains(true);
        }

        return isMatch;
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
                    DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                    return timeFormat.parse(attribute);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
            case "date":
                try {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    return dateFormat.parse(attribute);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
            case "datetime":
                try {
                    DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    return dateTimeFormat.parse(attribute);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            default:
                return attribute;
        }
        return null;
    }

    private static boolean compareAttributes(Object reqParsedAttribute, Object polParsedAttribute, String condition) {
        String extractedCondition = extractCondition(condition); // Extract the condition from the input string
        MyComparator comparator = new MyComparator();
        int result = comparator.compare(reqParsedAttribute, polParsedAttribute); // -1 1st < 2nd, 0 ==, 1 1st > 2nd

        String reqValue = reqParsedAttribute.toString();
        String polValue = polParsedAttribute.toString();

        switch (extractedCondition) {
            case "regexp-match" -> {
                return reqValue.matches(polValue);
            }
            case "equal" -> {
                return (result == 0);
            }
            case "greater-than" -> {  // req > pol
                return (result > 0);
            }
            case "greater-than-or-equal" -> {
                return (result >= 0);
            }
            case "less-than" -> {  // req < pol
                return (result < 0);
            }
            case "less-than-or-equal" -> {
                return (result <= 0);
            }
            case "contains" -> {
                return reqValue.contains(polValue);
            }
            case "starts-with" -> {
                return reqValue.startsWith(polValue);
            }
            case "ends-with" -> {
                return reqValue.endsWith(polValue);
            }
        }
        return false;
    }

    private static String extractCondition(String condition) {
        String[] parts = condition.split("-");
        StringBuilder extractedCondition = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) {
                extractedCondition.append("-");
            }
            extractedCondition.append(parts[i]);
        }
        return extractedCondition.toString();
    }

    public static Map<String, String> parseXacmlRequest(String requestJson) throws IOException {
        // Create an ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, String> attributeMap = new HashMap<>();
        try {
            // Read the JSON file and parse it into a JsonNode
            JsonNode rootNode = objectMapper.readTree(requestJson);

            // Create a map to store the attribute value

            // Get the "attributes" array from the JSON request
            JsonNode attributesNode = rootNode.path("Request").path("attributes");

            // Iterate over the attributes array
            Iterator<JsonNode> attributesIterator = attributesNode.elements();
            while (attributesIterator.hasNext()) {
                JsonNode attributeNode = attributesIterator.next();

                // Get the "category" and "attributeId" values
                String category = attributeNode.path("category").asText();
                String attributeId = attributeNode.path("attribute").get(0).path("attributeId").asText();

                // Get the "value" and "dataType" values
                String value = attributeNode.path("attribute").get(0).path("attributeValue").path("value").asText();
                String dataType = attributeNode.path("attribute").get(0).path("attributeValue").path("dataType").asText();

                // Construct the map key and value
                String mapKey = category + "_" + attributeId;
                String mapValue = value + "," + dataType;

                // Add the key-value pair to the attribute map
                attributeMap.put(mapKey, mapValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return attributeMap;
    }
}
