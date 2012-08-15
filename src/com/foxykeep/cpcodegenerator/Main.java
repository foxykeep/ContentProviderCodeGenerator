package com.foxykeep.cpcodegenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

        // For each file in the input folder
        for (File file : fileInputDir.listFiles()) {
            final String fileName = file.getName();
            if (fileName.equals("example")) {
                continue;
            }
            System.out.println("Generating code for " + fileName);

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
                final String outputColumnMetadataPath = PathUtils.getAndroidFullPath(fileName, classPackage, PathUtils.PROVIDER_UTIL) + "ColumnMetadata.java";
                FileCache.createFileDir(outputColumnMetadataPath);
                FileCache.copyFile(new FileInputStream(new File("res/ColumnMetadata.java")), new FileOutputStream(new File(outputColumnMetadataPath)));
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
