package com.foxykeep.cpcodegenerator.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.foxykeep.cpcodegenerator.util.NameUtil;

public class FieldData {

    public String name;
    public String type;

    public String dbName;
    public String dbConstantName;
    public String dbType = null;
    public boolean dbIsPrimaryKey;
    public boolean dbIsId;
    public boolean dbHasIndex;

    public FieldData(final JSONObject json) throws JSONException {
        name = json.getString("name");
        setType(json.getString("type"));

        dbConstantName = NameUtil.createConstantName(name);
        dbIsPrimaryKey = json.optBoolean("is_primary_key", false);
        dbIsId = json.optBoolean("is_id", false);
        if (dbIsId) {
            dbName = "_id";
        } else {
            dbName = name;
        }
        dbHasIndex = !dbIsPrimaryKey && json.optBoolean("db_has_index", false);
    }

    private void setType(final String type) {
        this.type = type;

        if (type.equals("int") || type.equals("integer") || type.equals("long") || type.equals("boolean") || type.equals("date")) {
            dbType = "integer";
        } else if (type.equals("float") || type.equals("double") || type.equals("real")) {
            dbType = "real";
        } else if (type.equals("string") || type.equals("text") || type.equals("String")) {
            dbType = "text";
        }
    }
}
