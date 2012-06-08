package com.foxykeep.cpcodegenerator.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.foxykeep.cpcodegenerator.FileCache;
import com.foxykeep.cpcodegenerator.model.FieldData;
import com.foxykeep.cpcodegenerator.model.TableData;

public class DatabaseGenerator {

    public static void generate(final String classPackage, final String classesPrefix, final int dbVersion, final ArrayList<TableData> tableDataList) {
        if (classPackage == null || classPackage.length() == 0 || classesPrefix == null || classesPrefix.length() == 0 || tableDataList == null
                || tableDataList.isEmpty()) {
            return;
        }
        generateContentClass(classPackage, classesPrefix, tableDataList);
        generateProviderClass(classPackage, classesPrefix, dbVersion, tableDataList);
    }

    private static void generateContentClass(final String classPackage, final String classesPrefix, final ArrayList<TableData> tableDataList) {

        final StringBuilder sb = new StringBuilder();
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(new File("res/content_class.txt")));
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            final String contentClass = sb.toString();

            sb.setLength(0);
            br = new BufferedReader(new FileReader(new File("res/content_subclass.txt")));
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            final String contentSubClass = sb.toString();

            final StringBuilder sbSubclasses = new StringBuilder();

            final StringBuilder sbEnumFields = new StringBuilder();
            final StringBuilder sbProjection = new StringBuilder();
            final StringBuilder sbCreateTable = new StringBuilder();
            final StringBuilder sbCreateTablePrimaryKey = new StringBuilder();
            final StringBuilder sbIndexes = new StringBuilder();

            boolean hasPreviousPrimaryKey = false;

            for (TableData tableData : tableDataList) {
                sbEnumFields.setLength(0);
                sbProjection.setLength(0);
                sbCreateTable.setLength(0);
                sbCreateTablePrimaryKey.setLength(0);
                sbIndexes.setLength(0);
                hasPreviousPrimaryKey = false;

                final int fieldListSize = tableData.fieldList.size();
                for (int i = 0; i < fieldListSize; i++) {
                    final FieldData fieldData = tableData.fieldList.get(i);
                    final boolean isNotLast = i != fieldListSize - 1;

                    sbEnumFields.append(fieldData.dbConstantName).append("(");
                    if (fieldData.dbIsPrimaryKey) {
                        sbEnumFields.append("BaseColumns._ID)");
                    } else {
                        sbEnumFields.append("\"").append(fieldData.dbName).append("\")");
                    }

                    sbProjection.append("COLUMNS.").append(fieldData.dbConstantName).append(".getColumnName()");

                    sbCreateTable.append("COLUMNS.").append(fieldData.dbConstantName).append(".getColumnName() + \" ").append(fieldData.dbType);
                    if (fieldData.dbIsPrimaryKey) {
                        if (hasPreviousPrimaryKey) {
                            sbCreateTablePrimaryKey.append(" + \", \" + ");
                        }
                        hasPreviousPrimaryKey = true;
                        sbCreateTablePrimaryKey.append("COLUMNS.").append(fieldData.dbConstantName).append(".getColumnName()");
                    }

                    if (fieldData.dbHasIndex) {
                        sbIndexes.append("            db.execSQL(\"CREATE INDEX ").append(tableData.dbTableName).append("_").append(fieldData.dbName)
                                .append(" on \" + TABLE_NAME + \"(\" + COLUMNS.").append(fieldData.dbConstantName)
                                .append(".getColumnName() + \");\");\n");
                    }

                    if (isNotLast) {
                        sbEnumFields.append(", ");
                        sbProjection.append(", ");
                        sbCreateTable.append(", \" + ");
                    } else {
                        sbCreateTable.append("\"");
                    }

                }

                sbSubclasses.append(String.format(contentSubClass, tableData.dbClassName, classesPrefix, tableData.dbTableName,
                        classesPrefix.toLowerCase(), tableData.dbTableName.toLowerCase(), sbEnumFields.toString(), sbProjection.toString(),
                        sbCreateTable.toString(), sbCreateTablePrimaryKey.toString(), sbIndexes.toString()));
            }

