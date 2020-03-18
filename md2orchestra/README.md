# md2orchestra

The md2orchestra utility translates a markdown document to an Orchestra repository file. It can use another Orchestra file as a reference to look up standard fields and components, relieving a user of having to type them in. Any referenced elements not defined in the markdown file are copied from the reference file into the result.

## Functionality

Messages and their elements descriptions are parsed from markdown to populate these Orchestra repository elements:

* Message
* Component
* Repeating group
* Fields
* Datatypes
* Codeset

The utility parses a text file and uses these markdown features:

* A markdown heading immediately above a description of a message or message element is parsed for identifiers including the element type, name, scenario, and numeric tag (if any).
* Any number of markdown paragraphs below the heading will used as documentation of the message or element.
* A markdown table is parsed is message or component members, codes in a codeset, or lists of fields or datatypes

Not all markdown features are recognized or given special treatment. Lists are recognized as a block, but list entries are not treated in any special way.

There are several variations of markdown in use. This utility follows the [GitHub Flavored Markdown](https://github.github.com/gfm/) specification.

## User Guide

See wiki page [md2orchestra User Guide](https://github.com/FIXTradingCommunity/md2spec/wiki/md2orchestra-User-Guide)

## Running md2orchestra

### Command line arguments

```
usage: Md2Orchestra
 -?,--help              display usage
 -e,--eventlog <arg>    path of log file
 -i,--input <arg>       path of markdown input file
 -o,--output <arg>      path of output Orchestra file
 -r,--reference <arg>   path of reference Orchestra file
 -v,--verbose           verbose event log
```

Example

```
java io.fixprotocol.md2orchestra.Md2Orchestra -i mymarkdown.md -o myorchestra.xml -r FixRepository50SP2EP247.xml
```

### Invoked from an application

The utility may be invoked from Java code as a library. It is constructed and configured by its `Builder` class.

Example

```java
Md2Orchestra md2Orchestra1 = Md2Orchestra.builder()
    .inputFile("mymarkdown.md")
    .outputFile("myorchestra.xml").build();
md2Orchestra1.generate();
```
