(ns leiningen.pom
  "Write a pom.xml file to disk for Maven interop."
  (:require [lancet])
  (:use [clojure.contrib.duck-streams :only [writer]]
        [clojure.contrib.java-utils :only [file]])
  (:import [org.apache.maven.model Model Parent Dependency Repository]
           [org.apache.maven.project MavenProject]
           [org.apache.maven.artifact.ant Pom]))

(def #^{:doc "A notice to place at the bottom of generated files."} disclaimer
     "\n<!-- This file was autogenerated by the Leiningen build tool.
  Please do not edit it directly; instead edit project.clj and regenerate it.
  It should not be considered canonical data. For more information see
  http://github.com/technomancy/leiningen -->\n")

(defn make-dependency [[dep version]]
  (doto (Dependency.)
    (.setGroupId (or (namespace dep) (name dep)))
    (.setArtifactId (name dep))
    (.setVersion version)))

(defn make-repository [[id url]]
  (doto (Repository.)
    (.setId id)
    (.setUrl url)))

(def default-repos {"central" "http://repo1.maven.org/maven2"
                    "clojure-snapshots" "http://build.clojure.org/snapshots"})

(defn make-model [project]
  (let [model (doto (Model.)
                (.setModelVersion "4.0.0")
                (.setArtifactId (:name project))
                (.setName (:name project))
                (.setVersion (:version project))
                (.setGroupId (:group project))
                (.setDescription (:description project)))]
    (doseq [dep (:dependencies project)]
      (.addDependency model (make-dependency dep)))
    (doseq [repo (concat (:repositories project) default-repos)]
      (.addRepository model (make-repository repo)))
    model))

(defn make-pom [project]
  (doto (Pom.)
    (.setProject lancet/ant-project)
    (.setMavenProject (MavenProject. (make-model project)))))

(defn pom [project & [args]]
  (let [pom-file (file (:root project) "pom.xml")]
    (with-open [w (writer pom-file)]
      (when (or (not (.exists pom-file))
                (do (print "pom.xml exists; overwrite? ") (flush)
                    (re-find #"^y(es)?" (.toLowerCase (read-line)))))
        (.writeModel (MavenProject. (make-model project)) w)
        (.write w disclaimer)
        (println "Wrote pom.xml")))
    (.getAbsolutePath pom-file)))
