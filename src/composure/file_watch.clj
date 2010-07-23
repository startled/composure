(ns composure.file-watch
  (:import [name.pachler.nio.file Path Paths FileSystems WatchEvent
	    WatchKey StandardWatchEventKind ClosedWatchServiceException]))


(defprotocol WatchReaction
  (on-create [this filename] "Function called on a file create event.")
  (on-delete [this filename] "Function called on a file delete event.")
  (on-modify [this filename] "Function called on a file modify event.")
  (on-overflow [this] "Function called when an event overflow happens."))

(defrecord DefaultFileWatcher [watcher]
  WatchReaction
  (on-create
   [this filename]
   (println "Entry created: " filename))
  (on-delete
   [this filename]
   (println "Entry deleted: " filename))
  (on-modify
   [this filename]
   (println "Entry modified: " filename))
  (on-overflow
   [this]
   (println "Event overflow.")))

(defn- watch-fn
  [watcher]
  (try
   (loop [signaled-key (.take (:watcher watcher))]
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
       (recur (.take (:watcher watcher)))))
   ;; Catch this, because we want to just exit the main loop in this case.
   (catch ClosedWatchServiceException cwse)))

(defn watcher
  "Returns a new watch service inside an agent. You can call close
   on it, or change the watch options. Any arguments passed to this function
   are interpreted as file system paths to add to the watcher."
  [& paths]
  (let [watch-service (DefaultFileWatcher.
			(.. FileSystems (getDefault) (newWatchService)))]
    (doseq [path paths]
      (let [dir (Paths/get path)]
	(.register dir (:watcher watch-service)
		   (into-array [StandardWatchEventKind/ENTRY_CREATE
				StandardWatchEventKind/ENTRY_DELETE
				StandardWatchEventKind/ENTRY_MODIFY]))))
    (.start (Thread. #(watch-fn watch-service)))
    watch-service))

(defn close
  [watcher]
  (.close (:watcher watcher)))