            FileCache.saveFile("output/" + classPackage.replace(".", "/") + "/provider/" + classesPrefix + "Content.java",
                    String.format(contentClass, classPackage, classesPrefix, sbSubclasses.toString()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateProviderClass(final String classPackage, final String classesPrefix, final int dbVersion,
            final ArrayList<TableData> tableDataList) {

        final StringBuilder sbImports = new StringBuilder();
        final StringBuilder sbTableConstants = new StringBuilder();
        final StringBuilder sbTableNames = new StringBuilder();
        final StringBuilder sbUriMatcher = new StringBuilder();
        final StringBuilder sbCreateTables = new StringBuilder();
        final StringBuilder sbUpgradeTables = new StringBuilder();
        final StringBuilder sbCaseWithId = new StringBuilder();
        final StringBuilder sbCaseWithoutId = new StringBuilder();
        final StringBuilder sbGetType = new StringBuilder();

        final int tableDataListSize = tableDataList.size();
        for (int i = 0; i < tableDataListSize; i++) {
            final TableData tableData = tableDataList.get(i);
            sbImports.append("import ").append(classPackage).append(".provider.").append(classesPrefix).append("Content.")
                    .append(tableData.dbClassName).append(";\n");

            sbTableConstants.append("    private static final int ").append(tableData.dbConstantName).append("_BASE = 0x")
                    .append(Integer.toHexString(i).toUpperCase()).append("000;\n");
            sbTableConstants.append("    private static final int ").append(tableData.dbConstantName).append(" = ").append(tableData.dbConstantName)
                    .append("_BASE;\n");
            sbTableConstants.append("    private static final int ").append(tableData.dbConstantName).append("_ID = ")
                    .append(tableData.dbConstantName).append("_BASE + 1;\n\n");

            sbTableNames.append(tableData.dbClassName).append(".TABLE_NAME");
            if (i != tableDataListSize - 1) {
                sbTableNames.append(", ");
            }

            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(tableData.dbClassName).append(".TABLE_NAME, ")
                    .append(tableData.dbConstantName).append(");\n");
            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(tableData.dbClassName).append(".TABLE_NAME + \"/#\", ")
                    .append(tableData.dbConstantName).append("_ID);");
            sbUriMatcher.append("\n");

            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(tableData.dbClassName)
                    .append(" | createTable start\");\n            }\n");
            sbCreateTables.append("            ").append(tableData.dbClassName).append(".createTable(db);\n");
            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(tableData.dbClassName)
                    .append(" | createTable end\");\n            }\n");

            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(tableData.dbClassName)
                    .append(" | upgradeTable start\");\n            }\n");
            sbUpgradeTables.append("            ").append(tableData.dbClassName).append(".upgradeTable(db, oldVersion, newVersion);\n");
            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(tableData.dbClassName)
                    .append(" | upgradeTable end\");\n            }\n");

            sbCaseWithId.append("            case ").append(tableData.dbConstantName).append("_ID:\n");
            sbCaseWithoutId.append("            case ").append(tableData.dbConstantName).append(":\n");

            sbGetType.append("            case ").append(tableData.dbConstantName).append("_ID:\n");
            sbGetType.append("                return ").append(tableData.dbClassName).append(".TYPE_ELEM_TYPE;\n");
            sbGetType.append("            case ").append(tableData.dbConstantName).append(":\n");
            sbGetType.append("                return ").append(tableData.dbClassName).append(".TYPE_DIR_TYPE;\n");
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
                "output/" + classPackage.replace(".", "/") + "/provider/" + classesPrefix + "Provider.java",
                String.format(sb.toString(), classPackage, sbImports.toString(), classesPrefix, dbVersion, sbTableConstants.toString(),
                        sbTableNames.toString(), sbUriMatcher.toString(), sbCreateTables.toString(), sbUpgradeTables.toString(),
                        sbCaseWithId.toString(), sbCaseWithoutId.toString(), sbGetType.toString()));

    }
}
