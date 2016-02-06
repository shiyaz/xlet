# xlet

## Let binding using Scala-like pattern extraction.


## Usage

The ``xlet`` macro creates a lexical context by first matching a
binding pattern against an expression and then binding the symbols in
the pattern to values extracted from the expression similar to the
pattern extraction mechanism in Scala.

The symbol ``_`` is used as a wildcard that is not bound. It is used
to ignore the corresponding value in the extraction.

The expression used for extraction must evaluate to a type that
extends the protocol ``IExtractPattern``.

### Example:

    (extend-type java.util.Date
        atollier.xlet/IExtractPattern
        (unapply [date]
            [(.getYear date) (.getMonth date) (.getDate date)]))

    (atollier.xlet/xlet (java.util.Date. 2016 2 6)
        [_ _ _ d] (throw (Exception. "Won't be thrown."))
        [_ _ d] (str "Day of month: " d))


## License

Copyright © 2016 [Shiyas Rasheed](http://atollier.com)

Distributed under the Eclipse Public License either version 1.0.
