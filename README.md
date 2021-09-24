# tablature

Converts a Markdown document to a specification and documents a specification as markdown. Markdown can automatically be rendered as a web page in GitHub or other sites. This is an easy way to publish rules of engagement. Roundtrip is supported, allowing a user to update a specification iteratively with an ordinary text editor.

For a brief introduction to markdown, see [Markdown Notation](https://github.com/FIXTradingCommunity/tablature/wiki/Markdown-Notation).

## FIX Orchestra

The intial target of tablature is FIX Orchestra, "machine readable rules of engagement". 

See these projects for the Orchestra standard and related resources:

* [fix-orchestra-spec](https://github.com/FIXTradingCommunity/fix-orchestra-spec) - the standard. The inital implementation of this project is based on FIX Orchestra version 1.0 Draft Standard.
* [fix-orchestra](https://github.com/FIXTradingCommunity/fix-orchestra) - XML schemas, utilities, and demonstration code

## Modules

### md2orchestra

Translates a Markdown document to an Orchestra repository file. 

See the [Tablature User Guide for Messages](https://github.com/FIXTradingCommunity/tablature/wiki/Tablature-User-Guide-for-Messages)

### orchestra2md

Translates an Orchestra repository file to a Markdown document. 

### md2interfaces

Translates a Markdown document to an Orchestra interfaces file. 

See the [Tablature User Guide for Interfaces](https://github.com/FIXTradingCommunity/tablature/wiki/Tablature-User-Guide-for-Interfaces)

### interfaces2md

Translates an Orchestra interfaces file to a Markdown document. 

## Prerequisites
This project requires Java 11 or later. It should run on any platform for which a JVM is supported. Several open-source implementations are available, including [AdoptOpenJDK](https://adoptopenjdk.net/).

## Build
The project is built with Maven version 3.0 or later. See [Building this Project](https://github.com/FIXTradingCommunity/tablature/wiki/Building-this-Project)

## License
© Copyright 2020-2021 FIX Protocol Limited

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.