package com.foxykeep.cpcodegenerator.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.foxykeep.cpcodegenerator.FileCache;
import com.foxykeep.cpcodegenerator.model.ClassData;
import com.foxykeep.cpcodegenerator.model.FieldData;

public class DatabaseGenerator {

    public static void generate(final String classPackage, final String classesPrefix, final int dbVersion, final ArrayList<ClassData> classDataList) {
        if (classPackage == null || classPackage.length() == 0 || classesPrefix == null || classesPrefix.length() == 0 || classDataList == null
                || classDataList.isEmpty()) {
            return;
        }
        generateContentClass(classPackage, classesPrefix, classDataList);
        generateProviderClass(classPackage, classesPrefix, dbVersion, classDataList);
    }

    private static void generateContentClass(final String classPackage, final String classesPrefix, final ArrayList<ClassData> classDataList) {

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

            for (ClassData classData : classDataList) {
                sbEnumFields.setLength(0);
                sbProjection.setLength(0);
                sbCreateTable.setLength(0);
                sbCreateTablePrimaryKey.setLength(0);
                sbIndexes.setLength(0);
                hasPreviousPrimaryKey = false;

                final int fieldListSize = classData.fieldList.size();
                for (int i = 0; i < fieldListSize; i++) {
                    final FieldData fieldData = classData.fieldList.get(i);
                    final boolean isNotLast = i != fieldListSize - 1;

                    sbEnumFields.append(fieldData.dbConstantName).append("(");
                    if (fieldData.dbIsPrimaryKey) {
                        sbEnumFields.append("BaseColumns._ID, \"").append(fieldData.jsonField).append("\")");
                    } else {
                        sbEnumFields.append("\"").append(fieldData.dbName).append("\", \"").append(fieldData.jsonField).append("\")");
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
                        sbIndexes.append("            db.execSQL(\"CREATE INDEX ").append(classData.dbTableName).append("_").append(fieldData.dbName)
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

                sbSubclasses.append(String.format(contentSubClass, classData.dbClassName, classesPrefix, classData.dbTableName,
                        classesPrefix.toLowerCase(), classData.dbTableName.toLowerCase(), sbEnumFields.toString(), sbProjection.toString(),
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
            final ArrayList<ClassData> classDataList) {

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

        final int classDataListSize = classDataList.size();
        for (int i = 0; i < classDataListSize; i++) {
            final ClassData classData = classDataList.get(i);
            sbImports.append("import ").append(classPackage).append(".provider.").append(contentName).append(".").append(classData.dbClassName)
                    .append(";\n");

            sbTableConstants.append("    private static final int ").append(classData.dbConstantName).append("_BASE = 0x")
                    .append(Integer.toHexString(i + 1).toUpperCase()).append("000;\n");
            sbTableConstants.append("    private static final int ").append(classData.dbConstantName).append(" = ").append(classData.dbConstantName)
                    .append("_BASE;\n");
            sbTableConstants.append("    private static final int ").append(classData.dbConstantName).append("_ID = ")
                    .append(classData.dbConstantName).append("_BASE + 1;\n\n");

            sbTableNames.append(classData.dbClassName).append(".TABLE_NAME");
            if (i != classDataListSize - 1) {
                sbTableNames.append(", ");
            }

            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(classData.dbClassName).append(".TABLE_NAME, ")
                    .append(classData.dbConstantName).append(");\n");
            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(classData.dbClassName).append(".TABLE_NAME + \"/#\", ")
                    .append(classData.dbConstantName).append("_ID);");
            sbUriMatcher.append("\n");

            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(classData.dbClassName)
                    .append(" | createTable start\");\n            }\n");
            sbCreateTables.append("            ").append(classData.dbClassName).append(".createTable(db);\n");
            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(classData.dbClassName)
                    .append(" | createTable end\");\n            }\n");

            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(classData.dbClassName)
                    .append(" | upgradeTable start\");\n            }\n");
            sbUpgradeTables.append("            ").append(classData.dbClassName).append(".upgradeTable(db, oldVersion, newVersion);\n");
            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(classData.dbClassName)
                    .append(" | upgradeTable end\");\n            }\n");

            sbCaseWithId.append("            case ").append(classData.dbConstantName).append("_ID:\n");
            sbCaseWithoutId.append("            case ").append(classData.dbConstantName).append(":\n");

            sbGetType.append("            case ").append(classData.dbConstantName).append("_ID:\n");
            sbGetType.append("                return ").append(classData.dbClassName).append(".TYPE_ELEM_TYPE;\n");
            sbGetType.append("            case ").append(classData.dbConstantName).append(":\n");
            sbGetType.append("                return ").append(classData.dbClassName).append(".TYPE_DIR_TYPE;\n");
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
                "output/" + classPackage.replace(".", "/") + "/provider/" + providerName + ".java",
                String.format(sb.toString(), classPackage, sbImports.toString(), providerName, dbVersion, sbTableConstants.toString(),
                        sbTableNames.toString(), sbUriMatcher.toString(), sbCreateTables.toString(), sbUpgradeTables.toString(),
                        sbCaseWithId.toString(), sbCaseWithoutId.toString(), sbGetType.toString(), contentName));

    }
}
