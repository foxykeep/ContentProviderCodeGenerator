package com.foxykeep.databasecodegenerator.model;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DatabaseTableData {

    private static final Pattern PATTERN = Pattern.compile("([a-z])([A-Z])");
    private static final String REPLACEMENT = "$1_$2";

    public String className;
    public String tableName;
    public String tableConstantName;
    public boolean hasBulkInsert;

    public ArrayList<DatabaseColumnData> databaseColumnDataList = new ArrayList<DatabaseColumnData>();

    public DatabaseTableData(final JSONObject jsonTableData) throws JSONException {
        className = jsonTableData.getString("class_name");
        tableName = className.substring(0, 1).toLowerCase() + className.substring(1);
        tableConstantName = createConstantName(tableName);
        hasBulkInsert = jsonTableData.optBoolean("has_bulk_insert", false);

        final JSONArray jsonFieldArray = jsonTableData.getJSONArray("fields");
        final int jsonFieldArrayLength = jsonFieldArray.length();
        for (int i = 0; i < jsonFieldArrayLength; i++) {
            databaseColumnDataList.add(new DatabaseColumnData(jsonFieldArray.getJSONObject(i)));
        }
    }

    public static String createConstantName(final String tableName) {
        return PATTERN.matcher(tableName).replaceAll(REPLACEMENT).toUpperCase();
    }

    public class DatabaseColumnData {
        public String name;
        public String constantName;
        public String type;
        public String bulkType;
        public boolean isPrimaryKey;
        public boolean isIndex;

        public DatabaseColumnData(final JSONObject jsonColumnData) throws JSONException {
            name = jsonColumnData.getString("name");
            constantName = createConstantName(name);
            setType(jsonColumnData.getString("type"));
            isPrimaryKey = jsonColumnData.optBoolean("is_primary_key", false);
            isIndex = jsonColumnData.optBoolean("is_index", false);
        }

        public void setType(final String dbType) {
            type = dbType;
            if (dbType.equals("integer")) {
                bulkType = "Long";
            } else if (dbType.equals("real")) {
                bulkType = "Double";
            } else if (dbType.equals("text")) {
                bulkType = "String";
            }
        }
    }
}
