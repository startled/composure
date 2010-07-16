(ns composure.file-watch
  (:import [name.pachler.nio.file Path Paths FileSystems WatchEvent
	    WatchKey StandardWatchEventKind ClosedWatchServiceException]))

(defn- watch-fn
  [watcher]
  (try
   (loop [signaled-key (.take watcher)]
     (doseq [event (.pollEvents signaled-key)]
       (let [kind (.kind event)
	     filename (.context event)]
	 (condp = kind
	   StandardWatchEventKind/ENTRY_CREATE
	   (println "Entry created: " filename)
	   StandardWatchEventKind/ENTRY_DELETE
	   (println "Entry deleted: " filename)
	   StandardWatchEventKind/ENTRY_MODIFY
	   (println "Entry modified: " filename)
	   StandardWatchEventKind/OVERFLOW
	   (println "Event overflow.")
	   ;; Default.
	   (println "Stuff happened."))))
     (if (.reset signaled-key)
       (recur (.take watcher))))
   ;; Catch this, because we want to just exit the main loop in this case.
   (catch ClosedWatchServiceException cwse)))

(defn watcher
  "Returns a new watch service inside an agent. You can call close
   on it, or change the watch options. Any arguments passed to this function
   are interpreted as file system paths to add to the watcher."
  [& paths]
  (let [watch-service (.. FileSystems (getDefault) (newWatchService))]
    (doseq [path paths]
      (let [dir (Paths/get path)]
	(.register dir watch-service
		   (into-array [StandardWatchEventKind/ENTRY_CREATE
				StandardWatchEventKind/ENTRY_DELETE
				StandardWatchEventKind/ENTRY_MODIFY]))))
    (.start (Thread. #(watch-fn watch-service)))
    watch-service))

(defn close
  [watcher]
  (.close watcher))