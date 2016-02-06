(ns atollier.test-xlet
  (:require [atollier.xlet :refer :all]
            [clojure.test :refer :all]))


(extend-type java.util.Date
  IExtractPattern
  (unapply [date]
    [(.getYear date) (.getMonth date) (.getDate date)]))

(extend-type java.net.URI
  IExtractPattern
  (unapply [uri]
    [(.getHost uri) (.getPath uri)]))

(deftest test-seq-destructuring
  (is (= 1 (xlet '(1 2 3) [a _ _] a)))
  (is (= '(2 3) (xlet '(1 2 3) [_ a] a))))

(deftest test-arbitrary-java-objects
  (is (= 2016 (xlet (java.util.Date. 2016 2 6) [y _ _] y)))
  (is (= "www.google.com" (xlet (java.net.URI/create "https://www.google.com/q") [host path] host))))


