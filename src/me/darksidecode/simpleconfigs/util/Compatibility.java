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

/**
 * Used to allow cross-platform compatibility.
 */
public class Compatibility {

    /**
     * Returns the line breaking character or character sequence for the current platform
     * @return the line breaking character or character sequence for the current platform
     */
    public static String getLineBreak() {
        return OS.getOS().getLineBreak();
    }

    /**
     * Platform utilities...
     */
    public enum OS {
        WINDOWS ("\r\n"),
        LINUX   (  "\n"),
        MAC     (  "\r");

        /**
         * Current platform cache. Used to avoid performing
         * redudant OS checking as that is redudant.
         */
        private static OS os = null;

        /**
         * The line breaking character or character sequence for the current platform
         */
        private final String lineBreak;

        OS(final String lineBreak) {
            this.lineBreak = lineBreak;
        }

        /**
         * Returns the line breaking character or character sequence for the current platform
         * @return the line breaking character or character sequence for the current platform
         */
        public String getLineBreak() {
            return lineBreak;
        }

        /**
         * Obtains the platform the application is running on using
         * System.getProperty("os.name") and String#contains.
         *
         * @return the platform the application is running on
         */
        public static OS getOS() {
            if (os != null)
                return os;

            final String name = System.getProperty("os.name").toLowerCase();

            if (name.contains("win")) return os = WINDOWS;
            else if (name.contains("mac"))
                return os = MAC;
            else return os = LINUX;
        }
    }

}
