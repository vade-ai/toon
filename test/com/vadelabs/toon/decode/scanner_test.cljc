(ns com.vadelabs.toon.decode.scanner-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing]])
   [com.vadelabs.toon.decode.scanner :as scanner]))

;; ============================================================================
;; to-parsed-lines Tests
;; ============================================================================

(deftest parse-simple-flat-structure-test
  (testing "Parse simple flat structure (no nesting)"
    (let [input "name: Alice\nage: 30"
          result (scanner/to-parsed-lines input 2 true)
          lines (:lines result)]
      (is (= 2 (count lines)))
      (is (= 0 (count (:blank-lines result))))

      ;; First line
      (is (= "name: Alice" (:content (first lines))))
      (is (= 0 (:depth (first lines))))
      (is (= 0 (:indent (first lines))))
      (is (= 1 (:line-number (first lines))))

      ;; Second line
      (is (= "age: 30" (:content (second lines))))
      (is (= 0 (:depth (second lines))))
      (is (= 0 (:indent (second lines))))
      (is (= 2 (:line-number (second lines)))))))

(deftest parse-nested-structure-test
  (testing "Parse nested structure with multiple depths"
    (let [input "user:\n  name: Alice\n  address:\n    city: NYC"
          result (scanner/to-parsed-lines input 2 true)
          lines (:lines result)]
      (is (= 4 (count lines)))

      ;; Depth 0
      (is (= "user:" (:content (nth lines 0))))
      (is (= 0 (:depth (nth lines 0))))

      ;; Depth 1
      (is (= "name: Alice" (:content (nth lines 1))))
      (is (= 1 (:depth (nth lines 1))))
      (is (= 2 (:indent (nth lines 1))))

      ;; Depth 1
      (is (= "address:" (:content (nth lines 2))))
      (is (= 1 (:depth (nth lines 2))))

      ;; Depth 2
      (is (= "city: NYC" (:content (nth lines 3))))
      (is (= 2 (:depth (nth lines 3))))
      (is (= 4 (:indent (nth lines 3)))))))

(deftest parse-with-blank-lines-test
  (testing "Track blank lines separately"
    (let [input "a: 1\n\nb: 2\n\n\nc: 3"
          result (scanner/to-parsed-lines input 2 true)
          lines (:lines result)
          blank-lines (:blank-lines result)]
      ;; Content lines
      (is (= 3 (count lines)))
      (is (= "a: 1" (:content (first lines))))
      (is (= "b: 2" (:content (second lines))))
      (is (= "c: 3" (:content (nth lines 2))))

      ;; Blank lines
      (is (= 3 (count blank-lines)))
      (is (= 2 (:line-number (first blank-lines))))
      (is (= 4 (:line-number (second blank-lines))))
      (is (= 5 (:line-number (nth blank-lines 2)))))))

(deftest parse-empty-input-test
  (testing "Handle empty input"
    (let [result (scanner/to-parsed-lines "" 2 true)]
      (is (= 0 (count (:lines result))))
      (is (= 0 (count (:blank-lines result)))))))

(deftest parse-single-line-test
  (testing "Handle single line input"
    (let [result (scanner/to-parsed-lines "hello" 2 true)
          lines (:lines result)]
      (is (= 1 (count lines)))
      (is (= "hello" (:content (first lines))))
      (is (= 0 (:depth (first lines))))
      (is (= 1 (:line-number (first lines)))))))

(deftest parse-with-different-indent-size-test
  (testing "Parse with indent size of 4"
    (let [input "root:\n    child:\n        grandchild"
          result (scanner/to-parsed-lines input 4 true)
          lines (:lines result)]
      (is (= 3 (count lines)))
      (is (= 0 (:depth (nth lines 0))))
      (is (= 1 (:depth (nth lines 1))))
      (is (= 4 (:indent (nth lines 1))))
      (is (= 2 (:depth (nth lines 2))))
      (is (= 8 (:indent (nth lines 2)))))))

