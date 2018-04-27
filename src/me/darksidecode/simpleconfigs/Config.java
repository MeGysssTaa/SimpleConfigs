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

package me.darksidecode.simpleconfigs;

import me.darksidecode.simpleconfigs.util.Compatibility;
import me.darksidecode.simpleconfigs.util.Files;
import me.darksidecode.simpleconfigs.util.Strings;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The configuration object and util at the same time.
 *
 * As object, it's a parsed form of a given text
 * configuration file, which can be used to both
 * obtain and assign values. Creating new entries
 * is also possible.
 *
 * As util, the class is used to read Config objects
 * from text files and vise-versa.
 */
public class Config {

    /**
     * The API version of this SimpleConfigs release in Semantics format X.Y.Z,
     * where:
     *     X - major version number;
     *     Y - minor version number;
     *     Z - patch number.
     */
    public static final String VERSION = "1.2.0";

    /**
     * The character sequence that splits entries' keys and values:
     * {key}<ASSIGN_MARK>{value}
     */
    private static final String ASSIGN_MARK = " = ";

    /**
     * The character sequence that indicates that the value is possibly of type LIST,
     * that we need to expect the closing CLOSE_LIST_MARK somewhere ahead, and that in
     * case we find it, all values between OPEN_LIST_MARK and CLOSED_LIST_MARK, splitted
     * by LIST_SPLIT_MARK, should be added to a LIST, which should be assigned as the value.
     *
     * @see Config#CLOSE_LIST_MARK
     * @see Config#LIST_SPLIT_MARK
     */
    private static final String OPEN_LIST_MARK = "[";

    /**
     * The character sequence that indicates that we have reached the end of a
     * possible LIST, and that if there was an opening OPEN_LIST_MARK behind, all
     * values between OPEN_LIST_MARK and CLOSED_LIST_MARK, splitted by LIST_SPLIT_MARK,
     * should be added to a LIST, which should be assigned as the value.
     *
     * @see Config#OPEN_LIST_MARK
     * @see Config#LIST_SPLIT_MARK
     */
    private static final String CLOSE_LIST_MARK = "]";

    /**
     * The character sequence that splits values in a LIST.
     *
     * @see Config#OPEN_LIST_MARK
     * @see Config#CLOSE_LIST_MARK
     */
    private static final String LIST_SPLIT_MARK = Pattern.quote(", ");

    /**
     * The character sequence that, being put at the end of line, indicates start of a new secton.
     */
    private static final String SECTION_MARK = ":";

    /**
     * RegEx-escaped version of the section key split character sequence.
     * Used to extract sections from Strings like `section.subsection.key`.
     */
    private static final String SECTION_SPLIT = Pattern.quote(".");

    /**
     * The number of spaces that should be counted as an indent.
     * Used when saving and extracting sections by their indents.
     */
    private static final int INDENT_SIZE = 4;

    /**
     * The minimal length of an entire line.
     *
     * As entries consist of a key, a value and the assign mark between
     * them, and the minmal length of both the key and the value is 1,
     * the valid entry length is computed as the length of the assign mark
     * plus 2 (1 (min key length) + 1 (min value length)).
     */
    private static final int MIN_ENTRY_LEN = ASSIGN_MARK.length() + 2;

    /**
     * The configuration is read to the following map in the natural key:value format.
     * This means that a configuration entry like this:
     *     my_key = my_value
     * will result in 'config.put("my_key", my_value)'.
     */
    private final Map<String, Object> config = new LinkedHashMap<>();

    /**
     * The map of indents to lists of appropriate sections in this Config.
     * In EasyConfigs, configuration sections are recognized and saved
     * with use of indents (leading spaces). Each section has its own
     * indents number, while there can be many sections bound to one
     * number of indents. Therefore, we use an <Integer, List<String>> map.
     */
    private final Map<Integer, List<String>> sections = new HashMap<>();

    /**
     * The hash code of this config as it was just after the file has been fully read and parsed.
     * Used to check if there are any changes made to the config after load.
     */
    private final int initialHash;

