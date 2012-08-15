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
import com.foxykeep.cpcodegenerator.util.PathUtils;

public class DatabaseGenerator {

    private static final String BULK_STRING_VALUE = "            String value;\n";

    private DatabaseGenerator() {

    }

    public static void generate(final String fileName, final String classPackage, final int dbVersion, final String dbAuthorityPackage, final String classesPrefix,
            final ArrayList<TableData> TableDataList) {
        if (classPackage == null || classPackage.length() == 0 || classesPrefix == null || classesPrefix.length() == 0 || TableDataList == null || TableDataList.isEmpty()) {
            return;
        }
        generateContentClass(fileName, classPackage, classesPrefix, TableDataList);
        generateProviderClass(fileName, classPackage, dbVersion, dbAuthorityPackage, classesPrefix, TableDataList);
    }

    private static void generateContentClass(final String fileName, final String classPackage, final String classesPrefix, final ArrayList<TableData> TableDataList) {

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
            final StringBuilder sbBulkFields = new StringBuilder();
            final StringBuilder sbBulkParams = new StringBuilder();
            final StringBuilder sbBulkValues = new StringBuilder();

            boolean hasPreviousPrimaryKey = false, hasTextField = false;
            ;

            for (TableData TableData : TableDataList) {
                sbEnumFields.setLength(0);
                sbProjection.setLength(0);
                sbCreateTable.setLength(0);
                sbCreateTablePrimaryKey.setLength(0);
                sbIndexes.setLength(0);
                sbBulkFields.setLength(0);
                sbBulkParams.setLength(0);
                sbBulkValues.setLength(0);
                hasPreviousPrimaryKey = false;
                hasTextField = false;

                final int fieldListSize = TableData.fieldList.size();
                for (int i = 0; i < fieldListSize; i++) {
                    final FieldData fieldData = TableData.fieldList.get(i);

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
                        sbIndexes.append("            db.execSQL(\"CREATE INDEX ").append(TableData.dbTableName).append("_").append(fieldData.dbName).append(" on \" + TABLE_NAME + \"(\" + COLUMNS.")
                                .append(fieldData.dbConstantName).append(".getColumnName() + \");\");\n");
                    }

                    sbBulkFields.append(".append(").append("COLUMNS.").append(fieldData.dbConstantName).append(".getColumnName())");
                    sbBulkParams.append("?");
                    if (fieldData.dbType.equals("text")) {
                        hasTextField = true;
                        sbBulkValues.append("            value = values.getAsString(").append("COLUMNS.").append(fieldData.dbConstantName).append(".getColumnName());\n");
                        sbBulkValues.append("            stmt.bindString(i++, value != null ? value : \"\");\n");
                    } else if (fieldData.dbType.equals("integer")) {
                        sbBulkValues.append("            stmt.bindLong(i++, values.getAsLong(").append("COLUMNS.").append(fieldData.dbConstantName).append(".getColumnName()));\n");
                    } else if (fieldData.dbType.equals("real")) {
                        sbBulkValues.append("            stmt.bindDouble(i++, values.getAsDouble(").append("COLUMNS.").append(fieldData.dbConstantName).append(".getColumnName()));\n");
                    }

                    if (isNotLast) {
                        sbEnumFields.append(", ");
                        sbProjection.append(", ");
                        sbCreateTable.append(", \" + ");
                        sbBulkFields.append(".append(\", \")");
                        sbBulkParams.append(", ");
                    } else {
                        sbCreateTable.append("\"");
                    }
                }

                sbSubclasses.append(String.format(contentSubClass, TableData.dbClassName, classesPrefix, TableData.dbTableName, classesPrefix.toLowerCase(), TableData.dbTableName.toLowerCase(),
                        sbEnumFields.toString(), sbProjection.toString(), sbCreateTable.toString(), sbCreateTablePrimaryKey.toString(), sbIndexes.toString(), sbBulkFields.toString(),
                        sbBulkParams.toString(), hasTextField ? BULK_STRING_VALUE : "", sbBulkValues.toString()));
            }

