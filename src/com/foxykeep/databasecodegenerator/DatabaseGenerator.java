package com.foxykeep.databasecodegenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.foxykeep.databasecodegenerator.model.DatabaseTableData;
import com.foxykeep.databasecodegenerator.model.DatabaseTableData.DatabaseColumnData;

public class DatabaseGenerator {

    public static void generate(final String dbPackage, final String classesPrefix, final int dbVersion,
            final ArrayList<DatabaseTableData> databaseTableDataList) {
        if (dbPackage == null || dbPackage.length() == 0 || classesPrefix == null || classesPrefix.length() == 0 || databaseTableDataList == null
                || databaseTableDataList.isEmpty()) {
            return;
        }
        generateContentClass(dbPackage, classesPrefix, databaseTableDataList);
        generateProviderClass(dbPackage, classesPrefix, dbVersion, databaseTableDataList);
    }

    private static void generateContentClass(final String dbPackage, final String classesPrefix,
            final ArrayList<DatabaseTableData> databaseTableDataList) {

        final String contentName = classesPrefix + "Content";
        final String providerName = classesPrefix + "Provider";
        final CodeBuilder cb = new CodeBuilder(dbPackage, contentName, null, true);

        cb.addFieldCustom(true, true, true, "android.net.Uri", "CONTENT_URI", "Uri.parse(\"content://\" + " + providerName + ".AUTHORITY)");
        cb.addNewLine();

        cb.addPrivateConstructor(contentName);

        for (DatabaseTableData databaseTableData : databaseTableDataList) {
            final String columnsName = databaseTableData.className + "Columns";
            cb.startInterface(columnsName);

            final int columnDataListSize = databaseTableData.databaseColumnDataList.size();
            String[] projectionArray = new String[columnDataListSize];
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < columnDataListSize; i++) {
                final DatabaseColumnData columnData = databaseTableData.databaseColumnDataList.get(i);

                cb.addFieldCustom(true, true, true, "String", columnData.constantName, '"' + columnData.name + '"');

                projectionArray[i] = columnData.constantName;

                sb.append("\" + ").append(columnData.constantName).append(" + \" ").append(columnData.type);
                if (columnData.isPrimaryKey) {
                    sb.append(" primary key");
                    if (columnData.type.equals("integer")) {
                        sb.append(" autoincrement");
                    }
                }
                if (i != columnDataListSize - 1) {
                    sb.append(", ");
                }
            }
            cb.endInterface();

            cb.startClass(databaseTableData.className, contentName, columnsName, true, true, true, false);

            cb.addFieldCustom(true, true, true, "String", "TABLE_NAME", '"' + databaseTableData.tableName + '"');
            cb.addFieldCustom(true, true, true, "android.net.Uri", "CONTENT_URI", "Uri.parse(" + contentName + ".CONTENT_URI + \"/\" + TABLE_NAME)");
            cb.addFieldCustom(true, true, true, "String", "TYPE_ELEM_TYPE", '"' + "vnd.android.cursor.item/" + classesPrefix.toLowerCase() + '-'
                    + databaseTableData.tableName.toLowerCase() + '"');
            cb.addFieldCustom(true, true, true, "String", "TYPE_DIR_TYPE", '"' + "vnd.android.cursor.dir/" + classesPrefix.toLowerCase() + '-'
                    + databaseTableData.tableName.toLowerCase() + '"');
            cb.addNewLine();

            cb.addFieldCustom(true, true, true, "String[]", "CONTENT_PROJECTION", "new String[] {" + cb.joinParamsArray(projectionArray) + '}');
            cb.startEnum(true, true, "CONTENT_PROJECTION_COLS");
            cb.addLine(cb.joinParamsArray(projectionArray));
            cb.endEnum();

            cb.addImport("android.database.sqlite.SQLiteDatabase");
            cb.startMethod(true, "void", "createTable", "SQLiteDatabase db");
            cb.addLine("db.execSQL(\"CREATE TABLE %1$s (%2$s);\");", databaseTableData.tableName, sb.toString());
            cb.addNewLine();
            for (int i = 0; i < columnDataListSize; i++) {
                final DatabaseColumnData columnData = databaseTableData.databaseColumnDataList.get(i);
                if (columnData.isIndex) {
                    cb.addImport("com.foxykeep.datadroid.provider.util.DatabaseUtil");
                    cb.addLine("db.execSQL(DatabaseUtil.getCreateIndexString(TABLE_NAME, %1$s));", columnData.constantName);
                }
            }
            cb.endMethod();

            cb.startMethod(true, "void", "upgradeTable", "SQLiteDatabase db", "int oldVersion", "int newVersion");
            cb.addLine("db.execSQL(\"DROP TABLE IF EXISTS \" + TABLE_NAME + \";\");");
            cb.addLine("%1$s.createTable(db);", databaseTableData.className);
            cb.endMethod();

            if (databaseTableData.hasBulkInsert) {
                sb.setLength(0);
                sb.append("\"INSERT INTO \" + TABLE_NAME + \" ( \"");
                for (int i = 0; i < columnDataListSize; i++) {
                    final DatabaseColumnData columnData = databaseTableData.databaseColumnDataList.get(i);
                    sb.append(" + ").append(columnData.constantName);
                    if (i != columnDataListSize - 1) {
                        sb.append(" + \", \"");
                    }
                }
                sb.append("+ \" ) VALUES (");
                for (int i = 0; i < columnDataListSize; i++) {
                    sb.append("?");
                    if (i != columnDataListSize - 1) {
                        sb.append(",");
                    }
                }
                sb.append(")\"");
                cb.addFieldCustom(true, true, true, "String", "BULK_INSERT_STRING", sb.toString());
                cb.addNewLine();

                cb.addImport("android.database.sqlite.SQLiteStatement");
                cb.addImport("android.content.ContentValues");
                cb.startMethod(true, "void", "bindValuesInBulkInsert", "SQLiteStatement stmt", "ContentValues values");
                for (int i = 0; i < columnDataListSize; i++) {
                    final DatabaseColumnData columnData = databaseTableData.databaseColumnDataList.get(i);
                    if (columnData.type.equals("text")) {
                        final String var = cb.createNewVar("s");
                        cb.addLine("final String %1$s = values.getAsString(%2$s);", var, columnData.constantName);
                        cb.addLine("stmt.bindString(%1$s, %2$s != null ? %2$s : \"\");", i + 1, var);
                    } else {
                        cb.addLine("stmt.bind%1$s(%2$s, values.getAs%1$s(%3$s));", columnData.bulkType, i + 1, columnData.constantName);
                    }
                }
                cb.endMethod();
            }
            cb.endClass();
        }
        cb.endClass();