    /**
     * Create a new configuration based on the given initial key-value map.
     * If no key-value map is given, an empty Config with no entries will be created.
     *
     * @param kv The key-value map to initialize a Config with.
     *           Arguments length must be a multiple of two.
     *           Arguments at even indexes are keys, and those
     *           at odd ones are values. So that a call like:
     *               new Config("k1", "v1", "k2", "v2)
     *           will create a Config with two entries:
     *               k1 = v1
     *               k2 = v2
     * @throws IllegalArgumentException If the length of the given key-value
     *                                  map is not a multiple of two.
     * @throws ConfigFormatException If any of keys or values are invalid.
     */
    public Config(final String... kv) {
        if ((kv != null) && (kv.length > 0)) {
            if ((kv.length % 2) != 0)
                throw new IllegalArgumentException("Key-value map length must be a multiple of two");

            /*

            FIXME: wrong order element positioning causing invalid save.
            EXAMPLE:
                Config c = new Config(
                    "indep_entry_1", "-10",
                    "test_section.sub_1.a", "qwe",
                    "test_section.sub_1.b", "rty",
                    "test_section.sub_2.a", "uio",
                    "test_section.sub_2.abc.test_list", "[a, s, d, f, g, h, j]",
                    "test_section.sub_2.abc.pi", "3.14",
                    "test_section.x0_formula", "-b/2a",
                    "test_section.sub_2.test_list", "[3, 7, -2, 9, -5]",
                    "test_section.sub_2.wrong_ordered_long", "387561823791519815",
                    "indep_entry_2", "-9",
                    "indep_entry_3", "-8",
                    "test_section.sub_1.hello", "world"
                );
             */

            for (int i = 0; i < kv.length; i += 2) {
                final String key = kv[i].trim().replace("\t", "    ");
                final String indentReducedKey = key.replace(" ", "");

                if (indentReducedKey.isEmpty())
                    throw new ConfigFormatException("Key cannot be empty");

                // The entry is inside a section or a subsection. In order to make this
                // Config saveable, we need to ensure all the sections are registered.
                if (key.contains(".")) {
                    final String[] parent = key.split(SECTION_SPLIT);

                    for (int j = 0; j < (parent.length - 1); j++) {
                        final List<String> bound = sections.computeIfAbsent(j * INDENT_SIZE, k -> new ArrayList<>());
                        final String p = parent[j];

                        if (!(bound.contains(p))) bound.add(p); // avoid duplicates
                    }
                }

                final String val = kv[i+1].trim().replace("\t", "    ");
                final String indentReducedVal = val.replace(" ", "");

                if (indentReducedVal.isEmpty())
                    throw new ConfigFormatException("Value cannot be empty");
                if ((val.startsWith(OPEN_LIST_MARK)) && (val.endsWith(CLOSE_LIST_MARK)))
                    config.put(key, parseList(val));
                else config.put(key, val);
            }

            initialHash = hashCode();
        } else initialHash = 0; // init empty config
    }

    /**
     * Parse config from the given string.
     *
     * @throws NullPointerException If the given string is null
     * @throws ConfigFormatException If the given string cannot be parsed to a config properly
     *
     * @param s the string to parse configuration from.
     */
    public Config(final String s) {
        if (s == null)
            throw new NullPointerException("Cannot parse config from null string");

        StringBuilder currentSection = new StringBuilder(); // holds the current section or subsection name
        int currentIndent = 0; // holds the current section or subsection number of indents

        for (String line : s.split(Compatibility.getLineBreak())) {
            line = line.replace("\t", "    "); // replace tabs with four spaces
            String indentReduced = line.replace(" ", "");

            // A comment or an empty line
            if ((indentReduced.startsWith("#")) || (indentReduced.length() < 2))
                continue;

            String trimmed = line.trim(); // remove any leading and tailing spaces

            // This is a new section declaration, e.g. `section:`
            if (indentReduced.endsWith(SECTION_MARK)) {
                // Count the number of leading spaces
                int indent = 0;

                for (final char c : line.toCharArray()) {
                    if (c != ' ')
                        break;
                    indent += 1;
                }

                if ((indent % INDENT_SIZE) != 0)
                    throw new ConfigFormatException("Indent size must be a multiple of " + INDENT_SIZE);

                // Reached end of section
                if (indent < currentIndent) {
                    if ((indent == 0) || (currentSection.indexOf(".") == -1))
                        currentSection = new StringBuilder();
                    else {
                        String[] parent = currentSection.toString().split(SECTION_SPLIT);
                        currentSection = new StringBuilder(Strings.join(0,
                                Arrays.copyOf(parent, indent/INDENT_SIZE) , "."));
                    }
                }

                final String section = trimmed.substring(0, trimmed.length() - SECTION_MARK.length());

                sections.computeIfAbsent(indent, k -> new ArrayList<>()).add(section);
                currentSection.append((currentSection.length() == 0) ? section : "." + section);

                currentIndent = indent;
            } else {
                // Empty line
                if (trimmed.length() < MIN_ENTRY_LEN)
                    continue;

                // Count the number of leading spaces
                int indent = 0;

                for (final char c : line.toCharArray()) {
                    if (c != ' ')
                        break;
                    indent += 1;
                }

                if ((indent % INDENT_SIZE) != 0)
                    throw new ConfigFormatException("Indent size must be a multiple of " + INDENT_SIZE);

                // Reached end of section
                if (indent < currentIndent) {
                    if ((indent == 0) || (currentSection.indexOf(".") == -1))
                        currentSection = new StringBuilder();
                    else {
                        String[] parent = currentSection.toString().split(SECTION_SPLIT);
                        currentSection = new StringBuilder(Strings.join(0,
                                Arrays.copyOf(parent, indent/INDENT_SIZE) , "."));
                    }
                }

                final String[] n = trimmed.split(ASSIGN_MARK);

                // Format is not `K = V`
                if (n.length < 2)
                    throw new ConfigFormatException("Not a statement: '" + trimmed + "'");

                // If this config entry is inside a section or a subsection, then the names
                // of all its parents are added as a prefix, splitted with a dot ('.').
                final String prefix = (currentSection.length() == 0) ? "" : currentSection + ".";

                final String key = prefix + n[0];
                final String val = Strings.join(1, n, ASSIGN_MARK).trim();

                // This entry is of type LIST
                if ((val.startsWith(OPEN_LIST_MARK)) && (val.endsWith(CLOSE_LIST_MARK)))
                    config.put(key, parseList(val));
                else config.put(key, val);

                currentIndent = indent;
            }
        }

        initialHash = hashCode();
    }

