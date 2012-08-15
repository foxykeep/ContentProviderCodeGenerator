package com.foxykeep.cpcodegenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.foxykeep.cpcodegenerator.generator.DatabaseGenerator;
import com.foxykeep.cpcodegenerator.model.TableData;
import com.foxykeep.cpcodegenerator.util.PathUtils;

public class Main {

    public static void main(final String[] args) {

        final File fileInputDir = new File("input");
        if (!fileInputDir.exists() || !fileInputDir.isDirectory()) {
            return;
        }

        String columnMetadataText = "";
        final StringBuilder sb = new StringBuilder();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(new File("res/column_metadata.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            columnMetadataText = sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // For each file in the input folder
        for (File file : fileInputDir.listFiles()) {
            final String fileName = file.getName();
            if (fileName.equals("example")) {
                continue;
            }
            System.out.println("Generating code for " + fileName);

            final char[] buffer = new char[2048];
            sb.setLength(0);
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
                return;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            final String content = sb.toString();
            if (content.length() == 0) {
                System.out.println("file is empty.");
                return;
            }

            try {
                final JSONObject root = new JSONObject(content);
                final JSONObject jsonDatabase = root.getJSONObject("database");

                // Classes generation
                String classPackage = null, classesPrefix = null, dbAuthorityPackage = null;
                int dbVersion = -1;
                classPackage = jsonDatabase.getString("package");
                classesPrefix = jsonDatabase.getString("classes_prefix");
                dbAuthorityPackage = jsonDatabase.getString("authority_package");
                dbVersion = jsonDatabase.getInt("version");

                ArrayList<TableData> classDataList = TableData.getClassesData(root.getJSONArray("tables"));

                // Database generation
                DatabaseGenerator.generate(fileName, classPackage, dbVersion, dbAuthorityPackage, classesPrefix, classDataList);

                FileCache.saveFile(PathUtils.getAndroidFullPath(fileName, classPackage, PathUtils.PROVIDER_UTIL) + "ColumnMetadata.java",
                        String.format(columnMetadataText, classPackage, PathUtils.PROVIDER_UTIL));

            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
