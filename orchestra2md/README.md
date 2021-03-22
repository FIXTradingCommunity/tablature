# orchestra2md

The orchestra2md utilty documents an Orchestra repository as a markdown document.

## Running orchestra2md

### Command line arguments

```
usage: Orchestra2md [options] <input-file>
 -?,--help              display usage
 -e,--eventlog <arg>    path of JSON event file
    --fixml             output fixml attributes
 -o,--output <arg>      path of markdown output file (required)
    --paragraph <arg>   paragraph delimiter for tables
    --pedigree          output pedigree attributes
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
