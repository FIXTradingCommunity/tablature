# md2interfaces

The interfaces2md utilty translates a markdown document into an Orchestra interfaces file.

Supported elements include:

* Protocol stack with orchestrations
* Session configurations

## Running md2interfaces

### Command line arguments

Md2Interfaces can input one or more markdown files. If multiple inputs are used, then order is significant for context. For example, a markdown file containing a session configuration will be associated with the most recent interface definition.

```
  usage: Md2Interfaces  [options] <input-file>...
  -?,--help             display usage
  -e,--eventlog <arg>   path of JSON event file
  -o,--output <arg>     path of output interfaces file (required)
 ```

### Invoked from an application

The utility may be invoked from Java code as a library. It is constructed and configured by its `Builder` class.

Example

```java
Md2Interfaces md2Interfaces = Md2Interfaces.builder()
    .inputFile("mymarkdown.md")
    .outputFile("myorchestra.xml").build();
md2Interfaces.generate();
```

### Event logging

The application uses log4j2 for event logging. Additionally, a JSON event file may be specified to convey warnings and errors encountered during translation. Such a file may be more easily rendered on a web page.