(ns threes.fx
  (:require
   [threes.dom :as dom]))

(def styles
  {:status
   {:background-color "#222"
    :border-top: "1px solid #666"
    :color "dodgerblue"
    :font-size "10pt"
    :font-family "monospace"
    :overflow "hidden"
    :position "fixed"
    :height "100px"
    :left "0"
    :right "0"
    :bottom "0"
    :padding "5px"
    :box-sizing "border-box"
    :-webkit-box-sizing "border-box"}
   :brick
   {:font-size "20px"
    :font-family "Helvetica Neue"
    :font-weight "400"
    :border "1px solid #555"
    :border-radius "3px"
    :width "300px"
    :height "100px"
    :color "white"
    :background-color "#369"
    :text-align "center"
    :box-sizing "border-box"
    :-webkit-box-sizing "border-box"}})

;;-----------------------------------------------------------------------------

(defn mk-scene
  []
  (new js/THREE.Scene))

(defn mk-camera
  [z]
  (let [c (js/THREE.PerspectiveCamera. 75 (/ 4 3) 0.1 10000)]
    (set! (.. c -position -z) z)
    c))

(defn mk-renderer
  [width height]
  (doto (js/THREE.CSS3DRenderer.)
    (.setSize width height)
    (.setClearColor 0xCCCCCC 1)))

(defn mk-brick
  [id w h c]
  (-> (dom/mk-element "div" id)
      (dom/set-html! (str id))
      (dom/set-style! (merge (:brick styles)
                         {:width (str w "px")
                          :height (str h "px")
                          :background-color c}))
      (dom/mk-3d)))

(defn set-pos!
  [object {:keys [x y z]}]
  (.set (.-position object) x y z))
