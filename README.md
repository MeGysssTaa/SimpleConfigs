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
    
    
## How to use
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

```
Config c = Config.read(new File("E:\\testcfg.txt"));
// ... or ...
Config c = Config.read("E:\\testcfg.txt");
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
System.out.printf("%s %s %s %s %s %s", myStr, myNumA, myNumB, myNumC, coolByte, sampleChar);```
```

Output:
`Hello world! 1000 -98471923012831895 3.1415 73 &`

