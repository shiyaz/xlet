(ns atollier.xlet)

(defprotocol IExtractPattern
  "Allows expressions of arbitrary types to be used for xlet binding.

  An unapply method for a type should strictly return a vector for simple binding,
  or a seq for sequence matching (which is a simple form of destructuring on sequences.)"
  (unapply [x]))

(extend-protocol IExtractPattern
  java.lang.Object
  (unapply [o]
    nil)
  clojure.lang.Seqable
  (unapply [s]
    s))

(defn- equate! [x v bindings]
  (letfn [(bound? [x]
            (contains? @bindings x))
          (bound-to? [x v]
            (= v (get-in @bindings [x])))
          (bind! [x v]
            (swap! bindings assoc x v)
            true)]
    (or (when (symbol? x)
          (or (= '_ x)
              (if (bound? x)
                (bound-to? x v)
                (bind! x v))))
        (= x v))))

(defn- simple-bind [pat ext bindings]
  (every? (fn [[p e]]
            (equate! p e bindings))
          (zipmap pat ext)))

(defn- seq-bind [[p & ps :as pat]
                 [e & es :as ext]
                 bindings]
  (if (empty? pat)
    (empty? ext)
    (if (and (empty? ps) (not-empty es))
      (equate! p ext bindings)
      (when (equate! p e bindings)
        (recur ps es bindings)))))

(defn bind-pattern* [pat ext]
  (let [bindings (atom {})]
    (cond (nil? ext)                           nil
          (nil? pat)                           nil
          (and (seq? ext)
               (seq-bind pat ext bindings))    @bindings
          (and (== (count pat) (count ext))
               (simple-bind pat ext bindings)) @bindings
          :default                             nil)))

 (defmacro xlet
   "The xlet macro creates a lexical context by first matching a binding pattern against
   an expression and then binding the symbols in the pattern to values extracted from the
   expression using IExtractPattern/unapply.

   The symbol _ is used as a wildcard that is not bound. It is used to ignore the
   corresponding value in the extraction.

   The expression e must evaluate to a type that extends the protocol IExtractPattern.

   Example:

   (extend-type java.util.Date
     IExtractPattern
     (unapply [date]
        [(.getYear date) (.getMonth date) (.getDay date)]))

   (xlet (java.util.Date. 2016 2 6)
     [a b c d] \"no match. ignored.\"
     [_ _ d] (str \"the day is \" d))
   "
   [e & clauses]
   (let [extraction (gensym)
         cc (count clauses)
         expand-clause-1 (fn [bindings patn body]
                           (let [vars (remove #(= %1 '_)
                                              (filter symbol? patn))]
                             (list 'let
                                   (vec (mapcat (fn [v _] (list v (list 'get-in bindings [`(quote ~v)])))
                                                vars vars))
                                   body)))
         expand-clauses (fn expand-clauses
                          [clauses]
                          (if clauses
                            (let [patn (first clauses)
                                  body (second clauses)
                                  bindings (gensym)]
                              (list (cons 'if-let
                                         (cons [bindings (list 'atollier.xlet/bind-pattern* `(quote ~patn) extraction)]
                                               (cons (expand-clause-1 bindings patn body)
                                                     (expand-clauses (next (next clauses))))))))
                            (list (list 'throw
                                        (list 'new 'java.lang.IllegalArgumentException
                                              "No pattern matches in xlet.")))))]
     (when (> cc 0)
       (if-not (even? cc)
         (throw (java.lang.IllegalArgumentException. "xlet requires an even number of forms."))
         (cons 'let
               (cons [extraction (list 'unapply e)]
                     (expand-clauses clauses)))))))
