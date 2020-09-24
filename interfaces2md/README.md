# interfaces2md

The interfaces2md utilty documents an Orchestra interfaces file as a markdown document.

## Running interfaces2md

### Command line arguments

```
usage: Interfaces2md [options] <input-file>
 -?,--help             display usage
 -e,--eventlog <arg>   path of JSON event file
 -o,--output <arg>     path of markdown output file (required)
 ```

### Invoked from an application

The utility may be invoked from Java code as a library. It is constructed and configured by its `Builder` class.

Example

```java
Interfaces2md interfaces2md = Interfaces2md.builder()
    .inputFile("myorchestra.xml")
    .outputFile("mymarkdown.md").build();
interfaces2md.generate();
```

### Event logging

The application uses log4j2 for event logging. Additionally, a JSON event file may be specified to convey warnings and errors encountered during translation. Such a file may be more easily rendered on a web page.