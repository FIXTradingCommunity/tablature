# interfaces2md

The interfaces2md utilty documents an Orchestra interfaces file as a markdown document.

## Running interfaces2md

### Command line arguments

```
usage: Interfaces2md [options] 
 -?,--help              display usage
 -e,--eventlog <arg>    path of log file
 -i,--input <arg>       path of markdown input file (required)
 -o,--output <arg>      path of output Orchestra file (required)
 -r,--reference <arg>   path of reference Orchestra file
 -v,--verbose           verbose event log
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