    /**
     * Obtain a whole list of all keys this Config has.
     * @return a whole list of all keys this Config has.
     */
    public Set<String> keys() {
        return config.keySet();
    }

    /**
     * Obtain a whole list of all values this Config has.
     * @return a whole list of all values this Config has.
     */
    public Collection<Object> values() {
        return config.values();
    }

    /**
     * Assign the given value to the given key.
     *
     * If an entry with the given key already exists, its value is updated
     * to match the given one, or a new entry is created otherwise.
     *
     * @param key The key to assign the value to.
     * @param value The value to assign.
     *
     * @throws NullPointerException If the given key is null, or if the given value is null
     * @throws IllegalArgumentException If the given key is empty or mathes the ASSIGN_MARK
     *
     * @return The configuration itself.
     */
    public Config set(final String key, final Object value) {
        if (key == null) throw new NullPointerException("Key cannot be null");
        if ((key.isEmpty()) || (key.equals(ASSIGN_MARK))) throw new IllegalArgumentException("Invalid key: '" + key + "'");
        if (value == null)
            throw new NullPointerException("Value cannot be null");
        config.put(key, value);
        return this;
    }

    /**
     * Looks up an entry with by the given key and returns its value as String.
     *
     * @param key The key to search by.
     * @return The value of the entry with the given key as String.
     */
    public String getString(final String key) {
        return get(key);
    }

    /**
     * Looks up an entry with by the given key and returns its value as String.
     * If no values are assigned to the key, then the given default String is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getOrSet(String, Object)
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as String.
     */
    public String getString(final String key, final String def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getString");
        return getOrSet(key, def);
    }

