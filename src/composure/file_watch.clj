(ns composure.file-watch
  (:import [name.pachler.nio.file Path Paths FileSystems WatchEvent
	    WatchKey StandardWatchEventKind ClosedWatchServiceException]
	   [java.io File]))

;; Utility Functions

(defn- all-subdirectories
  "Given a file (java.io.File), returns a sequence containing all
   subdirectories (not files)."
  [^File file]
  (filter #(.isDirectory %) (file-seq file)))

(defn- absolute-path
  "Get a string containing the absolute path from a Path object."
  [^Path path]
  (.. path (getFile) (getAbsolutePath)))

;; Datatypes

(defprotocol WatchReaction
  (on-create [this filename] "Function called on a file create event.")
  (on-delete [this filename] "Function called on a file delete event.")
  (on-modify [this filename] "Function called on a file modify event.")
  (on-overflow [this] "Function called when an event overflow happens."))

(defrecord DefaultFileWatcher [service]
  WatchReaction
  (on-create
   [this path]
   (println "Entry created: " (absolute-path path)))
  (on-delete
   [this path]
   (println "Entry deleted: " (absolute-path path)))
  (on-modify
   [this path]
   (println "Entry modified: " (absolute-path path)))
  (on-overflow
   [this]
   (println "Event overflow.")))

(defn- watch-fn
  [watcher]
  (try
   (loop [signaled-key (.take (:service watcher))]
     (doseq [event (.pollEvents signaled-key)]
       (let [kind (.kind event)
	     filename (.context event)]
	 (condp = kind
	   StandardWatchEventKind/ENTRY_CREATE
	   (on-create watcher filename)
	   StandardWatchEventKind/ENTRY_DELETE
	   (on-delete watcher filename)
	   StandardWatchEventKind/ENTRY_MODIFY
	   (on-modify watcher filename)
	   StandardWatchEventKind/OVERFLOW
	   (on-overflow watcher)
	   ;; Default.
	   (println "Stuff happened."))))
     (if (.reset signaled-key)
       (recur (.take (:service watcher)))))
   ;; Catch this, because we want to just exit the main loop in this case.
   (catch ClosedWatchServiceException cwse)))

(defn watch
  "Add additional paths to watch to an already-running watch service."
  [watcher & paths]
  (let [watch-service (:service watcher)]
    (doseq [path paths]
      (let [dir (Paths/get path)]
	(.register dir (:service watcher)
		   (into-array [StandardWatchEventKind/ENTRY_CREATE
				StandardWatchEventKind/ENTRY_DELETE
				StandardWatchEventKind/ENTRY_MODIFY]))))))

(defn watch-tree
  "Add an additional path to watch to an already-running watch service. If the
   path has subdirectories, those subdirectories are also added."
  [watcher & paths]
  ;; We don't check for duplicates because the watch service will take care of
  ;; that for us.
  (doseq [path paths]
    (let [all-subdirs (map str (all-subdirectories (File. path)))]
      (apply watch watcher all-subdirs)))) ;; Let watch do the real work.

(defn watcher
  "Returns a new watch service inside an agent. You can call close
   on it, or change the watch options. Any arguments passed to this function
   are interpreted as file system paths to add to the watcher, along with
   their recursive subdirectories. If you don't want the subdirectories,
   add paths yourself using the function watch."
  [& paths]
  (let [watcher (DefaultFileWatcher.
			(.. FileSystems (getDefault) (newWatchService)))]
    (apply watch-tree watcher paths)
    (.start (Thread. #(watch-fn watcher)))
    watcher))

(defn close
  "Stop and close a running watch service."
  [watcher]
  (.close (:service watcher)))