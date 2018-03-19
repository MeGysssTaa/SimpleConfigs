/*
 * Copyright 2018 DarksideCode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.darksidecode.simpleconfigs.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Used for writing to and reading from files easier.
 */
@SuppressWarnings ("ResultOfMethodCallIgnored")
public class Files {

    /**
     * The encoding encode configs with.
     * UTF-8 allows non-ASCII characters.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Write the given string to the given file.
     *
     * @param f The file to write the string in
     * @param data The string to write
     */
    public static void write(final File f, final String data) {
        try {
            java.nio.file.Files.write(f.toPath(), data.getBytes(ENCODING));
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to write to file: " + f.getAbsolutePath() + " (" + ((data.length() < 100) ? "\"" + data + "\"" : data.length() + " chars") + ")");
        }
    }

    /**
     * Returns the contents of the given file as String
     *
     * @param f The file to read from.
     * @return the contents of the given file as String
     */
    public static String read(final File f) {
        try {
            return new String(java.nio.file.Files.readAllBytes(f.toPath()), ENCODING);
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to read from file: " + f.getAbsolutePath(), ex);
        }
    }

}
