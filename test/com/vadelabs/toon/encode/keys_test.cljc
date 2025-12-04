(ns com.vadelabs.toon.encode.keys-test
  (:require
    #?(:clj [clojure.test :refer [deftest is testing]]
       :cljs [cljs.test :refer [deftest is testing]])
    [com.vadelabs.toon.encode.keys :as keys]))


;; ============================================================================
;; Key Collapsing Tests
;; ============================================================================

(deftest collapsing-result-simple-chain-test
  (testing "Collapses simple nested single-key objects"
    (is (= {:collapsed-key "data.config.server"
            :remainder nil
            :leaf-value "localhost"
            :segment-count 3}
           (keys/collapse "data"
                          {"config" {"server" "localhost"}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-disabled-test
  (testing "Returns nil when collapsing is disabled"
    (is (nil? (keys/collapse "data"
                             {"config" {"server" "localhost"}}
                             []
                             {:key-collapsing :off :flatten-depth ##Inf})))))


(deftest collapsing-result-non-object-test
  (testing "Returns nil for non-object values"
    (let [opts {:key-collapsing :safe :flatten-depth ##Inf}]
      (is (nil? (keys/collapse "key" "string" [] opts)))
      (is (nil? (keys/collapse "key" 42 [] opts)))
      (is (nil? (keys/collapse "key" [1 2 3] [] opts))))))


(deftest collapsing-result-multi-key-object-test
  (testing "Returns nil for multi-key objects"
    (is (nil? (keys/collapse "key"
                             {"a" 1 "b" 2}
                             []
                             {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-two-segments-test
  (testing "Collapses with exactly 2 segments"
    (is (= {:collapsed-key "data.server"
            :remainder nil
            :leaf-value "localhost"
            :segment-count 2}
           (keys/collapse "data"
                          {"server" "localhost"}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-invalid-segment-test
  (let [opts {:key-collapsing :safe :flatten-depth ##Inf}]
    (testing "Returns nil when segment contains dots"
      (is (nil? (keys/collapse "data" {"config" {"server.name" "localhost"}} [] opts))))

    (testing "Returns nil when segment contains slash"
      (is (nil? (keys/collapse "data" {"config" {"server/name" "localhost"}} [] opts))))

    (testing "Returns nil when segment starts with digit"
      (is (nil? (keys/collapse "data" {"config" {"123" "localhost"}} [] opts))))))


(deftest collapsing-result-collision-with-siblings-test
  (testing "Returns nil when collapsed key collides with sibling"
    (is (nil? (keys/collapse "data"
                             {"config" {"server" "localhost"}}
                             ["data.config.server"]  ; Collision!
                             {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-partial-fold-test
  (testing "Partially collapses when depth limit reached"
    (is (= {:collapsed-key "root.a.b"
            :remainder {"c" {"d" "value"}}
            :leaf-value {"c" {"d" "value"}}
            :segment-count 3}
           (keys/collapse "root"
                          {"a" {"b" {"c" {"d" "value"}}}}
                          []
                          {:key-collapsing :safe :flatten-depth 3})))))


(deftest collapsing-result-partial-fold-multi-key-test
  (testing "Partially collapses when encountering multi-key object"
    (is (= {:collapsed-key "data.config.settings"
            :remainder {"host" "localhost" "port" 8080}
            :leaf-value {"host" "localhost" "port" 8080}
            :segment-count 3}
           (keys/collapse "data"
                          {"config" {"settings" {"host" "localhost" "port" 8080}}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-leaf-array-test
  (testing "Collapses chain ending in array"
    (is (= {:collapsed-key "data.config.tags"
            :remainder nil
            :leaf-value ["a" "b" "c"]
            :segment-count 3}
           (keys/collapse "data"
                          {"config" {"tags" ["a" "b" "c"]}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-leaf-empty-object-test
  (testing "Collapses chain ending in empty object"
    (is (= {:collapsed-key "data.config.settings"
            :remainder nil
            :leaf-value {}
            :segment-count 3}
           (keys/collapse "data"
                          {"config" {"settings" {}}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-valid-identifiers-test
  (testing "Collapses with valid identifier segments"
    (is (= {:collapsed-key "app_data.user_config.server_name"
            :remainder nil
            :leaf-value "localhost"
            :segment-count 3}
           (keys/collapse "app_data"
                          {"user_config" {"server_name" "localhost"}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


;; ============================================================================
;; Edge Cases
;; ============================================================================

(deftest collapsing-result-underscore-prefix-test
  (testing "Collapses keys starting with underscore"
    (is (= {:collapsed-key "_data._private._config"
            :remainder nil
            :leaf-value "value"
            :segment-count 3}
           (keys/collapse "_data"
                          {"_private" {"_config" "value"}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-very-deep-chain-test
  (testing "Collapses very deep chain (10+ levels)"
    (is (= {:collapsed-key "root.a.b.c.d.e.f.g.h.i.j"
            :remainder nil
            :leaf-value "deep"
            :segment-count 11}
           (keys/collapse "root"
                          {"a" {"b" {"c" {"d" {"e" {"f" {"g" {"h" {"i" {"j" "deep"}}}}}}}}}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-root-literal-collision-test
  (testing "Returns nil when collapsed key collides with root literal"
    (is (nil? (keys/collapse "data"
                             {"config" {"server" "localhost"}}
                             []
                             {:key-collapsing :safe :flatten-depth ##Inf}
                             #{"data.config.server"}  ; Root-level dotted key
                             nil)))))


(deftest collapsing-result-mixed-case-identifiers-test
  (testing "Collapses with mixed case identifiers"
    (is (= {:collapsed-key "AppData.MyConfig.ServerName"
            :remainder nil
            :leaf-value "localhost"
            :segment-count 3}
           (keys/collapse "AppData"
                          {"MyConfig" {"ServerName" "localhost"}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-numeric-suffix-test
  (testing "Collapses keys with numeric suffixes"
    (is (= {:collapsed-key "data1.config2.server3"
            :remainder nil
            :leaf-value "localhost"
            :segment-count 3}
           (keys/collapse "data1"
                          {"config2" {"server3" "localhost"}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-single-level-no-fold-test
  (testing "Returns nil for single-level (only 1 segment total)"
    (is (nil? (keys/collapse "key"
                             "not-an-object"
                             []
                             {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-empty-object-chain-test
  (testing "Collapses chain with empty object at multiple levels"
    (is (= {:collapsed-key "root.a.b"
            :remainder nil
            :leaf-value {}
            :segment-count 3}
           (keys/collapse "root"
                          {"a" {"b" {}}}
                          []
                          {:key-collapsing :safe :flatten-depth ##Inf})))))


(deftest collapsing-result-flatten-depth-1-test
  (testing "Returns nil when flatten-depth is 1 (need 2+ for collapsing)"
    (is (nil? (keys/collapse "data"
                             {"config" {"server" "localhost"}}
                             []
                             {:key-collapsing :safe :flatten-depth 1})))))


(deftest collapsing-result-collision-with-nested-prefix-test
  (testing "Collapses when sibling is just a prefix"
    ;; Should still fold since "data.config.server" != "data.config"
    (is (= {:collapsed-key "data.config.server"
            :remainder nil
            :leaf-value "localhost"
            :segment-count 3}
           (keys/collapse "data"
                          {"config" {"server" "localhost"}}
                          ["data.config"]  ; Sibling that is prefix
                          {:key-collapsing :safe :flatten-depth ##Inf})))))
