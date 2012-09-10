package com.foxykeep.cpcodegenerator.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.foxykeep.cpcodegenerator.FileCache;
import com.foxykeep.cpcodegenerator.model.FieldData;
import com.foxykeep.cpcodegenerator.model.TableData;
import com.foxykeep.cpcodegenerator.util.PathUtils;

public class DatabaseGenerator {

    private static final String BULK_STRING_VALUE = "            String value;\n";
    private static final String PRIMARY_KEY_FORMAT = " + \", PRIMARY KEY (\" + %1$s + \")\"";

    private static final String UPGRADE_VERSION_COMMENT_NOTHING = "        // Version %1$d : No changes\n";
    private static final String UPGRADE_VERSION_COMMENT_NOTHING_MULTI = "        // Version %1$d - %2$d : No changes\n";
    private static final String UPGRADE_VERSION_COMMENT_FIELD = "        // Version %1$d : Add field%3$s %2$s\n";
    private static final String UPGRADE_VERSION_JUMP_TO_LATEST = "\n            if (oldVersion < newVersion) {\n                // No more changes since version %1$d so jump to newVersion\n                oldVersion = newVersion;\n            }";

    private static final String PROVIDER_UPGRADE_VERSION_VERSION = "    // Version %1$d : %2$s\n";
    private static final String PROVIDER_UPGRADE_VERSION_MULTI = "    // Version %1$d - %2$d : %3$s\n";
    private static final String PROVIDER_UPGRADE_VERSION_OTHER = "    //             %1$s\n";
    private static final String PROVIDER_UPGRADE_ADD_TABLE = "Add table %1$s";
    private static final String PROVIDER_UPGRADE_ADD_FIELD = "Add field%3$s %1$s in table %2$s";
    private static final String PROVIDER_UPGRADE_NO_CHANGES = "No changes";

    private DatabaseGenerator() {

    }

    public static void generate(final String fileName, final String classPackage, final int dbVersion, final String dbAuthorityPackage,
            final String classesPrefix, final ArrayList<TableData> tableDataList, final String providerFolder) {
        if (classPackage == null || classPackage.length() == 0 || classesPrefix == null || classesPrefix.length() == 0 || tableDataList == null
                || tableDataList.isEmpty()) {
            return;
        }
        generateContentClass(fileName, classPackage, classesPrefix, tableDataList, dbVersion, providerFolder);
        generateProviderClass(fileName, classPackage, dbVersion, dbAuthorityPackage, classesPrefix, tableDataList, providerFolder);
    }