            FileCache.saveFile(PathUtils.getAndroidFullPath(fileName, classPackage, PathUtils.PROVIDER) + classesPrefix + "Content.java",
                    String.format(contentClass, classPackage, classesPrefix, sbSubclasses.toString(), PathUtils.PROVIDER, PathUtils.PROVIDER_UTIL));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private static void generateProviderClass(final String fileName, final String classPackage, final int dbVersion, final String dbAuthorityPackage, final String classesPrefix,
            final ArrayList<TableData> TableDataList) {

        final StringBuilder sbImports = new StringBuilder();
        final StringBuilder sbTableConstants = new StringBuilder();
        final StringBuilder sbTableNames = new StringBuilder();
        final StringBuilder sbUriMatcher = new StringBuilder();
        final StringBuilder sbCreateTables = new StringBuilder();
        final StringBuilder sbUpgradeTables = new StringBuilder();
        final StringBuilder sbCaseWithId = new StringBuilder();
        final StringBuilder sbCaseWithoutId = new StringBuilder();
        final StringBuilder sbGetType = new StringBuilder();
        final StringBuilder sbBulk = new StringBuilder();

        String bulkText = "";
        final StringBuilder sb = new StringBuilder();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(new File("res/provider_bulk.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            bulkText = sb.toString();

            sb.setLength(0);
            br = new BufferedReader(new FileReader(new File("res/provider.txt")));
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

        final int TableDataListSize = TableDataList.size();
        for (int i = 0; i < TableDataListSize; i++) {
            final TableData TableData = TableDataList.get(i);

            sbImports.append("import ").append(classPackage).append(".").append(PathUtils.PROVIDER).append(".").append(classesPrefix).append("Content.").append(TableData.dbClassName).append(";\n");

            sbTableConstants.append("    private static final int ").append(TableData.dbConstantName).append("_BASE = 0x").append(Integer.toHexString(i + 1).toUpperCase()).append("000;\n");
            sbTableConstants.append("    private static final int ").append(TableData.dbConstantName).append(" = ").append(TableData.dbConstantName).append("_BASE;\n");
            sbTableConstants.append("    private static final int ").append(TableData.dbConstantName).append("_ID = ").append(TableData.dbConstantName).append("_BASE + 1;\n\n");

            sbTableNames.append(TableData.dbClassName).append(".TABLE_NAME");
            if (i != TableDataListSize - 1) {
                sbTableNames.append(", ");
            }

            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(TableData.dbClassName).append(".TABLE_NAME, ").append(TableData.dbConstantName).append(");\n");
            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(TableData.dbClassName).append(".TABLE_NAME + \"/#\", ").append(TableData.dbConstantName).append("_ID);");
            sbUriMatcher.append("\n");

            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(TableData.dbClassName).append(" | createTable start\");\n            }\n");
            sbCreateTables.append("            ").append(TableData.dbClassName).append(".createTable(db);\n");
            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(TableData.dbClassName).append(" | createTable end\");\n            }\n");

            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(TableData.dbClassName).append(" | upgradeTable start\");\n            }\n");
            sbUpgradeTables.append("            ").append(TableData.dbClassName).append(".upgradeTable(db, oldVersion, newVersion);\n");
            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(TableData.dbClassName).append(" | upgradeTable end\");\n            }\n");

            sbCaseWithId.append("            case ").append(TableData.dbConstantName).append("_ID:\n");
            sbCaseWithoutId.append("            case ").append(TableData.dbConstantName).append(":\n");

            sbGetType.append("            case ").append(TableData.dbConstantName).append("_ID:\n");
            sbGetType.append("                return ").append(TableData.dbClassName).append(".TYPE_ELEM_TYPE;\n");
            sbGetType.append("            case ").append(TableData.dbConstantName).append(":\n");
            sbGetType.append("                return ").append(TableData.dbClassName).append(".TYPE_DIR_TYPE;\n");

            sbBulk.append(String.format(bulkText, TableData.dbConstantName, TableData.dbClassName));
        }

        FileCache.saveFile(PathUtils.getAndroidFullPath(fileName, classPackage, PathUtils.PROVIDER) + classesPrefix + "Provider.java", String.format(sb.toString(), classPackage, sbImports.toString(),
                classesPrefix, dbAuthorityPackage, sbTableConstants.toString(), sbTableNames.toString(), sbUriMatcher.toString(), sbCreateTables.toString(), sbUpgradeTables.toString(),
                sbCaseWithId.toString(), sbCaseWithoutId.toString(), sbGetType.toString(), sbBulk.toString(), PathUtils.PROVIDER, dbVersion));

    }
}
