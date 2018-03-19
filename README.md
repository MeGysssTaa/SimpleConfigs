# SimpleConfigs
### An extremely simple and lightweight configs API for Java

Working on a kind of a small or simple project and looking for a simple and lightweight library that will enable you to store specific settings in configuration files with ability to then read those easily? **SimpleConfigs** is probably what you need.

**Features:**
* Write and edit configuration files both programmatically and manually;
* Read configuration files in a map with just 1 line of code;
* Modify values of existing settings and add new ones easily;
* Obtain settings casted to the types you need;
* Configuration comments;
* Very lightweight and easy to use.
    
**Drawbacks** (some are probably temporary and will be solved in the near future):
* Key-value split char space dependence (`=` != ` = `);
* No strict entries order after saving programmatically;
* Comments get removed after saving programmatically.
    
    
## Reading configurations
Configuration is read the way that 1 line equals 1 configuration entry.
Configuration entries consist of `key`, `value` and the **assign mark** between them (` = `):
> key = value

Let's write an example config containing the most important features manually:
```
# Example string
my_str = Hello world!

# Example numbers:
#    a - int/short
#    b - long
#    c - double/float
my_num_a = 1000
my_num_b = -98471923012831895
my_num_c = 3.1415

# Example byte
cool_byte = 73

# Example character
sample_char = &
```

Now let's obtain all the values here in code.
First off, we need to create a `Config` object by reading it using `Config#read`. Both `File` and `String` (path to file) are acceptable:

```java
Config c = Config.read(new File("E:\\testcfg.txt"));
```
...or...
```java
Config c = Config.read("E:\\testcfg.txt");
```

The other possible way is to construct a new `Config` object from text manually. This might be needed when we have already got config as text as `String`:
```java
Config c = new Config(configString);
```

The values are then obtainable with `getTYPE` methods, for example, `getInteger`, `getString`, etc. The argument should be entry `key` (string standing before ` = {value}` in the config. Let's output all the values in our configuration:

```java
// Read the configuration
Config c = Config.read("E:\\testcfg.txt");

// Obtain the values
final String myStr = c.getString("my_str");
final int myNumA = c.getInteger("my_num_a");
final long myNumB = c.getLong("my_num_b");
final float myNumC = c.getFloat("my_num_c");
final byte coolByte = c.getByte("cool_byte");
final char sampleChar = c.getCharacter("sample_char");

// Output the values
System.out.printf("%s %s %s %s %s %s", myStr, myNumA, myNumB, myNumC, coolByte, sampleChar);
```

Output:

`Hello world! 1000 -98471923012831895 3.1415 73 &`


## Editing and writing configurations
We use the `Config#set` method to update existing or create new entries. The method accepts two parameters: `key` of the entry we want to update, if such entry already exists, or of the new entry we want to create otherwise; and `value` — the object to assign. Let's say we want to change the value of an existing entry `my_str` to `"123 new-string"`. For this purpose we do:
```java
c.set("my_str", "123 new-string");
```

If we then output `c.getString("my_str")`, we'll see our new string instead of the old one.

Now let's suppose we need to create a new entry with a key our current configuration does not contain. We do essentially the same, passing the `key` of the entry we want to create as the first argument:
```java
c.set("new_int", 42);
```

After saving the code above will result in:
> new_int = 42

## Saving edited configurations
Now let's save our configuration. For that the `Config#save` method is strongly recommended, although there is also a static `Config#write` method. Both accept either `File` or `String` (path to file) to write the config to, and the `write` method also accepts a `Config` parameter (as it's static and can't know what to save otherwise).


**If you want to save the update config to the same file which it was loaded from, use:**
```java
c.save(new File("E:\\testcfg.txt"));
```
...or...
```java
c.save("E:\\testcfg.txt");
```

The `save` method checks if the config hash has updated. If not, this means there are no changes made to the configuration, and so no saving is needed. Nothing will be done in this case. However, if the hash the changed, this means the configuration has been modified and needs to be updated in file. If the given file already exists, it will be deleted. Then a file at the given located (also with the given name) is created, and the configuration is written in it.


**If you, however, want to save your config to another file and don't want SimpleConfigs to check hashes (so that the config will be saved even if you haven't touched anything in it), you need to use:**

```java
Config.write(new File("E:\\OtherDir\\other_file.txt"), c);
```
...or...
```java
Config.write("E:\\OtherDir\\other_file.txt", c);
```

The static utility method performs no updates checking and so you can use it to, for example, read configuration from file A and write it to file B, at the same time verifying the configuration text.


A different method of saving is also available — **manual saving**.
You can obtain the configuration as `String` with:
```java
String configText = c.toString();
```
and then write it manually (or do any other stuff with the text you want).


Both `Config#save` and static `Config#write` use `Config#toString`.


Please note that (at least at the moment) `toString` ensures no strict entries order. Moreover, the comments are skipped at the reading stage. This means that after saving, your configuration will lose all its comments and might get its entries reordered.


For any questions, improvements or issues please use the **[Issue Tracker](https://github.com/MeGysssTaa/SimpleConfigs/issues)**.
