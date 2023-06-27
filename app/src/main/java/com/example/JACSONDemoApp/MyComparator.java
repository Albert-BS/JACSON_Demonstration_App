package com.example.JACSONDemoApp;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.Date;

public class MyComparator implements Comparator<Object> {
    public int compare(Object reqValue, Object polValue) {
        if (reqValue instanceof Integer intReq && polValue instanceof Integer intPol) {
            return intReq.compareTo(intPol);
        } else if (reqValue instanceof Boolean boolReq && polValue instanceof Boolean boolPol) {
            return Boolean.compare(boolReq, boolPol);
        } else if (reqValue instanceof Double doubleReq && polValue instanceof Double doublePol) {
            return Double.compare(doubleReq, doublePol);
        } else if (reqValue instanceof Float floatReq && polValue instanceof Float floatPol) {
            return Float.compare(floatReq, floatPol);
        } else if (reqValue instanceof Date dateReq && polValue instanceof Date datePol) {
            return dateReq.compareTo(datePol);
        } else if (reqValue instanceof LocalTime timeReq && polValue instanceof LocalTime timePol) {
            return timeReq.compareTo(timePol);
        } else if (reqValue instanceof String stringReq && polValue instanceof String stringPol) {
            return stringReq.compareTo(stringPol);
        }
        return 0;
    }
}
