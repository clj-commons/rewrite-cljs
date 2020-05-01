## Changelog

### 0.4.5
- freshen project - update deps, fix test runner, setup for CircleCI

### 0.4.4
- refactor: Upgrade tools.reader to 1.0.5 and fix broken call to read-token
- fix: rewrite-clj.node/length
- fix: in-range?

### 0.4.3
- Handle multiline regex

### 0.4.2
- Support reader conditionals

### 0.4.1
- Fixed handling of global flags for regex getting lost when parsing (or rather when stringifiying them back)
- Clean up compiler warnings

### 0.4.0
- Upped Clojure to 1.7.0 and ClojureScript 1.7.228
- Using cljs.tools.reader rather than a custom reader
- Tests are run using https://github.com/bensu/doo

Kudos to https://github.com/mhuebert for the awesome pull request !

### 0.3.1
- Significant improvement in performance of parsing

### 0.3.0
- Performance improvements

### 0.2.0
- Kill one (ParEdit)
- Raise (ParEdit)
- Fixed bug with namespaced keywords
- Fixed bug with coercion of function nodes

### 0.1.0
- Initial release
- Port initiated from version 0.4.12 of rewrite-clj. Check out it's https://github.com/xsc/rewrite-clj/blob/master/CHANGES.md[changelog].
  Not all functions have been ported (most notably zip subedit support and printing of nodes are missing)
- Also added a paredit namespace for common pareedit features that modifies the source (slurp, barf, kill, join, split etc)
