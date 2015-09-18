/*
 * Copyright (C) 2015 Free Construction Sp. z.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
