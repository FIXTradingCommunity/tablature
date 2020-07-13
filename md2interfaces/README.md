# md2interfaces

The interfaces2md utilty translates a markdown document into an Orchestra interfaces file.

Supported elements include:

* Protocol stack with orchestrations
* Session configurations

## Running md2interfaces

### Command line arguments

```
usage: Md2Interfaces [options] 
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
Md2Interfaces md2Interfaces = Md2Interfaces.builder()
    .inputFile("mymarkdown.md")
    .outputFile("myorchestra.xml").build();
md2Interfaces.generate();
```