;; ============================================================================
;; Strict Mode Validation Tests
;; ============================================================================

(deftest strict-mode-rejects-tabs-test
  (testing "Strict mode rejects tabs in indentation"
    (is (thrown-with-msg?
         #?(:clj Exception :cljs js/Error)
         #"Tabs not allowed"
         (scanner/to-parsed-lines "name: value\n\tindented: value" 2 true)))))

(deftest strict-mode-rejects-invalid-indentation-test
  (testing "Strict mode rejects indentation not multiple of indent-size"
    (is (thrown-with-msg?
         #?(:clj Exception :cljs js/Error)
         #"Indentation must be multiple of 2"
         (scanner/to-parsed-lines "root:\n   child" 2 true)))))

(deftest non-strict-mode-allows-tabs-test
  (testing "Non-strict mode allows tabs"
    (let [result (scanner/to-parsed-lines "name: value\n\tindented: value" 2 false)
          lines (:lines result)]
      (is (= 2 (count lines))))))

(deftest non-strict-mode-allows-invalid-indentation-test
  (testing "Non-strict mode allows invalid indentation multiples"
    (let [result (scanner/to-parsed-lines "root:\n   child" 2 false)
          lines (:lines result)]
      (is (= 2 (count lines)))
      (is (= 1 (:depth (second lines)))))))

;; ============================================================================
;; LineCursor Navigation Tests
;; ============================================================================

(deftest cursor-peek-test
  (testing "Peek returns current line without advancing"
    (let [input "a: 1\nb: 2\nc: 3"
          result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result result)]
      (is (= "a: 1" (:content (scanner/peek-cursor cursor))))
      (is (= "a: 1" (:content (scanner/peek-cursor cursor)))) ;; Still same
      (is (= 0 (:position cursor))))))

(deftest cursor-next-test
  (testing "Next returns current line and advances cursor"
    (let [input "a: 1\nb: 2\nc: 3"
          result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result result)
          [line1 cursor2] (scanner/next-cursor cursor)
          [line2 cursor3] (scanner/next-cursor cursor2)
          [line3 cursor4] (scanner/next-cursor cursor3)]
      (is (= "a: 1" (:content line1)))
      (is (= "b: 2" (:content line2)))
      (is (= "c: 3" (:content line3)))
      (is (= 3 (:position cursor4))))))

(deftest cursor-advance-test
  (testing "Advance moves cursor forward by n steps"
    (let [input "a: 1\nb: 2\nc: 3\nd: 4"
          result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result result)
          cursor2 (scanner/advance-cursor cursor 2)]
      (is (= 2 (:position cursor2)))
      (is (= "c: 3" (:content (scanner/peek-cursor cursor2)))))))

(deftest cursor-at-end-test
  (testing "at-end? checks if cursor is exhausted"
    (let [input "a: 1\nb: 2"
          result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result result)]
      (is (not (scanner/at-end? cursor)))
      (let [cursor2 (scanner/advance-cursor cursor 2)]
        (is (scanner/at-end? cursor2))
        (is (nil? (scanner/peek-cursor cursor2)))))))

(deftest cursor-peek-at-depth-test
  (testing "peek-at-depth returns line only if at target depth"
    (let [input "root:\n  child:\n    grandchild"
          result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result result)]
      ;; Depth 0
      (is (= "root:" (:content (scanner/peek-at-depth cursor 0))))
      (is (nil? (scanner/peek-at-depth cursor 1)))

      ;; Advance to depth 1
      (let [cursor2 (scanner/advance-cursor cursor 1)]
        (is (= "child:" (:content (scanner/peek-at-depth cursor2 1))))
        (is (nil? (scanner/peek-at-depth cursor2 0)))
        (is (nil? (scanner/peek-at-depth cursor2 2)))))))

