package com.foxykeep.cpcodegenerator.model;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

public class Md5WhereClauseData {

    public static final int TYPE_EQ = 1;
    public static final int TYPE_NE = 2;
    public static final int TYPE_LT = 3;
    public static final int TYPE_LTOE = 4;
    public static final int TYPE_GT = 5;
    public static final int TYPE_GTOE = 6;

    private FieldData fieldData;
    private String value;
    private int type;

    // "md5_where":[
    // {
    // "field":"isPlayerBuilding"
    // "value":"true"
    // "type":1
    // }
    // ]

    public Md5WhereClauseData(final ArrayList<FieldData> fieldList, final JSONObject json) throws JSONException, IllegalArgumentException {
        final String field = json.getString("field");
        boolean fieldFound = false;
        final int fieldListSize = fieldList.size();
        for (int i = 0; i < fieldListSize; i++) {
            if (fieldList.get(i).name.equals(field)) {
                fieldData = fieldList.get(i);
                fieldFound = true;
                break;
            }
        }

        if (!fieldFound) {
            throw new IllegalArgumentException("Field does not exist in the class");
        }

        value = json.getString("value");
        type = json.getInt("type");
    }

    public void generateCriteriaMethod(final ClassData classData, final StringBuilder sb) {
        sb.append(".");
        switch (type) {
            case TYPE_EQ:
                sb.append("addEq(");
                break;
            case TYPE_NE:
                sb.append("addNe(");
                break;
            case TYPE_LT:
            case TYPE_LTOE:
                sb.append("addLt(");
                break;
            case TYPE_GT:
            case TYPE_GTOE:
                sb.append("addGt(");
                break;
        }
        sb.append(classData.dbClassName).append(".COLUMNS.").append(fieldData.dbConstantName).append(", ").append(value);
        switch (type) {
            case TYPE_EQ:
            case TYPE_NE:
                sb.append(")");
                break;
            case TYPE_LT:
            case TYPE_GT:
                sb.append(", false)");
                break;
            case TYPE_LTOE:
            case TYPE_GTOE:
                sb.append(", false)");
                break;
        }
    }
}
