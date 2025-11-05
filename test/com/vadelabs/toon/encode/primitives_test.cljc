(ns com.vadelabs.toon.encode.primitives-test
  (:require
    #?(:clj [clojure.test :refer [deftest is testing]]
       :cljs [cljs.test :refer [deftest is testing]])
    [com.vadelabs.toon.encode.primitives :as prim]))


;; ============================================================================
;; nil Encoding Tests
;; ============================================================================

(deftest nil-encoding-test
  (testing "nil encodes to null literal"
    (is (= "null" (prim/encode nil)))
    (is (= "null" (prim/encode nil ",")))
    (is (= "null" (prim/encode nil "\t")))))


;; ============================================================================
;; Boolean Encoding Tests
;; ============================================================================

(deftest boolean-encoding-test
  (testing "Booleans encode to true/false literals"
    (is (= "true" (prim/encode true)))
    (is (= "false" (prim/encode false))))

  (testing "Boolean encoding is delimiter-independent"
    (is (= "true" (prim/encode true ",")))
    (is (= "true" (prim/encode true "\t")))
    (is (= "false" (prim/encode false "|")))))


;; ============================================================================
;; Number Encoding Tests
;; ============================================================================

(deftest integer-encoding-test
  (testing "Integers encode to string representation"
    (is (= "0" (prim/encode 0)))
    (is (= "42" (prim/encode 42)))
    (is (= "-7" (prim/encode -7)))
    (is (= "1000" (prim/encode 1000)))))


(deftest float-encoding-test
  (testing "Floating point numbers encode to string representation"
    (is (= "3.14" (prim/encode 3.14)))
    (is (= "-0.5" (prim/encode -0.5)))
    (is (= "2.718" (prim/encode 2.718)))))


(deftest number-encoding-delimiter-independent-test
  (testing "Number encoding is delimiter-independent"
    (is (= "42" (prim/encode 42 ",")))
    (is (= "42" (prim/encode 42 "\t")))
    (is (= "3.14" (prim/encode 3.14 "|")))))


;; ============================================================================
;; String Encoding Tests
;; ============================================================================

(deftest simple-string-encoding-test
  (testing "Simple strings encode without quotes"
    (is (= "hello" (prim/encode "hello")))
    (is (= "world" (prim/encode "world")))
    (is (= "simple123" (prim/encode "simple123")))))


(deftest string-with-delimiter-encoding-test
  (testing "Strings containing delimiter are quoted"
    (is (= "\"a,b\"" (prim/encode "a,b" ",")))
    (is (= "\"a\\tb\"" (prim/encode "a\tb" "\t")))  ; Tab is always escaped
    (is (= "\"a|b\"" (prim/encode "a|b" "|")))))


(deftest string-delimiter-awareness-test
  (testing "String encoding is delimiter-aware"
    ;; Comma not quoted when using tab delimiter
    (is (= "a,b" (prim/encode "a,b" "\t")))

    ;; Tab is always escaped (control character), even with comma delimiter
    (is (= "\"a\\tb\"" (prim/encode "a\tb" ",")))

    ;; Each delimiter is quoted with its own delimiter
    (is (= "\"a,b\"" (prim/encode "a,b" ",")))
    (is (= "\"a\\tb\"" (prim/encode "a\tb" "\t")))))


(deftest string-with-quotes-encoding-test
  (testing "Strings containing quotes are quoted and escaped (JSON-style)"
    (is (= "\"say \\\"hi\\\"\"" (prim/encode "say \"hi\"")))
    (is (= "\"\\\"quoted\\\"\"" (prim/encode "\"quoted\"")))))


(deftest string-with-special-chars-encoding-test
  (testing "Strings with special characters are quoted"
    (is (= "\"key:value\"" (prim/encode "key:value")))
    (is (= "\"line1\\nline2\"" (prim/encode "line1\nline2")))  ; Newline is escaped
    (is (= "\" leading\"" (prim/encode " leading")))
    (is (= "\"trailing \"" (prim/encode "trailing ")))))


(deftest empty-string-encoding-test
  (testing "Empty strings are quoted"
    (is (= "\"\"" (prim/encode "")))
    (is (= "\"\"" (prim/encode "" ",")))
    (is (= "\"\"" (prim/encode "" "\t")))))


;; ============================================================================
;; Default Delimiter Tests
;; ============================================================================

(deftest default-delimiter-test
  (testing "Default delimiter is comma"
    (is (= "\"a,b\"" (prim/encode "a,b")))
    (is (= "\"a\\tb\"" (prim/encode "a\tb")))  ; Tab is always escaped
    (is (= "a|b" (prim/encode "a|b")))))


;; ============================================================================
;; Error Handling Tests
;; ============================================================================

(deftest non-primitive-error-test
  (testing "Non-primitive values throw error"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (prim/encode [])))
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (prim/encode {})))
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (prim/encode :keyword)))))


;; ============================================================================
;; Comprehensive Mixed Tests
;; ============================================================================

(deftest comprehensive-primitive-encoding-test
  (testing "Mix of all primitive types with different delimiters"
    (let [test-cases [;; [value delimiter expected]
                      [nil "," "null"]
                      [true "," "true"]
                      [false "," "false"]
                      [42 "," "42"]
                      [3.14 "," "3.14"]
                      ["simple" "," "simple"]
                      ["a,b" "," "\"a,b\""]
                      ["a,b" "\t" "a,b"]
                      ["a\tb" "\t" "\"a\\tb\""]  ; Tab is escaped
                      ["a|b" "|" "\"a|b\""]
                      ["" "," "\"\""]
                      [" " "," "\" \""]]]
      (doseq [[value delimiter expected] test-cases]
        (is (= expected (prim/encode value delimiter))
            (str "Failed for value=" value " delimiter=" delimiter))))))