(deftest cursor-has-more-at-depth-test
  (testing "has-more-at-depth? checks if more lines exist at depth"
    (let [input "a: 1\n  b: 2\n  c: 3\nd: 4"
          result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result result)]
      (is (scanner/has-more-at-depth? cursor 0))
      (is (not (scanner/has-more-at-depth? cursor 1)))

      ;; Advance to first child
      (let [cursor2 (scanner/advance-cursor cursor 1)]
        (is (scanner/has-more-at-depth? cursor2 1))
        (is (not (scanner/has-more-at-depth? cursor2 0)))))))

(deftest cursor-get-blank-lines-in-range-test
  (testing "get-blank-lines-in-range extracts blank lines in range"
    (let [input "a: 1\n\nb: 2\n\n\nc: 3"
          result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result result)]
      ;; All blank lines
      (let [all-blanks (scanner/get-blank-lines-in-range cursor 1 10)]
        (is (= 3 (count all-blanks))))

      ;; Lines 2-4 only
      (let [some-blanks (scanner/get-blank-lines-in-range cursor 2 4)]
        (is (= 2 (count some-blanks)))
        (is (= 2 (:line-number (first some-blanks))))
        (is (= 4 (:line-number (second some-blanks)))))

      ;; No blank lines in range
      (let [no-blanks (scanner/get-blank-lines-in-range cursor 1 1)]
        (is (= 0 (count no-blanks)))))))

;; ============================================================================
;; Edge Cases
;; ============================================================================

(deftest parse-indented-blank-lines-test
  (testing "Blank lines with indentation are tracked"
    (let [input "a: 1\n  \nb: 2"
          result (scanner/to-parsed-lines input 2 true)
          blank-lines (:blank-lines result)]
      (is (= 1 (count blank-lines)))
      (is (= 2 (:line-number (first blank-lines))))
      (is (= 2 (:indent (first blank-lines))))
      (is (= 1 (:depth (first blank-lines)))))))

(deftest parse-trailing-blank-line-test
  (testing "Trailing blank line is tracked"
    (let [input "a: 1\n"
          result (scanner/to-parsed-lines input 2 true)
          lines (:lines result)
          blank-lines (:blank-lines result)]
      (is (= 1 (count lines)))
      ;; Trailing newline creates an empty string after split
      (is (= 1 (count blank-lines))))))

(deftest parse-only-blank-lines-test
  (testing "Input with only blank lines"
    (let [input "\n\n\n"
          result (scanner/to-parsed-lines input 2 true)
          lines (:lines result)
          blank-lines (:blank-lines result)]
      (is (= 0 (count lines)))
      ;; "\n\n\n" splits into 4 empty strings (before first, between each, after last)
      (is (= 4 (count blank-lines))))))

(deftest parse-array-header-test
  (testing "Parse array header lines"
    (let [input "items[2]:\n  - id: 1\n  - id: 2"
          result (scanner/to-parsed-lines input)
          lines (:lines result)]
      (is (= 3 (count lines)))
      (is (= "items[2]:" (:content (nth lines 0))))
      (is (= "- id: 1" (:content (nth lines 1))))
      (is (= "- id: 2" (:content (nth lines 2))))
      (is (= 1 (:depth (nth lines 1)))))))

(deftest parse-tabular-format-test
  (testing "Parse tabular array format"
    (let [input "items[2]{id,name}:\n  1,Alice\n  2,Bob"
          result (scanner/to-parsed-lines input)
          lines (:lines result)]
      (is (= 3 (count lines)))
      (is (= "items[2]{id,name}:" (:content (nth lines 0))))
      (is (= "1,Alice" (:content (nth lines 1))))
      (is (= "2,Bob" (:content (nth lines 2))))
      (is (= 1 (:depth (nth lines 1))))
      (is (= 1 (:depth (nth lines 2)))))))

(deftest cursor-lookup-test
  (testing "LineCursor supports keyword lookup"
    (let [input "a: 1"
          result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result result)]
      (is (= (:lines result) (:lines cursor)))
      (is (= (:blank-lines result) (:blank-lines cursor)))
      (is (= 0 (:position cursor)))
      (is (nil? (:nonexistent cursor))))))
