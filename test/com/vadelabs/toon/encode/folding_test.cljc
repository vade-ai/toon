(ns com.vadelabs.toon.encode.folding-test
  (:require
    #?(:clj [clojure.test :refer [deftest is testing]]
       :cljs [cljs.test :refer [deftest is testing]])
    [com.vadelabs.toon.encode.folding :as folding]))


;; ============================================================================
;; Key Folding Tests
;; ============================================================================

(deftest folding-result-simple-chain-test
  (testing "Folds simple nested single-key objects"
    (let [value {"config" {"server" "localhost"}}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (some? result))
      (is (= "data.config.server" (:folded-key result)))
      (is (nil? (:remainder result)))
      (is (= "localhost" (:leaf-value result)))
      (is (= 3 (:segment-count result))))))


(deftest folding-result-disabled-test
  (testing "Returns nil when folding is disabled"
    (let [value {"config" {"server" "localhost"}}
          siblings []
          options {:key-folding :off :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (nil? result)))))


(deftest folding-result-non-object-test
  (testing "Returns nil for non-object values"
    (let [options {:key-folding :safe :flatten-depth ##Inf}
          siblings []]
      (is (nil? (folding/result "key" "string" siblings options)))
      (is (nil? (folding/result "key" 42 siblings options)))
      (is (nil? (folding/result "key" [1 2 3] siblings options))))))


(deftest folding-result-multi-key-object-test
  (testing "Returns nil for multi-key objects"
    (let [value {"a" 1 "b" 2}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "key" value siblings options)]
      (is (nil? result)))))


(deftest folding-result-two-segments-test
  (testing "Folds with exactly 2 segments"
    (let [value {"server" "localhost"}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (some? result))
      (is (= "data.server" (:folded-key result)))
      (is (= 2 (:segment-count result))))))


(deftest folding-result-invalid-segment-test
  (testing "Returns nil when segment contains dots"
    (let [value {"config" {"server.name" "localhost"}}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (nil? result))))

  (testing "Returns nil when segment contains slash"
    (let [value {"config" {"server/name" "localhost"}}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (nil? result))))

  (testing "Returns nil when segment starts with digit"
    (let [value {"config" {"123" "localhost"}}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (nil? result)))))


(deftest folding-result-collision-with-siblings-test
  (testing "Returns nil when folded key collides with sibling"
    (let [value {"config" {"server" "localhost"}}
          siblings ["data.config.server"]  ; Collision!
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (nil? result)))))


(deftest folding-result-partial-fold-test
  (testing "Partially folds when depth limit reached"
    (let [value {"a" {"b" {"c" {"d" "value"}}}}
          siblings []
          options {:key-folding :safe :flatten-depth 3}
          result (folding/result "root" value siblings options)]
      (is (some? result))
      (is (= "root.a.b" (:folded-key result)))
      (is (= {"c" {"d" "value"}} (:remainder result)))
      (is (= {"c" {"d" "value"}} (:leaf-value result)))
      (is (= 3 (:segment-count result))))))


(deftest folding-result-partial-fold-multi-key-test
  (testing "Partially folds when encountering multi-key object"
    (let [value {"config" {"settings" {"host" "localhost" "port" 8080}}}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (some? result))
      (is (= "data.config.settings" (:folded-key result)))
      (is (= {"host" "localhost" "port" 8080} (:remainder result)))
      (is (= {"host" "localhost" "port" 8080} (:leaf-value result)))
      (is (= 3 (:segment-count result))))))


(deftest folding-result-leaf-array-test
  (testing "Folds chain ending in array"
    (let [value {"config" {"tags" ["a" "b" "c"]}}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (some? result))
      (is (= "data.config.tags" (:folded-key result)))
      (is (nil? (:remainder result)))
      (is (= ["a" "b" "c"] (:leaf-value result)))
      (is (= 3 (:segment-count result))))))


(deftest folding-result-leaf-empty-object-test
  (testing "Folds chain ending in empty object"
    (let [value {"config" {"settings" {}}}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "data" value siblings options)]
      (is (some? result))
      (is (= "data.config.settings" (:folded-key result)))
      (is (nil? (:remainder result)))
      (is (= {} (:leaf-value result)))
      (is (= 3 (:segment-count result))))))


(deftest folding-result-valid-identifiers-test
  (testing "Folds with valid identifier segments"
    (let [value {"user_config" {"server_name" "localhost"}}
          siblings []
          options {:key-folding :safe :flatten-depth ##Inf}
          result (folding/result "app_data" value siblings options)]
      (is (some? result))
      (is (= "app_data.user_config.server_name" (:folded-key result))))))
