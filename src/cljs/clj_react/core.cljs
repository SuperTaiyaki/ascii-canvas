(ns clj-react.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [accountant.core :as accountant]
   [clj-react.graphics :refer [pointer-in pointer-out pointer-new]]))

;; -------------------------
;; Routes

(def canvas-width 100) ;characters
(def cell-count 2000)

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

(defn action-page []
  (let [action-list (atom ["Nothing"])]
  (fn []
    [:span.main {:on-pointer-down (fn [e]
            (swap! action-list conj (str "Touch" (..  e -offsetX) " " (.. e -offsetY))))}
     [:p "Action list:"]
     [:ul (map (fn [action] [:li action]) @action-list)
      ]
     [:input {:type "button" :value "Add" :on-click (fn [] ( swap! action-list conj "Next"))}
      ]
     [:input {:type "button" :value "Add 2" :on-click (fn [] ( swap! action-list conj "Next 2"))}
      ]])))

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
             :on-pointer-enter (fn [e] (when (= (.. e -buttons) 1) (swap! cell pointer-in (.. e -clientX) (.. e -clientY))))
             :on-pointer-leave (fn [e] (when (= (.. e -buttons) 1) (swap! cell pointer-out (.. e -clientX) (.. e -clientY))))
             } (get @cell :display)]
     ]
    )))

(defn is-draw? [event]
  (if (=  (.. event -pointerType) "pen")
    (>  (.. event -pressure) 0)
    (= (.. event -buttons) 1))
  )

(defn ascii-page []
  ; huh, this layout isn't quite right. We want a new atom for every cell
  ; each cell should be its own data/renderer...
  (let [cells (repeatedly cell-count #(atom (pointer-new)))
        touch-on (fn [e cell] (println "Touch on:" (.. e -buttons) "Pressure" (.. e -pressure)) (when (is-draw? e) (swap! cell pointer-in (.. e -clientX) (.. e -clientY))))
        touch-off  (fn [e cell] (println "Touch off:" (.. e -buttons)) (when (is-draw? e) (swap! cell pointer-out (.. e -clientX) (.. e -clientY))))]
  (fn [] 
    [:div.main {:style {:width "800px" :font-family "monospace"
                        :white-space "pre-wrap" :word-break "break-all"
                        :border "1px solid black"
                        :line-height "150%"}}
     
     (interleave (map (fn [idx]  [:br {:style {:user-select "none" :pointer-events "none"
                                               :touch-action "none"} :key idx}]) (range))
                 (partition-all canvas-width 
     (doall (map-indexed (fn [idx cell] [:span {:style {:user-select "none"} :key idx
             ; need to store entry and exit to figure out what character to install
             ; is atom really the best way to hold state here??
             ; functional so update is a bit weird to start with
             ; is helix or hx better for this?
             ; anyway continue to prototype with this for now
             :on-pointer-enter #(touch-on % cell)
             ; :on-pointer-down #(touch-on % cell)
             ; :on-pointer-out #(touch-off % cell)
             :on-pointer-leave #(touch-off % cell)
             } (get @cell :display)]) cells))))
     [:br]
     [:input {:type "button" :value "Clear" :on-click (fn []
            (doall  (map (fn [cell] (reset! cell (pointer-new))) cells
                                                         )))}
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
        [:p [:a {:href (path-for :index)} "Home"] " | "
         [:a {:href (path-for :about)} "About clj-react"]]]
       [page]
       [:footer
        [:p "clj-react was generated by the "
         [:a {:href "https://github.com/reagent-project/reagent-template"} "Reagent Template"] "."]]])))

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
