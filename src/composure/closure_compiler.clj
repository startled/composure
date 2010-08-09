(ns composure.closure-compiler
  (:refer-clojure :exclude [compile])
  (:import [java.util.zip ZipEntry ZipInputStream]
	   [com.google.common.io LimitInputStream]
	   [com.google.javascript.jscomp BasicErrorManager
	    ClosureCodingConvention CommandLineRunner CompilationLevel
	    WarningLevel CompilerOptions CompilerOptions$DevMode JSSourceFile]
	   [com.google.javascript.jscomp.deps DependencyInfo JsFileParser]))

(defn default-externs
  "Returns a list of JSSourceFiles containing the contents of the default
   extern files."
  []
  (let [input (.getResourceAsStream (Class/forName "CommandLineRunner")
				    "/externs.zip")
	zip (ZipInputStream. input)]
	(loop [externs []
	       entry (.getNextEntry zip)]
	  (if (nil? entry)
	    externs
	    (let [entryStream (LimitInputStream. zip (.getSize entry))]
	      (recur (conj externs
			   (JSSourceFile/fromInputStream (.getName entry)
							 entryStream))
		     (.getNextEntry zip)))))))

(defn default-options
  "Returns a CompilerOptions object with default values."
  []
  (let [options (CompilerOptions.)
	compilation-level CompilationLevel/SIMPLE_OPTIMIZATIONS
	warning-level WarningLevel/DEFAULT]
    (.setCodingConvention options (ClosureCodingConvention.))
    (.setOptionsForCompilationLevel options)
    (.setOptionsForWarningLevel options)
    options))

(defn parse-dependencies
  [file-path]
  (let [deps-parser (JsFileParser. (BasicErrorManager.))]
    ;; Note the bad behavior below: we're passing an empty string in for the
    ;; closure relative path. This is because dealing with the closure relative
    ;; path is a pain in the ass we don't need, and all the deps parser does is
    ;; store it for use in the DependencyInfo object, which will also not use
    ;; it unless you want to build a deps.js. Which we also don't need.
    (.parseFile deps-parser file-path "")))

(defn compile
  [js-files externs options]
  (let [compiler (com.google.javascript.jscomp.Compiler.)
	file-array (into-array (map #(JSSourceFile/fromFile %) js-files))
	extern-array (into-array (map #(JSSourceFile/fromFile %) externs))]
    (.compile compiler extern-array file-array options)))