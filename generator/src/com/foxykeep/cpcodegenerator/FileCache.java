package com.foxykeep.cpcodegenerator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileCache {

    private static final int BUFFER_SIZE = 2048;

    private FileCache() {}

    public static void saveFile(final String path, final String content) {

        try {
            createFileDir(path);

            final ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes());
            final FileOutputStream fos = new FileOutputStream(path);
            byte[] buffer = new byte[BUFFER_SIZE];
            int readBytes;
            while ((readBytes = bais.read(buffer)) != -1) {
                fos.write(buffer, 0, readBytes);
            }
            fos.close();
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createFileDir(final String path) throws IOException {
        final File file = new File(path);
        if (file.exists()) {
            return;
        }

        final File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            File nomediaFile = new File(parentFile, ".nomedia");
            // Ultra ugly hack to try to evade the Motorola bug. Let's retry after some time
            for (int i = 0; i < 3 && !nomediaFile.mkdirs(); i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
