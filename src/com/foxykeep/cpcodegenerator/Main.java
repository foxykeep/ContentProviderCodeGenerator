package com.foxykeep.cpcodegenerator;

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

import com.foxykeep.cpcodegenerator.generator.DatabaseGenerator;
import com.foxykeep.cpcodegenerator.model.ClassData;

public class Main {

    private static final String DUMP_CODE_ROOT_FOLDER = "res/dumpcode/";

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
            System.out.println("file is empty.");
            return;
        }

        try {
            final JSONObject root = new JSONObject(content);
            final JSONArray jsonGames = root.getJSONArray("games");

            final int jsonGamesLength = jsonGames.length();
            for (int i = 0; i < jsonGamesLength; i++) {
                JSONObject gamesRoot = jsonGames.getJSONObject(i);

                // Classes generation
                String classPackage = null, classesShortcut = null, classesPrefix = null, dumpCodeFolder = null;
                int dbVersion = -1;
                classPackage = gamesRoot.getString("package");
                classesShortcut = gamesRoot.getString("classes_shortcut");
                classesPrefix = gamesRoot.getString("classes_prefix");
                dbVersion = gamesRoot.getInt("version");
                dumpCodeFolder = DUMP_CODE_ROOT_FOLDER + gamesRoot.getString("dump_code_folder") + "/";
                final boolean hasDumpCode = gamesRoot.optBoolean("has_dump_code", false);

                ArrayList<ClassData> classDataList = ClassData.getClassesData(gamesRoot.getJSONArray("classes"));

                for (ClassData classData : classDataList) {
                    classData.generateClass(classPackage, classesPrefix, dumpCodeFolder);
                }

                // Database generation
                DatabaseGenerator.generate(classPackage, classesPrefix, dbVersion, classDataList);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
