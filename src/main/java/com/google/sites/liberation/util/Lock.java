package com.google.sites.liberation.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author bestplay9@me.com
 */
public class Lock {

    private static final Logger LOGGER = LogManager.getLogger(Lock.class);
    private static Path file = null;

    public static String lockFilePath() {
        return file.toAbsolutePath().toString();
    }

    public static boolean notRunning(final String lockFile) {
        try {
            file = Paths.get(lockFile);
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "rw");
            final FileLock fileLock = randomAccessFile.getChannel().tryLock();
            if (fileLock != null) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        try {
                            fileLock.release();
                            randomAccessFile.close();
                            file.toFile().delete();
                        } catch (Exception e) {
                            LOGGER.error("Unable to remove lock file: " + lockFile, e);
                        }
                    }
                });
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Unable to create and/or lock file: " + lockFile, e);
        }
        return false;
    }

}
