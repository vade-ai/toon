(ns com.vadelabs.toon.path-expansion-integration-test
  (:require
    #?(:clj [clojure.test :refer [deftest is testing]]
       :cljs [cljs.test :refer [deftest is testing]])
    [com.vadelabs.toon.core :as toon]))


;; ============================================================================
;; Path Expansion Integration Tests
;; ============================================================================

(deftest path-expansion-disabled-by-default-test
  (testing "Path expansion is disabled by default"
    (let [toon-str "data.config.server: localhost"
          result (toon/decode toon-str)]
      (is (= {"data.config.server" "localhost"} result)))))


(deftest path-expansion-simple-dotted-key-test
  (testing "Expands simple dotted key"
    (let [toon-str "data.config.server: localhost"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"data" {"config" {"server" "localhost"}}} result)))))


(deftest path-expansion-multiple-keys-test
  (testing "Expands multiple dotted keys"
    (let [toon-str "user.name.first: Alice\nuser.name.last: Smith"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"user" {"name" {"first" "Alice" "last" "Smith"}}} result)))))


(deftest path-expansion-mixed-keys-test
  (testing "Expands dotted keys while preserving literal keys"
    (let [toon-str "data.config.server: localhost\nversion: 1.0"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"data" {"config" {"server" "localhost"}}
              "version" 1.0} result)))))


(deftest path-expansion-with-arrays-test
  (testing "Expands paths in array values"
    (let [toon-str "[2]:\n  - data.config: value1\n  - data.config: value2"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= [{"data" {"config" "value1"}}
              {"data" {"config" "value2"}}] result)))))


(deftest path-expansion-roundtrip-test
  (testing "Roundtrip with collapsing and expansion"
    (let [original {"data" {"config" {"server" "localhost"}}}
          encoded (toon/encode original {:key-collapsing :safe})
          decoded (toon/decode encoded {:expand-paths :safe})]
      (is (= "data.config.server: localhost" encoded))
      (is (= original decoded)))))


(deftest path-expansion-complex-roundtrip-test
  (testing "Complex roundtrip with collapsing and expansion"
    (let [original {"app" {"name" "MyApp"}
                    "config" {"server" {"host" "localhost" "port" 8080}}}
          encoded (toon/encode original {:key-collapsing :safe})
          decoded (toon/decode encoded {:expand-paths :safe})]
      ;; After roundtrip, structure should be preserved (numbers become floats)
      (is (= "app.name: MyApp\nconfig.server:\n  host: localhost\n  port: 8080"
             encoded))
      (is (= {"app" {"name" "MyApp"}
              "config" {"server" {"host" "localhost" "port" 8080.0}}} decoded)))))


(deftest path-expansion-with-numeric-values-test
  (testing "Expands paths with numeric values"
    (let [toon-str "config.port: 8080\nconfig.timeout: 30"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"config" {"port" 8080.0 "timeout" 30.0}} result)))))


(deftest path-expansion-with-boolean-values-test
  (testing "Expands paths with boolean values"
    (let [toon-str "config.enabled: true\nconfig.debug: false"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"config" {"enabled" true "debug" false}} result)))))


(deftest path-expansion-with-null-values-test
  (testing "Expands paths with null values"
    (let [toon-str "config.value: null"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"config" {"value" nil}} result)))))


(deftest path-expansion-with-array-leaf-test
  (testing "Expands paths with array leaf values"
    (let [toon-str "config.tags[3]: a,b,c"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"config" {"tags" ["a" "b" "c"]}} result)))))


(deftest path-expansion-nested-object-roundtrip-test
  (testing "Roundtrip with deeply nested objects"
    (let [original {"level1" {"level2" {"level3" {"value" "deep"}}}}
          encoded (toon/encode original {:key-collapsing :safe})
          decoded (toon/decode encoded {:expand-paths :safe})]
      (is (= "level1.level2.level3.value: deep" encoded))
      (is (= original decoded)))))


(deftest path-expansion-partial-fold-roundtrip-test
  (testing "Roundtrip with partially collapsed structure"
    (let [original {"data" {"config" {"host" "localhost" "port" 8080}}}
          encoded (toon/encode original {:key-collapsing :safe})
          decoded (toon/decode encoded {:expand-paths :safe})]
      (is (= {"data" {"config" {"host" "localhost" "port" 8080.0}}} decoded)))))


