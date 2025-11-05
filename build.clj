(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'com.vadelabs/toon)

(defn- date-commit-count-version []
  (let [date (.format (java.time.LocalDate/now)
                      (java.time.format.DateTimeFormatter/ofPattern "yyyy.MM.dd"))
        today-midnight (str date "T00:00:00")]
    (try
      (let [proc (.exec (Runtime/getRuntime) (into-array String ["git" "log" "--since" today-midnight "--oneline"]))
            _ (.waitFor proc)
            output (slurp (.getInputStream proc))
            commit-count (if (empty? output) 0 (count (clojure.string/split-lines output)))]
        (format "%s.%d" date commit-count))
      (catch Exception _
        ;; Fallback if git is not available
        (format "%s.0" date)))))

(def version (date-commit-count-version))
(def class-dir "target/classes")

(defn- pom-template [version]
  [[:description "TOON (Token-Oriented Object Notation) - A compact data format optimized for LLMs, reducing token usage by 30-60% compared to JSON while maintaining readability"]
   [:url "https://github.com/vadelabs/toon"]
   [:licenses
    [:license
     [:name "Eclipse Public License"]
     [:url "http://www.eclipse.org/legal/epl-v10.html"]]]
   [:developers
    [:developer
     [:name "Pragyan"]
     [:email "pragyan@vadelabs.com"]]]
   [:scm
    [:url "https://github.com/vadelabs/toon"]
    [:connection "scm:git:https://github.com/vadelabs/toon.git"]
    [:developerConnection "scm:git:ssh:git@github.com:vadelabs/toon.git"]
    [:tag (str "v" version)]]])

(defn- jar-opts [opts]
  (assoc opts
          :lib lib   :version version
          :jar-file  (format "target/%s-%s.jar" lib version)
          :basis     (b/create-basis {})
          :class-dir class-dir
          :target    "target"
          :src-dirs  ["src"]
          :pom-data  (pom-template version)))

(defn jar "Build the JAR." [opts]
  (b/delete {:path "target"})
  (let [opts (jar-opts opts)]
    (println "\nWriting pom.xml...")
    (b/write-pom opts)
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
    (println "\nBuilding JAR..." (:jar-file opts))
    (b/jar opts))
  opts)

(defn install "Install the JAR locally." [opts]
  (let [opts (jar-opts opts)]
    (b/install opts))
  opts)

(defn deploy "Deploy the JAR to Clojars." [opts]
  (let [{:keys [jar-file] :as opts} (jar-opts opts)]
    (dd/deploy {:installer :remote :artifact (b/resolve-path jar-file)
                :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))}))
  opts)
