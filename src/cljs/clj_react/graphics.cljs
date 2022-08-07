(ns clj-react.graphics)

; smells like OO, probably because of the atom-ish stuff

(defn pointer-new
  ( [] {:display " "})
  ( [c] {:display c}))

(defn gradient [x1 y1 x2 y2]
  (/ (- y2 y1) (- x2 x1)))

(defn character [gradient]
  (if (and (> gradient -0.5) (< gradient 0.5))
    "-"
    ; confusing coordinate systems! computer-style (0-top)
    ; 1/2 to 2/1
    ( if (and (>= gradient 0.5) (< gradient 2.0))
    "\\"
    (if (and (> gradient -2.0) (<= gradient -0.5))
      "/"
    "|"))))

(defn overlay [char1 char2]
  (let [input (disj (set [char1 char2]) " ")]
  (if (= 1 (count input))
    (first input)
    (if (and  (contains? input "|") (contains? input "-"))
      "+"
      (if (and (contains? input "/") (contains? input "\\"))
        "X"
        "*")))))

(defn pointer-in
  [prev x y]
  (assoc prev :in-coord [x y])
  )

(defn pointer-out
  [prev x y]
  (let [prev-coord (get prev :in-coord)]
  (if prev-coord
    (assoc prev :display (overlay (get prev :display)  (character (gradient
                                      (first prev-coord)
                                      (second prev-coord)
                                      x y))))
    (assoc prev :display " ")
  ; prevy == y -> bar
  ; (println (str "In:" (get prev :in-coord) " Out: " x " " y))
  )
  ))