    private static void generateContentClass(final String fileName, final String classPackage, final String classesPrefix,
            final ArrayList<TableData> tableDataList, final int dbVersion, final String providerFolder) {

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

            sb.setLength(0);
            br = new BufferedReader(new FileReader(new File("res/content_subclass_upgrade.txt")));
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            final String contentSubClassUpgrade = sb.toString();

            final StringBuilder sbSubclasses = new StringBuilder();

            final StringBuilder sbEnumFields = new StringBuilder();
            final StringBuilder sbProjection = new StringBuilder();
            final StringBuilder sbCreateTable = new StringBuilder();
            final StringBuilder sbCreateTablePrimaryKey = new StringBuilder();
            final StringBuilder sbUpgradeTableComment = new StringBuilder();
            final StringBuilder sbUpgradeTableCommentNewFields = new StringBuilder();
            final StringBuilder sbUpgradeTable = new StringBuilder();
            final StringBuilder sbUpgradeTableCreateTmpTable = new StringBuilder();
            final StringBuilder sbUpgradeTableCreateTmpTablePrimaryKey = new StringBuilder();
            final StringBuilder sbUpgradeTableInsertFields = new StringBuilder();
            final StringBuilder sbUpgradeTableInsertDefaultValues = new StringBuilder();
            final StringBuilder sbIndexes = new StringBuilder();
            final StringBuilder sbBulkFields = new StringBuilder();
            final StringBuilder sbBulkParams = new StringBuilder();
            final StringBuilder sbBulkValues = new StringBuilder();

            boolean hasPreviousPrimaryKey = false, hasPreviousInsertFields = false, hasPreviousInsertDefaultValues = false, hasTextField = false, hasPreviousUpgradeElements = true;
            int maxUpgradeVersion = 1, minUpgradeWithoutChanges = 1;

            for (TableData tableData : tableDataList) {
                sbEnumFields.setLength(0);
                sbProjection.setLength(0);
                sbCreateTable.setLength(0);
                sbCreateTablePrimaryKey.setLength(0);
                sbUpgradeTableComment.setLength(0);
                sbUpgradeTable.setLength(0);
                sbIndexes.setLength(0);
                sbBulkFields.setLength(0);
                sbBulkParams.setLength(0);
                sbBulkValues.setLength(0);
                hasPreviousPrimaryKey = false;
                hasTextField = false;

                for (int i = 0, n = tableData.fieldList.size(); i < n; i++) {
                    final FieldData fieldData = tableData.fieldList.get(i);

                    final boolean isNotLast = i != n - 1;

                    sbEnumFields.append(fieldData.dbConstantName).append("(");
                    if (fieldData.dbIsPrimaryKey) {
                        sbEnumFields.append("BaseColumns._ID");
                    } else {
                        sbEnumFields.append("\"").append(fieldData.dbName).append("\"");
                    }
                    sbEnumFields.append(", \"").append(fieldData.dbType).append("\")");

                    sbProjection.append("Columns.").append(fieldData.dbConstantName).append(".getName()");

                    sbCreateTable.append("Columns.").append(fieldData.dbConstantName).append(".getName() + \" \" + ").append("Columns.")
                            .append(fieldData.dbConstantName).append(".getType()");
                    if (fieldData.dbIsPrimaryKey) {
                        if (hasPreviousPrimaryKey) {
                            sbCreateTablePrimaryKey.append(" + \", \" + ");
                        }
                        hasPreviousPrimaryKey = true;
                        sbCreateTablePrimaryKey.append("Columns.").append(fieldData.dbConstantName).append(".getName()");
                    }

                    if (fieldData.dbHasIndex) {
                        sbIndexes.append("            db.execSQL(\"CREATE INDEX ").append(tableData.dbTableName).append("_").append(fieldData.dbName)
                                .append(" on \" + TABLE_NAME + \"(\" + Columns.").append(fieldData.dbConstantName).append(".getName() + \");\");\n");
                    }

                    sbBulkFields.append(".append(").append("Columns.").append(fieldData.dbConstantName).append(".getName())");
                    sbBulkParams.append("?");
                    if (fieldData.dbType.equals("text")) {
                        hasTextField = true;
                        sbBulkValues.append("            value = values.getAsString(").append("Columns.").append(fieldData.dbConstantName)
                                .append(".getName());\n");
                        sbBulkValues.append("            stmt.bindString(i++, value != null ? value : \"\");\n");
                    } else if (fieldData.dbType.equals("integer")) {
                        sbBulkValues.append("            stmt.bindLong(i++, values.getAsLong(").append("Columns.").append(fieldData.dbConstantName)
                                .append(".getName()));\n");
                    } else if (fieldData.dbType.equals("real")) {
                        sbBulkValues.append("            stmt.bindDouble(i++, values.getAsDouble(").append("Columns.").append(fieldData.dbConstantName)
                                .append(".getName()));\n");
                    }

                    if (isNotLast) {
                        sbEnumFields.append(", ");
                        sbProjection.append(", ");
                        sbCreateTable.append(" + \", \" + ");
                        sbBulkFields.append(".append(\", \")");
                        sbBulkParams.append(", ");
                    }
                }

                // Upgrade management
                maxUpgradeVersion = tableData.version;
                minUpgradeWithoutChanges = -1;
                for (int currentVersion = tableData.version + 1, n = dbVersion; currentVersion <= n; currentVersion++) {
                    final List<FieldData> upgradeFieldDataList = tableData.upgradeFieldMap.get(currentVersion);
                    if (upgradeFieldDataList == null) {
                        if (minUpgradeWithoutChanges == -1) {
                            minUpgradeWithoutChanges = currentVersion;
                        }
                        continue;
                    } else if (minUpgradeWithoutChanges != -1) {
                        if (minUpgradeWithoutChanges == currentVersion - 1) {
                            // Only one without change
                            sbUpgradeTableComment.append(String.format(UPGRADE_VERSION_COMMENT_NOTHING, minUpgradeWithoutChanges));
                        } else {
                            // Multiple versions with changes
                            sbUpgradeTableComment.append(String.format(UPGRADE_VERSION_COMMENT_NOTHING_MULTI, minUpgradeWithoutChanges, currentVersion - 1));
                        }

                        minUpgradeWithoutChanges = -1;
                    }

                    maxUpgradeVersion = currentVersion;

                    sbUpgradeTableCommentNewFields.setLength(0);
                    sbUpgradeTableCreateTmpTable.setLength(0);
                    sbUpgradeTableCreateTmpTablePrimaryKey.setLength(0);
                    sbUpgradeTableInsertFields.setLength(0);
                    sbUpgradeTableInsertDefaultValues.setLength(0);
                    hasPreviousUpgradeElements = false;
                    hasPreviousInsertFields = false;
                    hasPreviousInsertDefaultValues = false;
                    hasPreviousPrimaryKey = false;

                    for (FieldData fieldData : tableData.fieldList) {
                        if (fieldData.version > currentVersion) {
                            // This field doesn't exist yet in this version
                            continue;
                        }

                        if (hasPreviousUpgradeElements) {
                            sbUpgradeTableCreateTmpTable.append(" + \", \" + ");
                        }
                        hasPreviousUpgradeElements = true;

                        sbUpgradeTableCreateTmpTable.append("Columns.").append(fieldData.dbConstantName).append(".getName() + \" \" + ").append("Columns.")
                                .append(fieldData.dbConstantName).append(".getType()");
                        if (fieldData.dbIsPrimaryKey) {
                            if (hasPreviousPrimaryKey) {
                                sbUpgradeTableCreateTmpTablePrimaryKey.append(" + \", \" + ");
                            }
                            hasPreviousPrimaryKey = true;
                            sbUpgradeTableCreateTmpTablePrimaryKey.append("Columns.").append(fieldData.dbConstantName).append(".getName()");
                        }

                        if (fieldData.version < currentVersion) {
                            // The field is an old one and is added to the insert list
                            if (hasPreviousInsertFields) {
                                sbUpgradeTableInsertFields.append(" + \", \" + ");
                            }
                            hasPreviousInsertFields = true;

                            sbUpgradeTableInsertFields.append("Columns.").append(fieldData.dbConstantName).append(".getName() + \" \" + ").append("Columns.")
                                    .append(fieldData.dbConstantName).append(".getType()");
                        } else {
                            // This field is a new one. so default value
                            if (hasPreviousInsertDefaultValues) {
                                sbUpgradeTableInsertDefaultValues.append(", ");
                            }
                            hasPreviousInsertDefaultValues = true;

                            sbUpgradeTableInsertDefaultValues.append(FieldData.getDefaultValue(fieldData.type));
                        }
                    }

                    sbUpgradeTable.append(String.format(contentSubClassUpgrade, currentVersion, sbUpgradeTableCreateTmpTable.toString(),
                            hasPreviousPrimaryKey ? String.format(PRIMARY_KEY_FORMAT, sbUpgradeTableCreateTmpTablePrimaryKey.toString()) : "",
                            sbUpgradeTableInsertFields.toString(), sbUpgradeTableInsertDefaultValues.toString()));

                    hasPreviousUpgradeElements = false;
                    for (FieldData fieldData : upgradeFieldDataList) {
                        if (hasPreviousUpgradeElements) {
                            sbUpgradeTableCommentNewFields.append(", ");
                        }
                        hasPreviousUpgradeElements = true;

                        sbUpgradeTableCommentNewFields.append(fieldData.dbConstantName);
                    }
                    sbUpgradeTableComment.append(String.format(UPGRADE_VERSION_COMMENT_FIELD, currentVersion, sbUpgradeTableCommentNewFields.toString(),
                            upgradeFieldDataList.size() > 1 ? "s" : ""));
                }

                // No more changes for the last versions so add the code to jump to the latest version
                if (maxUpgradeVersion != dbVersion) {
                    sbUpgradeTable.append(String.format(UPGRADE_VERSION_JUMP_TO_LATEST, maxUpgradeVersion));

                    if (maxUpgradeVersion == dbVersion - 1) {
                        // Only one without change
                        sbUpgradeTableComment.append(String.format(UPGRADE_VERSION_COMMENT_NOTHING, maxUpgradeVersion + 1));
                    } else {
                        // Multiple versions with changes
                        sbUpgradeTableComment.append(String.format(UPGRADE_VERSION_COMMENT_NOTHING_MULTI, maxUpgradeVersion + 1, dbVersion));
                    }
                }

                sbSubclasses.append(String.format(contentSubClass, tableData.dbClassName, classesPrefix, tableData.dbTableName, classesPrefix.toLowerCase(),
                        tableData.dbTableName.toLowerCase(), sbEnumFields.toString(), sbProjection.toString(), sbCreateTable.toString(),
                        hasPreviousPrimaryKey ? String.format(PRIMARY_KEY_FORMAT, sbCreateTablePrimaryKey.toString()) : "", sbIndexes.toString(), sbBulkFields
                                .toString(), sbBulkParams.toString(), hasTextField ? BULK_STRING_VALUE : "", sbBulkValues.toString(), tableData.version,
                        sbUpgradeTableComment.toString(), sbUpgradeTable.toString()));
            }

            FileCache.saveFile(PathUtils.getAndroidFullPath(fileName, classPackage, providerFolder) + classesPrefix + "Content.java",
                    String.format(contentClass, classPackage, classesPrefix, sbSubclasses.toString(), providerFolder, providerFolder + "." + PathUtils.UTIL));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private static void generateProviderClass(final String fileName, final String classPackage, final int dbVersion, final String dbAuthorityPackage,
            final String classesPrefix, final ArrayList<TableData> tableDataList, final String providerFolder) {

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
        final StringBuilder sbUpgradeDatabaseComment = new StringBuilder();
        final StringBuilder sbUpgradeDatabaseCommentFields = new StringBuilder();

        int minUpgradeWithoutChanges = 1;

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

        final int TableDataListSize = tableDataList.size();
        for (int i = 0; i < TableDataListSize; i++) {
            final TableData TableData = tableDataList.get(i);

            sbImports.append("import ").append(classPackage).append(".").append(providerFolder).append(".").append(classesPrefix).append("Content.")
                    .append(TableData.dbClassName).append(";\n");

            sbTableConstants.append("    private static final int ").append(TableData.dbConstantName).append("_BASE = 0x")
                    .append(Integer.toHexString(i).toUpperCase()).append("000;\n");
            sbTableConstants.append("    private static final int ").append(TableData.dbConstantName).append(" = ").append(TableData.dbConstantName)
                    .append("_BASE;\n");
            sbTableConstants.append("    private static final int ").append(TableData.dbConstantName).append("_ID = ").append(TableData.dbConstantName)
                    .append("_BASE + 1;\n\n");

            sbTableNames.append(TableData.dbClassName).append(".TABLE_NAME");
            if (i != TableDataListSize - 1) {
                sbTableNames.append(", ");
            }

            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(TableData.dbClassName).append(".TABLE_NAME, ").append(TableData.dbConstantName)
                    .append(");\n");
            sbUriMatcher.append("        matcher.addURI(AUTHORITY, ").append(TableData.dbClassName).append(".TABLE_NAME + \"/#\", ")
                    .append(TableData.dbConstantName).append("_ID);");
            sbUriMatcher.append("\n");

            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(TableData.dbClassName)
                    .append(" | createTable start\");\n            }\n");
            sbCreateTables.append("            ").append(TableData.dbClassName).append(".createTable(db);\n");
            sbCreateTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(TableData.dbClassName)
                    .append(" | createTable end\");\n            }\n");

            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(TableData.dbClassName)
                    .append(" | upgradeTable start\");\n            }\n");
            sbUpgradeTables.append("            ").append(TableData.dbClassName).append(".upgradeTable(db, oldVersion, newVersion);\n");
            sbUpgradeTables.append("            if (ACTIVATE_ALL_LOGS) {\n                Log.d(LOG_TAG, \"").append(TableData.dbClassName)
                    .append(" | upgradeTable end\");\n            }\n");

            sbCaseWithId.append("            case ").append(TableData.dbConstantName).append("_ID:\n");
            sbCaseWithoutId.append("            case ").append(TableData.dbConstantName).append(":\n");

            sbGetType.append("            case ").append(TableData.dbConstantName).append("_ID:\n");
            sbGetType.append("                return ").append(TableData.dbClassName).append(".TYPE_ELEM_TYPE;\n");
            sbGetType.append("            case ").append(TableData.dbConstantName).append(":\n");
            sbGetType.append("                return ").append(TableData.dbClassName).append(".TYPE_DIR_TYPE;\n");

            sbBulk.append(String.format(bulkText, TableData.dbConstantName, TableData.dbClassName));
        }

        // Upgrade comments in the provider
        minUpgradeWithoutChanges = -1;
        for (int currentVersion = 2; currentVersion <= dbVersion; currentVersion++) {
            sbUpgradeDatabaseCommentFields.setLength(0);

            boolean firstElem = true;
            for (TableData tableData : tableDataList) {

                if (tableData.version == currentVersion) {
                    appendUpgradeDatabaseComment(sbUpgradeDatabaseComment, firstElem, minUpgradeWithoutChanges, currentVersion,
                            String.format(PROVIDER_UPGRADE_ADD_TABLE, tableData.dbClassName));
                    firstElem = false;
                    minUpgradeWithoutChanges = -1;
                }

                final List<FieldData> upgradeFieldList = tableData.upgradeFieldMap.get(currentVersion);
                if (upgradeFieldList != null) {
                    boolean firstField = true;
                    for (FieldData fieldData : upgradeFieldList) {
                        if (!firstField) {
                            sbUpgradeDatabaseCommentFields.append(", ");
                        }
                        firstField = false;
                        sbUpgradeDatabaseCommentFields.append(fieldData.dbConstantName);
                    }

                    appendUpgradeDatabaseComment(sbUpgradeDatabaseComment, firstElem, minUpgradeWithoutChanges, currentVersion, String.format(
                            PROVIDER_UPGRADE_ADD_FIELD, sbUpgradeDatabaseCommentFields.toString(), tableData.dbClassName, upgradeFieldList.size() > 1 ? "s"
                                    : ""));
                    firstElem = false;
                    minUpgradeWithoutChanges = -1;
                }
            }

            // No changes in this version
            if (firstElem && minUpgradeWithoutChanges == -1) {
                minUpgradeWithoutChanges = currentVersion;
            }
        }

        if (minUpgradeWithoutChanges != -1) {
            if (minUpgradeWithoutChanges == dbVersion) {
                // Only one without change
                sbUpgradeDatabaseComment.append(String.format(PROVIDER_UPGRADE_VERSION_VERSION, minUpgradeWithoutChanges, PROVIDER_UPGRADE_NO_CHANGES));
            } else {
                // Multiple versions with changes
                sbUpgradeDatabaseComment
                        .append(String.format(PROVIDER_UPGRADE_VERSION_MULTI, minUpgradeWithoutChanges, dbVersion, PROVIDER_UPGRADE_NO_CHANGES));
            }
        }

        FileCache.saveFile(PathUtils.getAndroidFullPath(fileName, classPackage, providerFolder) + classesPrefix + "Provider.java", String.format(sb.toString(),
                classPackage, sbImports.toString(), classesPrefix, dbAuthorityPackage, sbTableConstants.toString(), sbTableNames.toString(),
                sbUriMatcher.toString(), sbCreateTables.toString(), sbUpgradeTables.toString(), sbCaseWithId.toString(), sbCaseWithoutId.toString(),
                sbGetType.toString(), sbBulk.toString(), providerFolder, dbVersion, sbUpgradeDatabaseComment.toString()));

    }

    private static void appendUpgradeDatabaseComment(final StringBuilder sb, final boolean firstElem, final int minUpgradeWithoutChanges,
            final int currentVersion, final String content) {

        if (minUpgradeWithoutChanges != -1) {
            if (minUpgradeWithoutChanges == currentVersion - 1) {
                sb.append(String.format(PROVIDER_UPGRADE_VERSION_VERSION, currentVersion - 1, PROVIDER_UPGRADE_NO_CHANGES));
            } else {
                sb.append(String.format(PROVIDER_UPGRADE_VERSION_MULTI, minUpgradeWithoutChanges, currentVersion - 1, PROVIDER_UPGRADE_NO_CHANGES));
            }
        }

        if (firstElem) {
            sb.append(String.format(PROVIDER_UPGRADE_VERSION_VERSION, currentVersion, content));
        } else {
            sb.append(String.format(PROVIDER_UPGRADE_VERSION_OTHER, content));
        }
    }
}
