package com.foxykeep.cpcodegenerator.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.foxykeep.cpcodegenerator.util.NameUtil;

public class FieldData {

    public String name;
    public String type;
    public String methodType;
    public String subClass;
    public String subClassField;

    public String dbName;
    public String dbConstantName;
    public String dbType = null;
    public boolean dbIsPrimaryKey;
    public boolean dbIsId;
    public boolean dbHasIndex;

    public boolean isVersionField;

    public String jsonField;

    public int md5Order = -1;
    public String md5Type = null;

    public FieldData(final JSONObject json) throws JSONException {
        name = json.getString("name");
        setType(json.getString("type"));

        isVersionField = json.optBoolean("is_version_field", false);

        dbConstantName = NameUtil.createConstantName(name);
        dbIsPrimaryKey = json.optBoolean("db_is_primary_key", false);
        if (dbIsPrimaryKey && !isVersionField) {
            dbName = "_id";
        } else {
            dbName = name;
        }
        dbHasIndex = !dbIsPrimaryKey && json.optBoolean("db_has_index", false);

        jsonField = json.optString("json_field");

        md5Order = json.optInt("md5_order", -1);
        md5Type = json.optString("md5_type");
    }

    private void setType(final String type) {
        this.type = type;
        methodType = type.substring(0, 1).toUpperCase() + type.substring(1);

        if (type.equals("int") || type.equals("long") || type.equals("boolean") || type.equals("date")) {
            dbType = "integer";
        } else if (type.equals("float") || type.equals("double")) {
            dbType = "real";
        } else if (type.equals("string")) {
            dbType = "text";
        }
    }
}
