(ns composure.core
  (:use clojure.contrib.def)
  (:import [com.google.javascript.jscomp.deps DependencyInfo]))

(def *closure-library-directory* nil)
(def *source-directory* nil)
(def *output-directory* nil)

(defvar- bundles (atom {})
  "Central storage for bundle information. Keyed by bundle name.")

(defvar- dependencies (atom {})
  "Central storage location for the depencency graph (DependencyInfo objects).
   Keyed by namespace.")

(defn- dependencies-of
  [^DependencyInfo di]
  (seq (.getRequires di)))

(defn- calculate-requirements
  "Given a seq of namespaces, recursively captures all files required
   by this set for compilation."
  [input-list]
  (loop [[ns & rest-ns] input-list
	 output (transient #{})]
    (cond (nil? ns)
	  (persistent! output)
	  ;; Haven't already added this ns.
	  (not (output ns))
	  (recur (concat rest-ns
			 (dependencies-of ns))
		 (conj output ns))
	  ;; Otherwise, already seen this ns.
	  :else
	  (recur rest-ns output))))

(defn bundle
  "Gives a name to a bundle that should be made by compiling a bunch of
   namespaces. After the name is a list of namespaces that should be
   included in the compiled bundle; their dependencies will recursively be
   compiled in."
  [name & inputs]
  (let [requires (calculate-requirements inputs)]