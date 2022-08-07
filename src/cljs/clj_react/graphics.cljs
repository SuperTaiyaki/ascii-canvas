(ns clj-react.graphics)

; smells like OO, probably because of the atom-ish stuff

(defn pointer-new []
  {:display "_"})

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

(defn pointer-in
  [prev x y]
  (assoc prev :in-coord [x y])
  )

(defn pointer-out
  [prev x y]
  (let [prev-coord (get prev :in-coord)]
  (if prev-coord
    (assoc prev :display (character (gradient
                                      (first prev-coord)
                                      (second prev-coord)
                                      x y)))
    (assoc prev :display "-")
  ; prevy == y -> bar
  ; (println (str "In:" (get prev :in-coord) " Out: " x " " y))
  )
  ))


