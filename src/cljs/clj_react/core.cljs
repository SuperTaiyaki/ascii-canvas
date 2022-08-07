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

(def router
  (reitit/router
   [["/" :index]
    ["/items"
     ["" :items]
     ["/:item-id" :item]]
    ["/about" :about]
    ["/actions" :action]
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
      [:li [:a {:href (path-for :action)} "Action test"]]
      [:li [:a {:href (path-for :canvas)} "Canvas test"]]
      [:li [:a {:href (path-for :ascii)} "ascii test"]]
      [:li [:a {:href (path-for :items)} "Items of clj-react"]]
      [:li [:a {:href "/broken/link"} "Broken link 3"]]
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
             :on-pointer-enter (fn [e] (swap! cell pointer-in (.. e -clientX) (.. e -clientY)))
             :on-pointer-leave (fn [e] (when (= (.. e -buttons) 1) (swap! cell pointer-out (.. e -clientX) (.. e -clientY))))
             } (get @cell :display)]
     ]
    )))

(defn ascii-page []
  ; huh, this layout isn't quite right. We want a new atom for every cell
  (let [cells (repeatedly 500 #(atom (pointer-new)))]
  (fn [] 
    [:div.main {:style {:width "400px" :line-break "anywhere" :font-family "monospace"}}
     (doall (map-indexed (fn [idx cell] [:span {:style {} :key idx
             ; need to store entry and exit to figure out what character to install
             ; is atom really the best way to hold state here??
             ; functional so update is a bit weird to start with
             ; is helix or hx better for this?
             ; anyway continue to prototype with this for now
             :on-pointer-enter (fn [e] (swap! cell pointer-in (.. e -clientX) (.. e -clientY)))
             :on-pointer-leave (fn [e] (when (= 1 (.. e -buttons)) (swap! cell pointer-out (.. e -clientX) (.. e -clientY))))
             } (get @cell :display)]) cells))
     ])))


(defn items-page []
  (fn []
    [:span.main
     [:h1 "The items of clj-react"]
     [:ul (map (fn [item-id]
                 [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
               (range 1 60))]]))


(defn item-page []
  (fn []
    (let [routing-data (session/get :route)
          item (get-in routing-data [:route-params :item-id])]
      [:span.main
       [:h1 (str "Item " item " of clj-react")]
       [:p [:a {:href (path-for :items)} "Back to the list of items"]]])))


(defn about-page []
  (fn [] [:span.main
          [:h1 "About clj-react"]]))


;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :about #'about-page
    :items #'items-page
    :item #'item-page
    :action #'action-page
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
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))

(defn ^:dev/after-load reload! []
  (mount-root))
