# orchestra2md

The orchestra2md utilty documents an Orchestra repository as a markdown document.

## Running md2orchestra

### Command line arguments

```
usage: Orchestra2md
 -?,--help           display usage
 -i,--input <arg>    path of Orchestra input file
 -o,--output <arg>   path of markdown output file
 ```

### Invoked from an application

The utility may be invoked from Java code as a library. It is constructed and configured by its `Builder` class.

Example

```java
Orchestra2md orchestra2md = Orchestra2md.builder()
    .inputFile("myorchestra.xml")
    .outputFile("mymarkdown.md").build();
orchestra2md.generate();
```