(ns com.vadelabs.toon.encode.writer-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing]])
            [com.vadelabs.toon.encode.writer :as writer]))

;; ============================================================================
;; LineWriter Creation Tests
;; ============================================================================

(deftest create-writer-test
  (testing "Create writer with default indent"
    (let [w (writer/create)]
      (is (writer/empty-writer? w))
      (is (= 0 (writer/line-count w)))
      (is (= "" (writer/to-string w)))))

  (testing "Create writer with custom indent"
    (let [w (writer/create 4)]
      (is (writer/empty-writer? w))
      (is (= 0 (writer/line-count w)))
      (is (= "" (writer/to-string w))))))

;; ============================================================================
;; Push and Indentation Tests
;; ============================================================================

(deftest push-basic-test
  (testing "Push single line at depth 0"
    (let [w (-> (writer/create)
                (writer/push 0 "hello"))]
      (is (= 1 (writer/line-count w)))
      (is (= "hello" (writer/to-string w)))))

  (testing "Push multiple lines at depth 0"
    (let [w (-> (writer/create)
                (writer/push 0 "line1")
                (writer/push 0 "line2")
                (writer/push 0 "line3"))]
      (is (= 3 (writer/line-count w)))
      (is (= "line1\nline2\nline3" (writer/to-string w))))))

(deftest push-indentation-test
  (testing "Push lines with different indentation levels (2 spaces)"
    (let [w (-> (writer/create 2)
                (writer/push 0 "level0")
                (writer/push 1 "level1")
                (writer/push 2 "level2")
                (writer/push 1 "level1again"))]
      (is (= 4 (writer/line-count w)))
      (is (= "level0\n  level1\n    level2\n  level1again" (writer/to-string w)))))

  (testing "Push lines with different indentation levels (4 spaces)"
    (let [w (-> (writer/create 4)
                (writer/push 0 "level0")
                (writer/push 1 "level1")
                (writer/push 2 "level2"))]
      (is (= "level0\n    level1\n        level2" (writer/to-string w))))))

;; ============================================================================
;; Whitespace Invariants Tests
;; ============================================================================

(deftest trailing-spaces-test
  (testing "Trailing spaces are removed from content"
    (let [w (-> (writer/create)
                (writer/push 0 "hello   ")
                (writer/push 0 "world  "))]
      (is (= "hello\nworld" (writer/to-string w)))))

  (testing "Trailing spaces at various indentation levels"
    (let [w (-> (writer/create)
                (writer/push 0 "level0  ")
                (writer/push 1 "level1   ")
                (writer/push 2 "level2    "))]
      (is (= "level0\n  level1\n    level2" (writer/to-string w))))))

(deftest no-trailing-newline-test
  (testing "No trailing newline in output"
    (let [w (-> (writer/create)
                (writer/push 0 "line1")
                (writer/push 0 "line2"))
          result (writer/to-string w)]
      (is (not (.endsWith result "\n")))
      (is (= "line1\nline2" result)))))

(deftest empty-content-test
  (testing "Empty content is allowed"
    (let [w (-> (writer/create)
                (writer/push 0 "")
                (writer/push 1 "")
                (writer/push 0 "content"))]
      (is (= 3 (writer/line-count w)))
      (is (= "\n  \ncontent" (writer/to-string w))))))

;; ============================================================================
;; Edge Cases Tests
;; ============================================================================

(deftest complex-indentation-test
  (testing "Complex nested structure"
    (let [w (-> (writer/create)
                (writer/push 0 "root:")
                (writer/push 1 "child:")
                (writer/push 2 "grandchild: value")
                (writer/push 1 "another-child: value2"))]
      (is (= "root:\n  child:\n    grandchild: value\n  another-child: value2"
             (writer/to-string w))))))

(deftest preserve-inner-spaces-test
  (testing "Inner spaces are preserved"
    (let [w (-> (writer/create)
                (writer/push 0 "hello world")
                (writer/push 1 "foo  bar  baz"))]
      (is (= "hello world\n  foo  bar  baz" (writer/to-string w))))))

(deftest special-characters-test
  (testing "Special characters are preserved"
    (let [w (-> (writer/create)
                (writer/push 0 "name: \"Ada\"")
                (writer/push 0 "emoji: 🚀")
                (writer/push 0 "unicode: café"))]
      (is (= "name: \"Ada\"\nemoji: 🚀\nunicode: café" (writer/to-string w))))))

(deftest high-depth-test
  (testing "High indentation depth"
    (let [w (-> (writer/create)
                (writer/push 0 "d0")
                (writer/push 5 "d5")
                (writer/push 10 "d10"))]
      (is (= "d0\n          d5\n                    d10" (writer/to-string w))))))
