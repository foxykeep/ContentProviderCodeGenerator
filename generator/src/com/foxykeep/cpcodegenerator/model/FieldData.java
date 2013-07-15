package com.foxykeep.cpcodegenerator.model;

import com.foxykeep.cpcodegenerator.util.JsonUtils;
import com.foxykeep.cpcodegenerator.util.NameUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class FieldData {

    public String name;
    public String type;

    public int version;

    public String dbName;
    public String dbConstantName;
    public String dbType = null;
    public boolean dbIsPrimaryKey;
    public boolean dbIsId;
    public boolean dbIsAutoincrement = false;
    public boolean dbHasIndex;
    public boolean dbSkipBulkInsert;
    public String dbDefaultValue;
    public boolean dbIsUnique;

    public FieldData(final JSONObject json) throws JSONException {
        name = json.getString("name");
        setType(json.getString("type"));

        version = json.optInt("version", 1);

        dbConstantName = NameUtils.createConstantName(name);
        dbIsPrimaryKey = json.optBoolean("is_primary_key", false);
        dbIsId = json.optBoolean("is_id", false);
        dbIsAutoincrement = json.optBoolean("is_autoincrement", false);
        if (dbIsId) {
            if (!dbIsPrimaryKey) {
                throw new IllegalArgumentException("Field \"" + name + "\" | is_id can only be "
                        + "used on a field flagged with is_primary_key");
            }
            dbName = "_id";
            if (dbIsAutoincrement && !type.equals("integer")) {
                throw new IllegalArgumentException("Field \"" + name + "\" | is_autoincrement can "
                        + "only be used on an integer type field");
            }
        } else {
            if (dbIsAutoincrement) {
                throw new IllegalArgumentException("Field \"" + name + "\" | id_autoincrement can "
                        + "only be used on a field flagged with is_id");
            }
            dbName = name;
        }
        dbHasIndex = !dbIsPrimaryKey && json.optBoolean("is_index", false);

        dbSkipBulkInsert = json.optBoolean("skip_bulk_insert", false);

        dbDefaultValue = JsonUtils.getStringFixFalseNull(json, "default");
        dbIsUnique = json.optBoolean("unique", false);
    }

    private void setType(final String type) {
        this.type = type;

        if (type.equals("int") || type.equals("integer") || type.equals("long")
                || type.equals("boolean") || type.equals("date")) {
            dbType = "integer";
        } else if (type.equals("float") || type.equals("double") || type.equals("real")) {
            dbType = "real";
        } else if (type.equals("string") || type.equals("text") || type.equals("String")) {
            dbType = "text";
        }else if (type.equals("Blob") || type.equals("blob") ) {
            dbType = "blob";
        }
    }

    public static String getDefaultValue(final String type) {
        if (type.equals("string") || type.equals("text") || type.equals("String")) {
            return "''";
        } else {
            return "-1";
        }
    }
}