(deftest path-expansion-invalid-segments-test
  (testing "Does not expand keys with invalid segments"
    (let [toon-str "data.123.config: value"
          result (toon/decode toon-str {:expand-paths :safe})]
      ;; "123" is not a valid identifier, so no expansion
      (is (= {"data.123.config" "value"} result)))))


(deftest path-expansion-multiple-independent-paths-test
  (testing "Expands multiple independent dotted paths"
    (let [toon-str "user.profile.name: Alice\napp.settings.theme: dark"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"user" {"profile" {"name" "Alice"}}
              "app" {"settings" {"theme" "dark"}}} result)))))


;; ============================================================================
;; Edge Cases
;; ============================================================================

(deftest path-expansion-underscore-prefix-test
  (testing "Expands keys starting with underscore"
    (let [toon-str "_data._config._server: localhost"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"_data" {"_config" {"_server" "localhost"}}} result)))))


(deftest path-expansion-very-deep-path-test
  (testing "Expands very deep dotted paths"
    (let [toon-str "a.b.c.d.e.f.g.h.i.j: deep"
          result (toon/decode toon-str {:expand-paths :safe})
          expected {"a" {"b" {"c" {"d" {"e" {"f" {"g" {"h" {"i" {"j" "deep"}}}}}}}}}}]
      (is (= expected result)))))


(deftest path-expansion-mixed-case-test
  (testing "Expands mixed case identifiers"
    (let [toon-str "AppData.MyConfig.ServerName: localhost"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"AppData" {"MyConfig" {"ServerName" "localhost"}}} result)))))


(deftest path-expansion-numeric-suffix-test
  (testing "Expands keys with numeric suffixes"
    (let [toon-str "data1.config2.server3: localhost"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"data1" {"config2" {"server3" "localhost"}}} result)))))


(deftest path-expansion-overlapping-paths-test
  (testing "Deep merges overlapping paths"
    (let [toon-str "user.profile.name: Alice\nuser.profile.age: 30\nuser.settings.theme: dark"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"user" {"profile" {"name" "Alice" "age" 30.0}
                      "settings" {"theme" "dark"}}} result)))))


(deftest path-expansion-roundtrip-underscore-test
  (testing "Roundtrip with underscore-prefixed keys"
    (let [original {"_data" {"_config" {"_server" "localhost"}}}
          encoded (toon/encode original {:key-collapsing :safe})
          decoded (toon/decode encoded {:expand-paths :safe})]
      (is (= original decoded)))))


(deftest path-expansion-roundtrip-very-deep-test
  (testing "Roundtrip with very deep structures"
    (let [original {"a" {"b" {"c" {"d" {"e" {"f" "deep"}}}}}}
          encoded (toon/encode original {:key-collapsing :safe})
          decoded (toon/decode encoded {:expand-paths :safe})]
      (is (= original decoded)))))


(deftest path-expansion-with-nested-arrays-test
  (testing "Expands paths with nested arrays"
    (let [toon-str "data.items[3]: a,b,c\ndata.count: 3"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"data" {"items" ["a" "b" "c"]
                      "count" 3.0}} result)))))


(deftest path-expansion-empty-objects-chain-test
  (testing "Expands key with no value to nil"
    (let [toon-str "a.b.c:"
          result (toon/decode toon-str {:expand-paths :safe})]
      ;; TOON decodes "key:" as {"key" nil}, not {"key" {}}
      (is (= {"a" {"b" {"c" nil}}} result)))))


(deftest path-expansion-mixed-expanded-and-literal-test
  (testing "Handles mix of expanded dotted keys and literal nested structures"
    (let [toon-str "user.name: Alice\nconfig:\n  server.host: localhost"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"user" {"name" "Alice"}
              "config" {"server" {"host" "localhost"}}} result)))))


(deftest path-expansion-invalid-empty-segments-test
  (testing "Does not expand keys with empty segments"
    (let [toon-str "data..config: value"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"data..config" "value"} result)))))


(deftest path-expansion-trailing-dot-test
  (testing "Trailing dots are stripped during decode"
    (let [toon-str "data.config.: value"
          result (toon/decode toon-str {:expand-paths :safe})]
      ;; TOON decoder strips trailing dots from keys
      (is (= {"data" {"config" "value"}} result)))))


