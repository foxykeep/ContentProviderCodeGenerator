package com.foxykeep.cpcodegenerator.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.foxykeep.cpcodegenerator.CodeBuilder;
import com.foxykeep.cpcodegenerator.Config;
import com.foxykeep.cpcodegenerator.FileCache;
import com.foxykeep.cpcodegenerator.util.NameUtil;

public class ClassData {

    private static final String VERSION_JSON_OBJECT = "{\"name\":\"version\",\"type\":\"string\",\"db_has_index\":true,\"is_version_field\":true,\"db_is_primary_key\":true}";

    public String name;

    public String dbClassName = null;
    public String dbTableName = null;
    public String dbConstantName = null;

    public String plistFile;

    public String superclass;

    public boolean addJsonWriteMethod;

    public ArrayList<FieldData> fieldList = new ArrayList<FieldData>();

    public int md5Order = -1;
    public String md5Name;
    public ArrayList<Md5WhereClauseData> md5WhereClauseDataList = new ArrayList<Md5WhereClauseData>();

    public boolean mHasDumpCode;

    public ClassData(final JSONObject json) throws JSONException {
        name = json.getString("name");
        dbClassName = "Db" + name;
        dbTableName = NameUtil.createLowerCamelCaseName(name);
        dbConstantName = NameUtil.createConstantName(dbTableName);

        plistFile = json.getString("plist_file");

        superclass = json.optString("superclass", null);

        addJsonWriteMethod = json.optBoolean("add_json_write_method", false);

        final JSONArray jsonFieldArray = json.getJSONArray("fields");
        final int jsonFieldArrayLength = jsonFieldArray.length();
        for (int i = 0; i < jsonFieldArrayLength; i++) {
            fieldList.add(new FieldData(jsonFieldArray.getJSONObject(i)));
        }
        fieldList.add(new FieldData(new JSONObject(VERSION_JSON_OBJECT)));

        md5Order = json.optInt("md5_order", -1);
        md5Name = json.optString("md5_name", plistFile);

        final JSONArray jsonMd5WhereClauseArray = json.optJSONArray("md5_where");
        if (jsonMd5WhereClauseArray != null) {
            final int jsonMd5WhereClauseArrayLength = jsonMd5WhereClauseArray.length();
            for (int i = 0; i < jsonMd5WhereClauseArrayLength; i++) {
                try {
                    Md5WhereClauseData whereClause = new Md5WhereClauseData(fieldList, jsonMd5WhereClauseArray.getJSONObject(i));
                    md5WhereClauseDataList.add(whereClause);
                } catch (IllegalArgumentException e) {
                }
            }
        }

        mHasDumpCode = json.optBoolean("has_dump_code", false);
    }

    public static ArrayList<ClassData> getClassesData(final JSONArray jsonClassArray) throws JSONException {
        final ArrayList<ClassData> classDataList = new ArrayList<ClassData>();

        final int jsonClassArrayLength = jsonClassArray.length();
        for (int i = 0; i < jsonClassArrayLength; i++) {
            classDataList.add(new ClassData(jsonClassArray.getJSONObject(i)));
        }

        return classDataList;
    }

    public void generateClass(final String packageName, final String dbClassesPrefix, final String dumpCodeFolder) {
        final CodeBuilder cb = new CodeBuilder(packageName + "." + Config.DATAMODEL_PACKAGE, name, superclass != null ? superclass : "Data");
        if (superclass != null) {
            cb.addImport("com.funzio.framework.services.data." + superclass);
        } else {
            cb.addImport("com.funzio.framework.services.data.Data");
        }

        // Class Fields
        for (FieldData fieldData : fieldList) {
            if (!fieldData.isVersionField) {
                cb.addFieldCustom(true, false, true, fieldData.type, NameUtil.createFieldName(fieldData.name), null);
                if (fieldData.subClass != null) {
                    cb.addFieldCustom(true, false, true, packageName + "." + Config.DATAMODEL_PACKAGE + "." + fieldData.subClass, NameUtil.createFieldName(fieldData.subClass), null);
                }
            }
        }
        cb.addNewLine();

        // Class constructor
        cb.addImport("com.funzio.framework.services.data.source.DataSource");
        cb.addImport(packageName + ".provider." + dbClassesPrefix + "Content." + dbClassName);
        cb.startConstructor(name, "final DataSource ds");
        cb.addLine("super(ds);");
        for (FieldData fieldData : fieldList) {
            if (!fieldData.isVersionField) {
                cb.addLine("%1$s = ds.get%2$s(%3$s.COLUMNS.%4$s);", NameUtil.createFieldName(fieldData.name), fieldData.methodType, dbClassName, fieldData.dbConstantName);
                if (fieldData.subClass != null) {
                    if (fieldData.subClassField.equals("id")) {
                        cb.addLine("%1$s = ds.getObjectFromId(%2$s.class, %3$s);", NameUtil.createFieldName(fieldData.subClass), fieldData.subClass, NameUtil.createFieldName(fieldData.name));
                    } else {
                        cb.addImport(packageName + ".provider." + dbClassesPrefix + "Content.Db" + fieldData.subClass);
                        cb.addImport("com.funzio.framework.services.data.provider.Criteria");
                        cb.addLine("%1$s = ds.getObject(%2$s.class, new Criteria().addEq(Db%2$s.COLUMNS.%3$s, %4$s));", NameUtil.createFieldName(fieldData.subClass), fieldData.subClass,
                                NameUtil.createConstantName(fieldData.subClassField), NameUtil.createFieldName(fieldData.name));
                    }
                }
            }
        }
        cb.endConstructor();

        if (addJsonWriteMethod || dbTableName != null) {
            cb.addImport("com.funzio.framework.services.data.source.DataOutputSource");
            cb.startMethod("void", "writeTo", "final DataOutputSource dos, final String version");
            for (FieldData fieldData : fieldList) {
                cb.addLine("dos.put(%1$s.COLUMNS.%2$s, %3$s);", dbClassName, fieldData.dbConstantName, fieldData.isVersionField ? fieldData.name : NameUtil.createFieldName(fieldData.name));
            }
            cb.endMethod();
        }

        if (mHasDumpCode) {
            loadDumpCode(cb, dumpCodeFolder);
        }

        cb.endClass();

        FileCache.saveFile("output/" + packageName.replace(".", "/") + "/" + Config.DATAMODEL_PACKAGE + "/" + name + ".java", cb.getCode());
    }

    private void loadDumpCode(final CodeBuilder cb, final String dumpClassFolder) {
        final StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(dumpClassFolder + "classes/" + plistFile + ".txt")));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            cb.addCode(sb.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
