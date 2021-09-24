# md2orchestra

The md2orchestra utility translates a Markdown document to an Orchestra repository file. It can use another Orchestra file as a reference to look up standard fields and components, relieving a user of having to type them in. Any referenced elements not defined in the markdown file are copied from the reference file into the result.

## Functionality

Messages and their elements descriptions are parsed from markdown to populate these Orchestra repository elements:

* Message
* Component
* Repeating group
* Fields
* Datatypes
* Codeset
* Actor with state variables and state machines
* Flow

The utility parses one or more texts file and uses these Markdown features:

* A Markdown heading immediately above a description of a message or message element is parsed for identifiers including the element type, name, scenario, and numeric tag (if any).
* Any number of Markdown paragraphs below the heading will be used as documentation of the message or element.
* A Markdown table is parsed is message or component members, codes in a codeset, or lists of fields or datatypes

Not all Markdown features are recognized or given special treatment. (Lists are recognized as a block, but list entries are not treated in any special way.)

There are several variations of Markdown in use. This utility follows the [GitHub Flavored Markdown](https://github.github.com/gfm/) specification.

## User Guide

See wiki page [md2orchestra User Guide](https://github.com/FIXTradingCommunity/tablature/wiki/md2orchestra-User-Guide)

## Running md2orchestra

### Command line arguments

Md2Orchestra reads one or more input files to produce output.

```
usage: Md2Orchestra [options] <input-file>...
 -?,--help                display usage
 -d,--searchdepth <arg>   nested component search depth
 -e,--eventlog <arg>      path of JSON event file
 -f,--fullsearch          full nested component search
    --import <arg>        directory for file import
 -o,--output <arg>        path of output Orchestra file (required)
    --paragraph <arg>     paragraph delimiter for tables
 -r,--reference <arg>     path of reference Orchestra file
```

`<input-file>` can be a literal name or a glob pattern where 
- `*` is a wildcard to match any number of characters
- `?` is a wildcard to match a single character
- `**` matches multiple characters that may cross directory boundaries

Example with one named input file

```
java io.fixprotocol.md2orchestra.Md2Orchestra -o myorchestra.xml -r FixRepository50SP2EP247.xml mymarkdown.md
```

Example with two inputs

```
java io.fixprotocol.md2orchestra.Md2Orchestra -o myorchestra.xml -r FixRepository50SP2EP247.xml mymarkdown1.md mymarkdown2.md
```

Example with glob pattern for inputs
```
java io.fixprotocol.md2orchestra.Md2Orchestra -o myorchestra.xml -r FixRepository50SP2EP247.xml *.md
```

### Invoked from an application

The utility may be invoked from Java code as a library. It is constructed and configured by its `Builder` class in fluent code style.

Example

```java
Md2Orchestra md2Orchestra1 = Md2Orchestra.builder()
    .inputFilePattern("mymarkdown.md")
    .referenceFile("FixRepository50SP2EP247.xml")
    .outputFile("myorchestra.xml")
    .build();
md2Orchestra1.generate();
```
