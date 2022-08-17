(ns clj-react.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [accountant.core :as accountant]
   [clj-react.graphics :refer [pointer-in pointer-out pointer-new erase]]))

;; -------------------------
;; Routes

(def canvas-width 60) ;characters
(def cell-count 1200)

(def router
  (reitit/router
   [["/" :index]
    ["/index.html" :index_html]
    ["/items"
     ["" :items]
     ["/:item-id" :item]]
    ["/canvas" :canvas]
    ["/ascii" :ascii]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; -------------------------
;; Page components

(defn home-page []
  (fn []
    [:span.main
     [:h1 "Welcome to clj-react"]
     [:ul
      [:li [:a {:href (path-for :canvas)} "Canvas test"]]
      [:li [:a {:href (path-for :ascii)} "ascii test"]]
      [:li [:a {:href "/broken/link"} "Broken link"]]]]))

(defn dump-coords [detail event ]
  (println "X:" (. event -clientX) " Y: " (. event -clientY) "Buttons: " (. event -buttons) "Pressure: " (. event -pressure) "Device: "  (. event -pointerType) "Source:" detail))

(defn canvas-page []
  (let [pixels (atom [])
        cell (atom ( pointer-new))]
  (fn []
    [:span.main
     [:svg {:style  {:width "500px" :height "500px"}
      :on-pointer-enter (fn [e] (println "Buttons: " (.. e -buttons)))
      :on-pointer-leave (fn [e] (println "Buttons: " (.. e -buttons)))
      :on-pointer-down (fn [e]
                          (swap! pixels conj [(.. e -clientX) (.. e -clientY)]))}
      (map (fn [el] [:circle {:r 2 :cx (first el) :cy (second el) :fill "green"}]) @pixels)
      ]
     [:span {:style {:border "1px solid black"}
             ; need to store entry and exit to figure out what character to install
             ; is atom really the best way to hold state here??
             ; functional so update is a bit weird to start with
             ; is helix or hx better for this?

             ; probably want to create a pointer-start and pointer-end methods that operate on a state object...
             ; anyway continue to prototype with this for now
             :on-pointer-enter (fn [e] (when (= (. e -buttons) 1) (swap! cell pointer-in (. e -clientX) (. e -clientY))) true)
             :on-pointer-leave (fn [e] (when (= (. e -buttons) 1) (swap! cell pointer-out (. e -clientX) (. e -clientY))) true)
             } (get @cell :display)]
     ]
    )))

(defn pointer-state [event]
  (if (=  (.. event -pointerType) "pen")
    (if (= (. event -buttons) 5)
      :erase
      (if (> (. event -pressure) 0)
        :draw
        :nop))
    (if (>  (. event -buttons) 1)
      :erase
      (if  (= (.. event -buttons) 1)
        :draw
        :nop)
      )))

(defn input-active? [event]
  (case (. event -pointerType)
    "mouse" (.. js/document (getElementById "mouseEnabled") -checked)
    "pen" (.. js/document (getElementById "stylusEnabled") -checked)
    "touch" (.. js/document (getElementById "touchEnabled") -checked)
    false
    ))


(defn printable? [key]
  (=  (. key -length) 1)
  )

(defn ascii-page []
  ; huh, this layout isn't quite right. We want a new atom for every cell
  ; each cell should be its own data/renderer...
  (let [cells (repeatedly cell-count #(atom (pointer-new)))
        cursor (atom 0)
        grid-on? (atom true)
        touch-on  (fn [e cell idx]  (case (pointer-state e)
                                  :draw (do  
                                          (swap! cell pointer-in (. e -clientX) (. e -clientY))
                                          (reset! cursor idx))
                                  :erase (swap! cell erase)
                                  ()))
        touch-off (fn [e cell idx]  (case  (pointer-state e)
                                  :draw (swap! cell pointer-out (. e -clientX) (. e -clientY))
                                  ()))]
  (fn [] 
    [:div.main {:style {:width "800px" :font-family "monospace"
                        :white-space "pre-wrap" :word-break "break-all"
                        :border "1px solid black"
                        :line-height "150%" :touch-action "none"}
                :tabIndex -1 ; catch keyboard input
                :on-key-down (fn [e]
                               (when (printable? (. e -key))
                               (do
                                 (reset! (nth cells @cursor) (pointer-new (. e -key)))
                                 (swap! cursor inc))))}
     [:label
      [:input {:type "checkbox" :name "mouse" :defaultChecked "yes" :id "mouseEnabled"} ]
      [:span "Mouse"]]
     [:label
      [:input {:type "checkbox" :name "stylus" :defaultChecked "yes" :id "stylusEnabled"} ]
      [:span "Stylus"]]
     [:label
      [:input {:type "checkbox" :name "touch" :defaultChecked "yes" :id "penEnabled"} ]
      [:span "Touch"]]
     
     (interleave (map (fn [idx]  [:br {:style {:user-select "none" :pointer-events "none"
                                               :touch-action "none"} :key idx}]) (range))
                 (partition-all canvas-width 
     (doall (map-indexed (fn [idx cell] [:span {:style {:user-select "none" :touch-action "none"
                                                        :border (if @grid-on? "1px solid #CCC" "none")
                                                        :padding (if @grid-on? "0px" "1px")
                                                        } :key idx
             ; need to store entry and exit to figure out what character to install
             ; is atom really the best way to hold state here??
             ; functional so update is a bit weird to start with
             ; is helix or hx better for this?
             ; anyway continue to prototype with this for now
             :on-pointer-enter (fn [e] (when (input-active? e)
                                                (dump-coords "enter" e) (touch-on e cell idx) (. (. e -target) (releasePointerCapture (. e -pointerId)))) true)
             :on-pointer-down (fn [e] (when (input-active? e) (dump-coords "down" e) (touch-on e cell idx) (. (. e -target) (releasePointerCapture (. e -pointerId)))) true)
             ; there should be a less clunky way to get to releasePointerCapture
             :on-pointer-up (fn [e] (touch-off e cell idx) true)
             :on-pointer-leave (fn [e] (touch-off e cell idx) true)

             :on-drag-start (fn [e] false)
             ; Why is this skipping evets? my thinkpad too slow?
             } (get @cell :display)]) cells))))
     [:br]
     [:label
      [:input {:type "checkbox" :name "showGrid" :defaultChecked "yes" :id "showGrid"
               :on-change (fn [e] (reset! grid-on? (.. e -target -checked)))} ]
      [:span "Show grid"]]
     [:input {:type "button" :value "Clear" :on-click (fn []
            (doall  (map (fn [cell] (reset! cell (pointer-new))) cells
                                                         )))}
      ]
     [:div
      [:ul
       [:li "Draw with any device" ]
       [:li "Stylus works better with mouse disabled"]
       [:li "Erase with any button other than primary (middle mouse, stylus #2 on my devices)"]
       [:li "Enter text with the keyboard at the last clicked location"]
       ]
      ]
     ])))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :index_html #'home-page
    :canvas #'canvas-page
    :ascii #'ascii-page))


;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header
        ]
       [page]
       [:footer
        ]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router "/ascii")
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (session/put! :route {:current-page  #'ascii-page  ;(page-for current-page)
                              :route-params route-params})
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))

(defn ^:dev/after-load reload! []
  (mount-root))
