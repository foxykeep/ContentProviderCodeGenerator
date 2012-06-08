package com.foxykeep.cpcodegenerator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileCache {

    private static final int BUFFER_SIZE = 2048;
    public static final int ZIP_BUFFER_SIZE = 4 * BUFFER_SIZE;

    private FileCache() {
    }

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
        if (file == null || file.exists()) {
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

    public static void copyFile(final FileInputStream fromFile, final FileOutputStream toFile) throws IOException {
        FileChannel fromChannel = null;
        FileChannel toChannel = null;
        try {
            fromChannel = fromFile.getChannel();
            toChannel = toFile.getChannel();
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            try {
                if (fromChannel != null) {
                    fromChannel.close();
                }
            } finally {
                if (toChannel != null) {
                    toChannel.close();
                }
            }
        }
    }
}
