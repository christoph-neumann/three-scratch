(ns threes.dom
  (:require
    [clojure.string :as s]))

(defn- clj->css
  [style-map]
  (->> style-map
       (map #(let [[k v] %] (str (name k) ": " v "; ")))
       (apply str)
       (s/trim)))

(defn win-height
  []
  (.-innerHeight js/window))

(defn win-width
  []
  (.-innerWidth js/window))

(defn screen-h
  [width]
  (/ (* width 3) 4))

(defn aspect
  []
  [(win-width) (screen-h (win-width))])

(defn by-id
  [id]
  (.getElementById js/document id))

(defn mk-element
  [tag id]
  (doto (.createElement js/document tag)
    (.setAttribute "id" id)))

(defn set-html!
  [el html]
  (set! (.-innerHTML el) html)
  el)

(defn set-style!
  [el style]
  (set! (.. el -style -cssText)
        (clj->css style))
  el)

(defn append-child!
  [el place]
  (.appendChild place el)
  el)

(defn mk-3d
  [el]
  ;; Can't set size on this object. Has no function
  ;; allowing it!
  (new js/THREE.CSS3DSprite el))

(defn listen!
  ([el event cb]
   (.addEventListener el event cb false))
  ([event cb]
   (listen! js/window event cb)))
