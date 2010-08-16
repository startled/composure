(ns composure.path-utils-test
  (:use [composure.path-utils] :reload-all)
  (:use [clojure.test]))

(deftest test-normalize
  (is (= "/A/B" (normalize-path "/A/B/C/..")))
  (is (= "/A/B" (normalize-path "/A/B/.")))
  (is (= "/A/B" (normalize-path "/A/B/C/../")))
  (is (= "/A/B" (normalize-path "/A/B/./")))
  (is (= "/A/B" (normalize-path "/A/C/../B/")))
  (is (= "A/B" (normalize-path "A/B")))
  (is (= "C:/Test" (normalize-path "C:/Test/a/.."))))

(deftest test-relative-path
  (is (= "../E/F"
	 (relative-path "/A/B/C/D"
			"/A/B/C/E/F"))))