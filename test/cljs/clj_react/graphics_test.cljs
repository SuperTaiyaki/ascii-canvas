(ns clj-react.graphics-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [clj-react.graphics :as SUT]))

(deftest gradient-check
  (testing "Something"
    (is (= 1 1))
    (is (= 2 2))))

(defn in-out 
  ([inx iny outx outy]
  (let [state (SUT/pointer-new)]
    (SUT/pointer-out (SUT/pointer-in state inx iny) outx outy)))
  ([c inx iny outx outy]
  (let [state (SUT/pointer-new c)]
    (SUT/pointer-out (SUT/pointer-in state inx iny) outx outy))))

; &rest is the better way...
(defn in-out-display
  ([inx iny outx outy] (get (in-out inx iny outx outy) :display))
  ([ch inx iny outx outy] (get  (in-out ch inx iny outx outy) :display))
  )

(deftest lines
  (testing "Horizontal"
    (is (= " " (get (SUT/pointer-new) :display)))
    (is (= "-" (in-out-display  0 5 10 5)))
    (is (= "-" (in-out-display 10 5 0 5)))
    )
  (testing "Vertical"
    ; non-zero case
    (is (= "|" (in-out-display  5 0 6 20)))
    (is (= "|" (in-out-display  5 20 6 0)))
    )
  (testing "divide-by-zero"
    ; doesn't need a special case???
    ; just goes to Inf and everything works!
    (is (= "|" (in-out-display  5 20 5 0)))
    )
  (testing "diagonal"
    (is (= "/" (in-out-display  0 20 20 0)))
    (is (= "\\" (in-out-display  0 0 20 20)))
    ))

(deftest combinations
  (testing "Plus"
    (is (= "+" (in-out-display "-" 5 20 5 0)))
  (testing "Cross")
  (testing "Star")
  )
)


( deftest intersections
  (testing "Plus"
    (is (= "+" (SUT/overlay "|" "-")))
    (is (= "+" (SUT/overlay "-" "|")))
    (is (= "X" (SUT/overlay "/" "\\")))
    )
  )
