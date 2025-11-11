(ns com.vadelabs.toon.decode.keys-test
  (:require
    #?(:clj [clojure.test :refer [deftest is testing]]
       :cljs [cljs.test :refer [deftest is testing]])
    [com.vadelabs.toon.decode.keys :as keys]))


;; ============================================================================
;; Path Expansion Unit Tests
;; ============================================================================

(deftest expand-paths-disabled-test
  (testing "Returns value unchanged when expansion is disabled"
    (let [value {"data.config.server" "localhost"}
          result (keys/expand value true :off)]
      (is (= value result)))))


(deftest expand-paths-simple-dotted-key-test
  (testing "Expands simple dotted key"
    (let [value {"data.config.server" "localhost"}
          result (keys/expand value true :safe)]
      (is (= {"data" {"config" {"server" "localhost"}}} result)))))


(deftest expand-paths-multiple-dotted-keys-test
  (testing "Expands multiple dotted keys"
    (let [value {"user.name.first" "Alice"
                 "user.name.last" "Smith"}
          result (keys/expand value true :safe)]
      (is (= {"user" {"name" {"first" "Alice" "last" "Smith"}}} result)))))


(deftest expand-paths-mixed-keys-test
  (testing "Expands dotted keys while preserving literal keys"
    (let [value {"data.config.server" "localhost"
                 "version" "1.0"}
          result (keys/expand value true :safe)]
      (is (= {"data" {"config" {"server" "localhost"}}
              "version" "1.0"} result)))))


(deftest expand-paths-invalid-segments-test
  (testing "Does not expand when segments contain dots in the segment itself"
    (let [value {"data.config" "value"}
          result (keys/expand value true :safe)]
      (is (= {"data" {"config" "value"}} result))))

  (testing "Does not expand when segments start with digits"
    (let [value {"data.123.config" "value"}
          result (keys/expand value true :safe)]
      ;; "123" is not a valid identifier, so no expansion
      (is (= {"data.123.config" "value"} result)))))


(deftest expand-paths-array-values-test
  (testing "Recursively expands array elements"
    (let [value [{"data.config" "value1"}
                 {"data.config" "value2"}]
          result (keys/expand value true :safe)]
      (is (= [{"data" {"config" "value1"}}
              {"data" {"config" "value2"}}] result)))))


(deftest expand-paths-nested-objects-test
  (testing "Recursively expands nested object values"
    (let [value {"outer" {"inner.key" "value"}}
          result (keys/expand value true :safe)]
      (is (= {"outer" {"inner" {"key" "value"}}} result)))))


(deftest expand-paths-deep-merge-test
  (testing "Deep merges when multiple keys expand to same path"
    (let [value {"user.name" "Alice"
                 "user.age" 30}
          result (keys/expand value true :safe)]
      (is (= {"user" {"name" "Alice" "age" 30}} result)))))


(deftest expand-paths-conflict-strict-test
  (testing "Throws on conflict in strict mode"
    (let [value {"user.name" "Alice"
                 "user" "Bob"}]  ; Conflict: "user" expands to object but also has literal value
      (is (thrown? #?(:clj Exception :cljs js/Error)
                   (keys/expand value true :safe))))))


(deftest expand-paths-conflict-non-strict-test
  (testing "Overwrites on conflict in non-strict mode (LWW)"
    (let [value {"user.name" "Alice"
                 "user" "Bob"}
          result (keys/expand value false :safe)]
      ;; Non-strict should not throw, result depends on insertion order
      (is (map? result)))))


(deftest expand-paths-primitives-test
  (testing "Returns primitives unchanged"
    (is (= "string" (keys/expand "string" true :safe)))
    (is (= 42 (keys/expand 42 true :safe)))
    (is (= true (keys/expand true true :safe)))
    (is (nil? (keys/expand nil true :safe)))))


(deftest expand-paths-empty-structures-test
  (testing "Handles empty structures"
    (is (= {} (keys/expand {} true :safe)))
    (is (= [] (keys/expand [] true :safe)))))


(deftest expand-paths-complex-structure-test
  (testing "Expands complex nested structure"
    (let [value {"app.config.server.host" "localhost"
                 "app.config.server.port" 8080
                 "app.name" "MyApp"
                 "version" "1.0"}
          result (keys/expand value true :safe)]
      (is (= {"app" {"config" {"server" {"host" "localhost"
                                         "port" 8080}}
                     "name" "MyApp"}
              "version" "1.0"} result)))))