        FileCache.saveFile("output/" + dbPackage.replace(".", "/") + "/" + contentName + ".java", cb.getCode());
    }

    private static void generateProviderClass(final String dbPackage, final String classesPrefix, final int dbVersion,
            final ArrayList<DatabaseTableData> databaseTableDataList) {

        final String contentName = classesPrefix + "Content";
        final String providerName = classesPrefix + "Provider";

        final StringBuilder sbImports = new StringBuilder();
        final StringBuilder sbTableConstants = new StringBuilder();
        final StringBuilder sbTableNames = new StringBuilder();
        final StringBuilder sbUriMatcher = new StringBuilder();
        final StringBuilder sbCreateTables = new StringBuilder();
        final StringBuilder sbUpgradeTables = new StringBuilder();
        final StringBuilder sbCaseWithId = new StringBuilder();
        final StringBuilder sbCaseWithoutId = new StringBuilder();
        final StringBuilder sbGetType = new StringBuilder();
        final StringBuilder sbBulkInsert = new StringBuilder();

        final int databaseTableDataListSize = databaseTableDataList.size();
        for (int i = 0; i < databaseTableDataListSize; i++) {
            final DatabaseTableData databaseTableData = databaseTableDataList.get(i);
            sbImports.append("import ").append(dbPackage).append(".").append(contentName).append(".").append(databaseTableData.className)
                    .append(";\n");

            sbTableConstants.append("    private static final int ").append(databaseTableData.tableConstantName).append("_BASE = 0x").append(i)
                    .append("000;\n");
            sbTableConstants.append("    private static final int ").append(databaseTableData.tableConstantName).append(" = ")
                    .append(databaseTableData.tableConstantName).append("_BASE;\n");
            sbTableConstants.append("    private static final int ").append(databaseTableData.tableConstantName).append("_ID = ")
                    .append(databaseTableData.tableConstantName).append("_BASE + 1;\n\n");

            sbTableNames.append(databaseTableData.className).append(".TABLE_NAME");
            if (i != databaseTableDataListSize - 1) {
                sbTableNames.append(", ");
            }

            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(databaseTableData.className).append(".TABLE_NAME, ")
                    .append(databaseTableData.tableConstantName).append(");\n");
            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(databaseTableData.className).append(".TABLE_NAME + \"/#\", ")
                    .append(databaseTableData.tableConstantName).append("_ID);\n\n");

            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(databaseTableData.className)
                    .append(" | createTable start\");\n            }\n");
            sbCreateTables.append("            ").append(databaseTableData.className).append(".createTable(db);\n");
            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(databaseTableData.className)
                    .append(" | createTable end\");\n            }\n");

            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(databaseTableData.className)
                    .append(" | upgradeTable start\");\n            }\n");
            sbUpgradeTables.append("            ").append(databaseTableData.className).append(".upgradeTable(db, oldVersion, newVersion);\n");
            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(databaseTableData.className)
                    .append(" | upgradeTable end\");\n            }\n");

            sbCaseWithId.append("            case ").append(databaseTableData.tableConstantName).append("_ID:\n");
            sbCaseWithoutId.append("            case ").append(databaseTableData.tableConstantName).append(":\n");

            sbGetType.append("            case ").append(databaseTableData.tableConstantName).append("_ID:\n");
            sbGetType.append("                return ").append(databaseTableData.className).append(".TYPE_ELEM_TYPE;\n");
            sbGetType.append("            case ").append(databaseTableData.tableConstantName).append(":\n");
            sbGetType.append("                return ").append(databaseTableData.className).append(".TYPE_DIR_TYPE;\n");

            if (databaseTableData.hasBulkInsert) {
                sbBulkInsert.append("            case ").append(databaseTableData.tableConstantName).append(":\n");
                sbBulkInsert.append("                insertStmt = db.compileStatement(").append(databaseTableData.className)
                        .append(".BULK_INSERT_STRING);\n");
                sbBulkInsert.append("                for (final ContentValues value : values) {\n");
                sbBulkInsert.append("                    ").append(databaseTableData.className)
                        .append(".bindValuesInBulkInsert(insertStmt, value);\n");
                sbBulkInsert
                        .append("                    insertStmt.execute();\n                insertStmt.clearBindings();\n            }")
                        .append("\n            insertStmt.close();\n            db.setTransactionSuccessful();\n            numberInserted = values.length;\n            break;\n");
            }
        }

        final StringBuilder sb = new StringBuilder();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(new File("res/provider.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        FileCache.saveFile(
                "output/" + dbPackage.replace(".", "/") + "/" + providerName + ".java",
                String.format(sb.toString(), dbPackage, sbImports.toString(), providerName, dbVersion, sbTableConstants.toString(),
                        sbTableNames.toString(), sbUriMatcher.toString(), sbCreateTables.toString(), sbUpgradeTables.toString(),
                        sbCaseWithId.toString(), sbCaseWithoutId.toString(), sbGetType.toString(), sbBulkInsert.toString(), contentName));

    }
}
