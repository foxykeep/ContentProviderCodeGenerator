package com.foxykeep.databasecodegenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.foxykeep.databasecodegenerator.model.DatabaseTableData;

public class Generator {

    public static void main(final String[] args) {

        final File file = new File("res/classes.json");
        final char[] buffer = new char[2048];
        final StringBuilder sb = new StringBuilder();
        final Reader in;
        try {
            in = new InputStreamReader(new FileInputStream(file), "UTF-8");
            int read;
            do {
                read = in.read(buffer, 0, buffer.length);
                if (read != -1) {
                    sb.append(buffer, 0, read);
                }
            } while (read >= 0);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String content = sb.toString();
        if (content.length() == 0) {
            System.out.println("file is empty");
            return;
        }

        // Classes generation
        ArrayList<DatabaseTableData> databaseTableDataList = new ArrayList<DatabaseTableData>();
        String dbPackage = null, dbClassesPrefix = null;
        int dbVersion = -1;
        try {
            JSONObject root = new JSONObject(content);
            JSONObject jsonDatabase = root.optJSONObject("database");
            if (jsonDatabase != null) {
                dbPackage = jsonDatabase.getString("package");
                dbClassesPrefix = jsonDatabase.getString("database_classes_prefix");
                dbVersion = jsonDatabase.getInt("version");
            }

            final JSONArray jsonTablesArray = root.getJSONArray("tables");
            final int jsonTablesArrayLength = jsonTablesArray.length();
            for (int i = 0; i < jsonTablesArrayLength; i++) {
                databaseTableDataList.add(new DatabaseTableData(jsonTablesArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Database generation
        DatabaseGenerator.generate(dbPackage, dbClassesPrefix, dbVersion, databaseTableDataList);
    }
}
