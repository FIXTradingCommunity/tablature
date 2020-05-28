# tablature

*(formerly md2spec)*

Converts a markdown document to a specification and documents a specification as markdown. Roundtrip is thus supported, allowing a user to update a specification iteratively with an ordinary text editor.

## FIX Orchestra

The intial target of tablature is FIX Orchestra, "machine readable rules of engagement". 

See these projects for the Orchestra standard and related resources:

* [fix-orchestra-spec](https://github.com/FIXTradingCommunity/fix-orchestra-spec) - the standard. The inital implementation of this project is based on FIX Orchestra version 1.0 Draft Standard.
* [fix-orchestra](https://github.com/FIXTradingCommunity/fix-orchestra) - XML schemas, utilities, and demonstration code

## Modules

### md2orchestra

Translates a markdown document to an Orchestra repository file. Currently, only message structures are supported. Future enhancments may also include workflow.

See the [User Guide](https://github.com/FIXTradingCommunity/tablature/wiki/md2orchestra-User-Guide)

### orchestra2md

Translates an Orchestra repository file to a markdown document. Markdown can automatcially be rendered as a web page in GitHub or other sites. This is an easy way to publish rules of engagement. Currently, only message structures are supported.

### md-grammar

A library to parse and write markdown files. Generic data structures act as an intermediary between the markdown parser and writer logic and applications that use them. In other words, the part of an application that reads or writes a specification like Orchestra needs no special knowledge of markdown.

## Prerequisites
This project requires Java 11 or later. It should run on any platform for which a JVM is supported. Several open-source implementations are available, including OpenJDK.

## Build
The project is built with Maven version 3.0 or later. See [Building this Project](https://github.com/FIXTradingCommunity/tablature/wiki/Building-this-Project)

## License
© Copyright 2020 FIX Protocol Limited

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.