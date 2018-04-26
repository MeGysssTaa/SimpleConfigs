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

public class Strings {

    /**
     * Joins the previously splitted strings in the given array
     * into a single string, placing the given separator between them.
     *
     * @param start The index to start joining from.
     * @param arr The array of strings to join.
     * @param separator The character or character sequence to put between joined strings.
     *
     * @return The result String.
     */
    public static String join(final int start, final String[] arr, final String separator) {
        final StringBuilder builder = new StringBuilder();

        for (int i = start; i < arr.length; i++) builder.append(arr[i]).append(separator);
        for (int i = 0; i < separator.length(); i++)
            builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    /**
     * Deletes the given number of tailing characters from the specified StringBuilder.
     * If the desired number of chars to delete is greater than or equal to the actual
     * length of the given StringBuilder's character sequence, then a new, fresh object
     * is returned with no String set, as per:
     *     new StringBuilder()
     *
     * @param sb The StringBuilder to delete characters from.
     * @param chars The number of tailing chars to delete.
     *
     * @return a new, empty StringBuilder if the specified number of chars to delete
     *         was greater than or equal to the length of the given StringBuilder's
     *         character sequence, or the given StringBuilder with the specified number
     *         of tailing characters deleted.
     */
    public static StringBuilder deleteTailing(final StringBuilder sb, final int chars) {
        if (chars >= sb.length()) return new StringBuilder();
        for (int i = 0; i < chars; i++)
            sb.deleteCharAt(sb.length() - 1);
        return sb;
    }

}