    /**
     * Looks up an entry with by the given key and returns its value as Boolean.
     *
     * @param key The key to search by.
     * @throws ConfigFormatException If the value of the found entry is not a Boolean
     *
     * @return The value of the entry with the given key as Boolean.
     */
    public Boolean getBoolean(final String key) {
        try {
            return Boolean.valueOf(getString(key));
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Boolean", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Boolean.
     * If no values are assigned to the key, then the given default Boolean is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getString(String, String)
     * @see Config#getOrSet(String, Object)
     *
     * @throws ConfigFormatException If the value of the found entry is not a Boolean.
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as Boolean.
     */
    public Boolean getBoolean(final String key, final Boolean def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getBoolean");

        try {
            return Boolean.valueOf(getString(key, def.toString()));
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Boolean", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Integer.
     *
     * @param key The key to search by.
     * @throws ConfigFormatException If the value of the found entry is not an Integer
     *
     * @return The value of the entry with the given key as Integer.
     */
    public Integer getInteger(final String key) {
        try {
            return Integer.valueOf(getString(key));
        } catch (final NumberFormatException nfe) {
            throw new ConfigFormatException("Value at '" + key + "' is not an Integer", nfe);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Integer.
     * If no values are assigned to the key, then the given default Integer is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getString(String, String)
     * @see Config#getOrSet(String, Object)
     *
     * @throws ConfigFormatException If the value of the found entry is not an Integer.
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as Integer.
     */
    public Integer getInteger(final String key, final Integer def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getInteger");

        try {
            return Integer.valueOf(getString(key, def.toString()));
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Integer", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Short.
     *
     * @param key The key to search by.
     * @throws ConfigFormatException If the value of the found entry is not a Short
     *
     * @return The value of the entry with the given key as Short.
     */
    public Short getShort(final String key) {
        try {
            return Short.valueOf(getString(key));
        } catch (final NumberFormatException nfe) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Short", nfe);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Short.
     * If no values are assigned to the key, then the given default Short is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getString(String, String)
     * @see Config#getOrSet(String, Object)
     *
     * @throws ConfigFormatException If the value of the found entry is not a Short.
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as Short.
     */
    public Short getShort(final String key, final Short def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getShort");

        try {
            return Short.valueOf(getString(key, def.toString()));
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Short", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Long.
     *
     * @param key The key to search by.
     * @throws ConfigFormatException If the value of the found entry is not a Long
     *
     * @return The value of the entry with the given key as Long.
     */
    public Long getLong(final String key) {
        try {
            return Long.valueOf(getString(key));
        } catch (final NumberFormatException nfe) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Long", nfe);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Long.
     * If no values are assigned to the key, then the given default Long is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getString(String, String)
     * @see Config#getOrSet(String, Object)
     *
     * @throws ConfigFormatException If the value of the found entry is not a Long.
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as Long.
     */
    public Long getLong(final String key, final Long def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getLong");

        try {
            return Long.valueOf(getString(key, def.toString()));
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Long", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Byte.
     *
     * @param key The key to search by.
     * @throws ConfigFormatException If the value of the found entry is not a Byte
     *
     * @return The value of the entry with the given key as Byte.
     */
    public Byte getByte(final String key) {
        try {
            return Byte.valueOf(getString(key));
        } catch (final NumberFormatException nfe) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Byte", nfe);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Byte.
     * If no values are assigned to the key, then the given default Byte is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getString(String, String)
     * @see Config#getOrSet(String, Object)
     *
     * @throws ConfigFormatException If the value of the found entry is not a Byte.
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as Byte.
     */
    public Byte getByte(final String key, final Byte def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getByte");

        try {
            return Byte.valueOf(getString(key, def.toString()));
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Byte", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Float.
     *
     * @param key The key to search by.
     * @throws ConfigFormatException If the value of the found entry is not a Float
     *
     * @return The value of the entry with the given key as Float.
     */
    public Float getFloat(final String key) {
        try {
            return Float.valueOf(getString(key));
        } catch (final NumberFormatException nfe) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Float", nfe);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Float.
     * If no values are assigned to the key, then the given default Float is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getString(String, String)
     * @see Config#getOrSet(String, Object)
     *
     * @throws ConfigFormatException If the value of the found entry is not a Float.
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as Float.
     */
    public Float getFloat(final String key, final Float def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getFloat");

        try {
            return Float.valueOf(getString(key, def.toString()));
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Float", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Double.
     *
     * @param key The key to search by.
     * @throws ConfigFormatException If the value of the found entry is not a Double
     *
     * @return The value of the entry with the given key as Double.
     */
    public Double getDouble(final String key) {
        try {
            return Double.valueOf(getString(key));
        } catch (final NumberFormatException nfe) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Double", nfe);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Double.
     * If no values are assigned to the key, then the given default Double is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getString(String, String)
     * @see Config#getOrSet(String, Object)
     *
     * @throws ConfigFormatException If the value of the found entry is not a Double.
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as Double.
     */
    public Double getDouble(final String key, final Double def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getDouble");

        try {
            return Double.valueOf(getString(key, def.toString()));
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Double", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Character.
     *
     * @param key The key to search by.
     * @throws ConfigFormatException If the value of the found entry is not a Character
     *
     * @return The value of the entry with the given key as Character.
     */
    public Character getCharacter(final String key) {
        try {
            return getString(key).charAt(0);
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Character", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as Character.
     * If no values are assigned to the key, then the given default Character is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getString(String, String)
     * @see Config#getOrSet(String, Object)
     *
     * @throws ConfigFormatException If the value of the found entry is not a Character.
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as Character.
     */
    public Character getCharacter(final String key, final Character def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getCharacter");

        try {
            return getString(key, def.toString()).charAt(0);
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a Character", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as List.
     *
     * @param key The key to search by.
     * @throws ConfigFormatException If the value of the found entry is not a List
     *
     * @return The value of the entry with the given key as List.
     */
    public List getList(final String key) {
        try {
            return get(key);
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a List", ex);
        }
    }

    /**
     * Looks up an entry with by the given key and returns its value as List.
     * If no values are assigned to the key, then the given default List is
     * returned and assigned.
     *
     * @param key The key to search by.
     * @param def The default value to assign and return in case
     *            there are no values assigned to the key yet.
     *
     * @see Config#getString(String, String)
     * @see Config#getOrSet(String, Object)
     *
     * @throws ConfigFormatException If the value of the found entry is not a List.
     * @throws NullPointerException If the specified default is null.
     *
     * @return The value of the entry with the given key as List.
     */
    public List getList(final String key, final List def) {
        if (def == null)
            throw new NullPointerException("Def cannot be null for getList");

        try {
            return getOrSet(key, def);
        } catch (final Exception ex) {
            throw new ConfigFormatException("Value at '" + key + "' is not a List", ex);
        }
    }
    
    /**
     * Attempts to retreive the object located at the given key, if present,
     * or assigns the given value to the key and returns it otherwise.
     *
     * @param key The key to look for values at, or to assign the given default to.
     * @param def The default value to return and assign to the given key in case
     *            there are no any values assigned to that key yet.
     *
     * @see Map#computeIfAbsent(Object, Function)
     *
     * @return the value assigned to the given key. If no values were assigned
     *         at the beginning of this method execution, then the specified
     *         default value is returned.
     */
    public <T> T getOrSet(final String key, final T def) {
        return (T) config.computeIfAbsent(key, k -> def);
    }

    /**
     * Looks up an entry with by the given key and returns its value
     * generified according to the wanted type T.
     *
     * @param key The key to search by.
     * @throws NullPointerException If the given key is null.
     *
     * @return The value of the entry with the given key casted to the wanted type T.
     */
    public <T> T get(final String key) {
        return (T) raw(key);
    }

    /**
     * Looks up an entry with by the given key and returns its value as Object.
     *
     * @param key The key to search by.
     * @throws NullPointerException If the given key is null.
     *
     * @return The value of the entry with the given key as Object if an entry with
     *         the key same as the given was found, null otherwise.
     */
    public Object raw(final String key) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        return config.get(key);
    }

    /**
     * Save this configuration as string to the file located at the given path.
     * If the current hash code is same as initialHash, no actions are taken.
     * If the file to write to already exists, it is deleted.
     *
     * @throws NullPointerException If the given path is null
     *
     * @param path The path of the file to write to.
     * @return The configuration itself.
     */
    public Config save(final String path) {
        if (hashCode() == initialHash)
            return this;
        write(path, this);
        return this;
    }

    /**
     * Save this configuration as string to the given file.
     * If the current hash code is same as initialHash, no actions are taken.
     *
     * @throws NullPointerException If the given file is null
     *
     * @param f The file to write to.
     * @return The configuration itself.
     */
    public Config save(final File f) {
        if (hashCode() == initialHash)
            return this;
        write(f, this);
        return this;
    }

    /**
     * Returns this config as string
     * @return this config as string
     */
    @Override
    public String toString() {
        final Set<String> initializedSections = new HashSet<>(); // holds set of sections or subsections which don't need init
        final StringBuilder sb = new StringBuilder();

        for (String key : config.keySet()) {
            // This config entry is inside a section or a subsection
            if (key.contains(".")) {
                final String[] parent = key.split(SECTION_SPLIT);
                final int indents = INDENT_SIZE * parent.length;

                for (int i = 0; i < (parent.length - 1); i++) {
                    final String section = parent[i];

                    // Declare section with `section_name:` if needed
                    if (!(initializedSections.contains(section))) {
                        int sectionIndent = -1;

                        // Find the number of indents we need to add for this section/subsection
                        for (final int ind : sections.keySet()) {
                            for (final String s : sections.get(ind)) {
                                if (s.equals(section)) {
                                    sectionIndent = ind;
                                    break;
                                }
                            }
                        }

                        if (sectionIndent == -1)
                            throw new ConfigFormatException("No indent for configuration section \"" + section + "\"");
                        for (int j = 0; j < sectionIndent; j++)
                            sb.append(' ');
                        sb.append(section).append(SECTION_MARK).append('\n');
                        initializedSections.add(section);
                    }
                }

                final StringBuilder indent = new StringBuilder();

                for (int i = 0; i < (indents - INDENT_SIZE); i++)
                    indent.append(' ');
                sb.append(indent.toString());
                sb.append(parent[parent.length - 1]);
            } else sb.append(key); // this is an independent configuration entry not bound to any sections or subsections

            sb.append(ASSIGN_MARK);
            Object val = config.get(key);

            // This is a LIST value (`[a, b, c]`)
            if (val instanceof List) {
                List list = (List) val;
                sb.append(OPEN_LIST_MARK);

                if (!(list.isEmpty())) {
                    for (final Object o : list)
                        sb.append(o.toString()).append(LIST_SPLIT_MARK);
                    Strings.deleteTailing(sb, LIST_SPLIT_MARK.length()); // remove unnecessary tailing LIST_SPLIT_MARK
                }

                sb.append(CLOSE_LIST_MARK);
            } else sb.append(val.toString()); // this is a normal value (`a`)

            sb.append('\n');
        }

        return sb.toString();
    }

    /**
     * Computes the hash code of this configuration.
     * The hash code is taken from the 'config' map.
     *
     * @return the hash code of this configuration.
     */
    @Override
    public int hashCode() {
        return config.hashCode();
    }

    /**
     * Writes the given config as string to the file located at the given path.
     * If the file to write to already exists, it is deleted.
     *
     * @throws NullPointerException If the given path or config is null
     *
     * @param path The path of the file to write to.
     * @param c The configuration to write.
     */
    public static void write(final String path, final Config c) {
        if (path == null)
            throw new NullPointerException("Path cannot be null");
        write(new File(path), c);
    }

    /**
     * Writes the given config as string to the given file.
     * If the file to write to already exists, it is deleted.
     *
     * @throws NullPointerException If the given file or config is null
     *
     * @param f The file to write to.
     * @param c The configuration to write.
     */
    public static void write(final File f, final Config c) {
        if (c == null) throw new NullPointerException("Config cannot be null");
        if (f == null) throw new NullPointerException("File cannot be null");
        if (f.exists())
            f.delete();
        Files.write(f, c.toString());
    }

    /**
     * Parses configuration from file located at the given path.
     *
     * @throws NullPointerException If the given path is null
     * @throws IllegalArgumentException If no files are located at the given path
     * @throws ConfigFormatException If the file located at the given path is not a valid configuration
     *
     * @param path The path of the file to read from.
     * @return A Config object representing configuration written in the file at the given path.
     */
    public static Config read(final String path) {
        if (path == null)
            throw new NullPointerException("Path cannot be null");
        return read(new File(path));
    }

    /**
     * Parses configuration from the given file.
     *
     * @throws IllegalArgumentException If the given file is null or does not exist
     * @throws ConfigFormatException If the given file is not a valid configuration
     *
     * @param f The file to read from.
     * @return A Config object representing configuration written in the given file.
     */
    public static Config read(final File f) {
        if ((f == null) || (!(f.exists())))
            throw new IllegalArgumentException("No such file: " + ((f == null) ? "<NULL>" : f.getAbsolutePath()));
        return new Config(Files.read(f));
    }

    /**
     * Attempts to parse the specified String to a config value of type LIST,
     * or throws a ConfigFormatException if that's impossible.
     *
     * @param s The String to parse a LIST from.
     *
     * @throws ConfigFormatException If the given String cannot be parsed to a config
     *                               value of type LIST properly.
     * @return List, with all the values the given string is supposed to provide.
     */
    private static List parseList(final String s) {
        try {
            final String p = s.replace("[", "").replace("]", "");
            final List list = new ArrayList();

            if (p.isEmpty()) return list; // empty list, i.e. "[]"
            if (p.contains(LIST_SPLIT_MARK))
                list.addAll(Arrays.asList(p.split(LIST_SPLIT_MARK)));
            else list.add(p); // one-value list, e.g. "[value]"

            return list;
        } catch (final Exception ex) {
            throw new ConfigFormatException("Cannot parse a LIST from \"" + s + "\"");
        }
    }

}