(deftest path-expansion-leading-dot-test
  (testing "Does not expand keys with leading dots"
    (let [toon-str ".data.config: value"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {".data.config" "value"} result)))))


(deftest path-expansion-array-elements-with-dotted-keys-test
  (testing "Expands dotted keys within array elements"
    (let [toon-str "[2]:\n  - user.name: Alice\n  - user.name: Bob"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= [{"user" {"name" "Alice"}}
              {"user" {"name" "Bob"}}] result)))))


(deftest path-expansion-complex-mixed-structure-test
  (testing "Expands complex structure with mixed nesting and dotted keys"
    (let [toon-str "app:\n  name: MyApp\n  config.server.host: localhost\nversion: 2"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"app" {"name" "MyApp"
                     "config" {"server" {"host" "localhost"}}}
              "version" 2.0} result)))))


;; ============================================================================
;; Quoted Key Tests - Keys that should NOT be expanded
;; ============================================================================

(deftest quoted-key-not-expanded-test
  (testing "Quoted dotted key is not expanded"
    (let [toon-str "\"data.metadata\": value"
          result (toon/decode toon-str {:expand-paths :safe})]
      ;; Quoted key should remain as literal, not expanded
      (is (= {"data.metadata" "value"} result)))))


(deftest quoted-key-mixed-with-unquoted-test
  (testing "Mix of quoted and unquoted dotted keys"
    (let [toon-str "\"data.metadata\": literal\nuser.name: Alice"
          result (toon/decode toon-str {:expand-paths :safe})]
      ;; Quoted key stays literal, unquoted key gets expanded
      (is (= {"data.metadata" "literal"
              "user" {"name" "Alice"}} result)))))


(deftest quoted-key-in-nested-object-test
  (testing "Quoted key in nested object is not expanded"
    (let [toon-str "config:\n  \"server.host\": localhost\n  port: 8080"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"config" {"server.host" "localhost"
                        "port" 8080.0}} result)))))


(deftest quoted-key-in-list-item-test
  (testing "Quoted key in list item is not expanded"
    (let [toon-str "[2]:\n  - \"user.name\": Alice\n  - \"user.name\": Bob"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= [{"user.name" "Alice"}
              {"user.name" "Bob"}] result)))))


(deftest quoted-vs-unquoted-different-keys-test
  (testing "Quoted and unquoted dotted keys with different names"
    ;; Note: Same key string (e.g., a.b and "a.b") would create duplicate keys
    ;; which isn't valid. This test uses different keys to demonstrate the behavior.
    (let [toon-str "a.b: expanded\n\"c.d\": literal"
          result (toon/decode toon-str {:expand-paths :safe})]
      ;; Unquoted key expands, quoted key stays literal
      (is (= {"a" {"b" "expanded"}
              "c.d" "literal"} result)))))


(deftest quoted-key-complex-dots-test
  (testing "Quoted key with multiple dots stays literal"
    (let [toon-str "\"a.b.c.d.e\": deep-literal"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"a.b.c.d.e" "deep-literal"} result)))))


(deftest quoted-key-with-array-value-test
  (testing "Quoted key with array value is not expanded"
    (let [toon-str "\"config.tags\"[3]: a,b,c"
          result (toon/decode toon-str {:expand-paths :safe})]
      (is (= {"config.tags" ["a" "b" "c"]} result)))))


(deftest quoted-key-roundtrip-preservation-test
  (testing "Roundtrip preserves structure when key collapsing creates dotted keys"
    ;; When we encode with key-collapsing, then decode with expand-paths,
    ;; we should get back the original structure
    (let [original {"user" {"profile" {"name" "Alice"}}}
          encoded (toon/encode original {:key-collapsing :safe})
          decoded (toon/decode encoded {:expand-paths :safe})]
      (is (= "user.profile.name: Alice" encoded))
      (is (= original decoded)))))


(deftest quoted-key-without-expansion-test
  (testing "Quoted key without expansion enabled stays literal"
    (let [toon-str "\"data.config\": value"
          result (toon/decode toon-str)]  ;; No :expand-paths option
      (is (= {"data.config" "value"} result)))))


(deftest unquoted-key-without-expansion-test
  (testing "Unquoted dotted key without expansion enabled stays literal"
    (let [toon-str "data.config: value"
          result (toon/decode toon-str)]  ;; No :expand-paths option
      (is (= {"data.config" "value"} result)))))
