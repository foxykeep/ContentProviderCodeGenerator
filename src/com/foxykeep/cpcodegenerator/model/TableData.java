package com.foxykeep.cpcodegenerator.model;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.foxykeep.cpcodegenerator.util.NameUtil;

public class TableData {

    public String dbClassName = null;
    public String dbTableName = null;
    public String dbConstantName = null;

    public ArrayList<FieldData> fieldList = new ArrayList<FieldData>();

    public TableData(final JSONObject json) throws JSONException {
        dbClassName = json.getString("table_name");
        dbTableName = NameUtil.createLowerCamelCaseName(dbClassName);
        dbConstantName = NameUtil.createConstantName(dbTableName);

        final JSONArray jsonFieldArray = json.getJSONArray("fields");
        final int jsonFieldArrayLength = jsonFieldArray.length();
        for (int i = 0; i < jsonFieldArrayLength; i++) {
            fieldList.add(new FieldData(jsonFieldArray.getJSONObject(i)));
        }
    }

    public static ArrayList<TableData> getClassesData(final JSONArray jsonClassArray) throws JSONException {
        final ArrayList<TableData> classDataList = new ArrayList<TableData>();

        final int jsonClassArrayLength = jsonClassArray.length();
        for (int i = 0; i < jsonClassArrayLength; i++) {
            classDataList.add(new TableData(jsonClassArray.getJSONObject(i)));
        }

        return classDataList;
    }
}
