# md-grammar

Parser and writer for markdown documents. 

The markdown grammar follows [GitHub Flavored Markdown Spec](https://github.github.com/gfm/).

## Supported markdown features

### Leaf blocks

* ATX headings
* Fenced code blocks -- a fenced code block has an optional infostring that tells the language of the code block. Originally, language was for programming language of the code block to support syntax-specific formatting. However, language has expanded to include various file types. See the list of [languages recognized by GitHub](https://github.com/github/linguist/blob/master/lib/linguist/languages.yml).
* Paragraphs
* Tables
* Block quotes
* Lists

### Unsupported

The following markdown features are currently unsupported.

* Thematic breaks
* Setext headings
* HTML blocks
* Link reference definitions
