(ns composure.path-utils
  (:require [clojure.contrib.str-utils2 :as str])
  (:import [java.io File]))

(def ^{:private true} separator-pattern
     (re-pattern File/separator))

(defn normalize-path
  "Cleans up a path so that it has no leading/trailing whitespace, and
   removes any same/parent path references."
  [filepath]
  (let [[prefix & path] (str/split filepath separator-pattern)]
    (loop [result (transient [prefix])
	   remaining-path path]
      (let [[curr & rest-remaining] remaining-path]
	(cond (nil? curr)
	      (str/join File/separator (persistent! result))
	      ;; If we see an empty string, we had repeated separators.
	      (= "" curr)
	      (recur result rest-remaining)
	      ;; If we see a ., we ignore it.
	      (= "." curr)
	      (recur result rest-remaining)
	      ;; If we see a .., we need to delete the last part we saw.
	      (= ".." curr)
	      (recur (pop! result)
		     rest-remaining)
	      :else
	      (recur (conj! result curr)
		     rest-remaining))))))

(defn- common-prefix
  "Given two collections, returns a sequence containing the prefix they
   share. Example: (common-prefix [\\a \\b] [\\a \\b \\c \\d]) -> (\\a \\b)"
  [coll1 coll2]
  (map first (take-while #(= (first %) (second %))
			 (map #(vector %1 %2) coll1 coll2))))

(defn- unique-suffix
  "Returns the elements of interesting-coll that are not part of the common
   prefix with uninteresting-coll."
  [uninteresting-coll interesting-coll]
  (let [common-parts (common-prefix uninteresting-coll interesting-coll)]
    (drop (count common-parts) interesting-coll)))

(defn relative-path
  "Takes two absolute paths, and returns a relative path that indicates
   the same file system location as destination-path, but relative to
   base-path.
   Note: By default, relative-path assumes you are sending in a directory
   for the first argument. It will check if the first argument is an actual
   entity on disk, and if it's a file, it will take that into account in
   doing the relative path calculation. If it isn't or it doesn't exist,
   it assumes you mean a directory."
  [base-path-str dest-path-str]
  (let [[base-prefix & base-path] (str/split (normalize-path base-path-str)
					     separator-pattern)
	[dest-prefix & dest-path] (str/split (normalize-path dest-path-str)
					     separator-pattern)
	common-path (common-prefix base-path dest-path)
	base-suffix (drop (count common-path) base-path)
	dest-suffix (drop (count common-path) dest-path)
	base-file (File. base-path-str)
	base-is-file? (if (.exists base-file)
			    (.isFile base-file)
			    false)
	file-seg (if base-is-file? 1 0)]
    (if (not= base-prefix dest-prefix)
      (throw (IllegalArgumentException. "Incompatible path prefixes.")))
    (if (nil? common-path)
      (throw (IllegalArgumentException. "Paths contain no common components.")))
    (str/join File/separator
	      (concat
	       (repeat (- (count base-suffix) file-seg) "..")
	       dest-suffix))))