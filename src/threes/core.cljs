(ns threes.core
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<! timeout close! chan alts! put!]]
   [clojure.string :as s]
   [weasel.repl :as ws-repl]))

(enable-console-print!)

(ws-repl/connect "ws://localhost:9001")

;;-----------------------------------------------------------------------------
;; DOM utilities

(defn clj->css
  [style-map]
  (->> style-map
       (map #(let [[k v] %] (str (name k) ": " v "; ")))
       (apply str)
       (s/trim)))

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
  (new js/THREE.CSS3DObject el))

(defn listen!
  ([el event cb]
   (.addEventListener el event cb false))
  ([event cb]
   (listen! js/window event cb)))

;;-----------------------------------------------------------------------------
;; Styles

(def styles
  {:game-board
   {:position "absolute"
    :margin "0"
    :padding "0"
    :top "0"
    :left "0"
    :right "0"
    :bottom "100px"
    :background-color "#d8d8d8"
    :border "2px solid sienna"
    :box-sizing "border-box"
    :-webkit-box-sizing "border-box"}
   :status
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

(defn win-height
  []
  (.-innerHeight js/window))

(defn win-width
  []
  (.-innerWidth js/window))

(defn win-top
  []
  (int (/ (win-height) 2)))

(defn  mk-scene
  []
  (new js/THREE.Scene))

(defn mk-camera
  [z]
  (let [c (js/THREE.PerspectiveCamera. 75 (/ (win-width)
                                             (- (win-height) 100)) 0.1 1000)]
    (set! (.. c -position -z) z)
    c))

(defn set-size!
  [object width height]
  (doto object
    (.setSize width height)))

(defn mk-renderer
  [width height]
  (set-size! (js/THREE.CSS3DRenderer.) width height))

(defn mk-brick
  [id w h c]
  (-> (mk-element "div" id)
      (set-html! (str "-[ " id " ]-"))
      (set-style! (merge (:brick styles)
                         {:width (str w "px")
                          :height (str h "px")
                          :background-color c}))
      (mk-3d)))


;;-----------------------------------------------------------------------------

(defn set-pos!
  [object {:keys [x y z]}]
  (set! (.. object -position -x) x)
  (set! (.. object -position -y) y)
  (set! (.. object -position -z) z))

(defn set-rot!
  [object {:keys [x y z]}]
  (set! (.. object -rotation -x) x)
  (set! (.. object -rotation -y) y)
  (set! (.. object -rotation -z) z))

(defn set-scale!
  [object {:keys [x y z]}]
  (set! (.. object -scale -x) x)
  (set! (.. object -scale -y) y)
  (set! (.. object -scale -z) z))

;;-----------------------------------------------------------------------------
;;-----------------------------------------------------------------------------
;; The idea is to separate the "state" of the game from the actual objects so
;; that we can update the state and the objects will adjust accordingly. Kinda
;; like react but without the diff optimization. (Actually, we could do that.)

(def states
  (atom {:a {:pos {:x 0 :y (* 32 5) :z 0} :speed 2}
         :b {:pos {:x 0 :y (* 32 6) :z 0} :speed -2.33}
         :c {:pos {:x 0 :y (* 32 7) :z 0} :speed 1.5}
         :d {:pos {:x 0 :y (* 32 8) :z 0} :speed -1.6}
         :e {:pos {:x 0 :y (* 32 9) :z 0} :speed 1.7}}))

(def shapes
  (atom {:a (mk-brick "a" 100 30 "#336699")
         :b (mk-brick "b" 100 30 "#990000")
         :c (mk-brick "c" 100 30 "#009900")
         :d (mk-brick "d" 100 30 "#000099")
         :e (mk-brick "e" 100 30 "#990099")}))

(defn make-game
  []
  {:scene (mk-scene)
   :camera (mk-camera 400)
   :renderer (mk-renderer (win-width) (win-height))
   :animating? (atom nil)})

(defn add-all
  ;;
  ;; Add object to the scene. The idea is that we could add
  ;; or remove them as needed because we keep a reference to
  ;; them outside the scene itself.
  ;;
  [{:keys [scene] :as game} shapes]
  (doseq [[id shape] shapes]
    (.add scene shape)))

(defn render-brick!
  [brick state]
  (when (:pos state)
    (set-pos! brick (:pos state))))

(defn render
  ;;
  ;; All this does is iterate through the shapes and pull
  ;; out a game-state for that shape and apply it.
  ;;
  [{:keys [renderer scene camera] :as game}]
  (doseq [[id shape] @shapes]
    (let [state (id @states)]
      (render-brick! shape state)))
  (.render renderer scene camera))

(defn resize!
  [evt {:keys [camera renderer] :as game}]
  (let [h (- (win-height) 100)
        w (win-width)]
    (set! (.. camera -aspect) (/ w h))
    (.updateProjectionMatrix camera)
    (.setSize renderer w h)
    (render game)))

(defn setup!
  [game]
  ;;
  ;; Hijack the dom.
  (set! js/document.body (.createElement js/document "body"))
  ;;
  ;; Game Board
  (let [wa (-> (mk-element "div" "workarea")
               (set-style! (:game-board styles)))
        st (-> (mk-element "footer" "status")
               (set-html! "debug")
               (set-style! (:status styles)))]
    (append-child! (.-domElement (:renderer game)) wa)
    (append-child! wa js/document.body)
    (append-child! st js/document.body))
  (listen! "resize" #(resize! % game)))

(defn start!
  [{:keys [animating?] :as game}]
  (when-not @animating?
    (reset! animating? :yep)
    ((fn f []
       (when-not (nil? @animating?)
         (js/requestAnimationFrame f))
       (render game)))))

(defn stop!
  [game]
  (reset! (:animating? game) nil))

;;-----------------------------------------------------------------------------
;; -----------------------------------------------------------------------------
;; This sim stuff just updates the "states" global defined above. It
;; has no idea that there are actual rendered objects floating around
;; the DOM. It does have to know about the window width, though.

(defn make-sim
  []
  (atom {:control (chan)
         :working? false}))

(defn slide!
  [oid]
  (let [brick (oid @shapes)
        pos (:pos (oid @states))
        speed (:speed (oid @states))
        max (+ 100 (int (/ (win-width) 2)))
        min (* -1 max)]
    (swap! states update-in [oid :pos :x]
           (fn [x]
             (cond
               (> (:x pos) max) min
               (< (:x pos) min) max
               :else (+ x speed))))))

(defn sim-work!
  [states]
  (doseq [oid (keys @states)]
    (slide! oid)))

(defn debug-state!
  [states]
  (set-html! (by-id "status")
             (str {:win-height (win-height) :win-width (win-width)
                   :radius (int (/ (win-width) 2))
                   :win-top (win-top)}
                  ;; "<br/>"
                  ;; (apply str (map (fn [[k v]] (str [k v] "<br/>")) @states))
                  )))

(def gap 16.66667)

(defn start-sim!
  [sim]
  (when-not (:working? @sim)
    (swap! sim assoc :working? true)
    (go (loop []
          (let [[val ch] (alts! [(:control @sim) (timeout gap)])]
            (when-not (= val :done)
              (sim-work! states)
              (debug-state! states)
              (recur)))))))

(defn stop-sim!
  [sim]
  (when (:working? @sim)
    (swap! sim assoc :working? false)
    (put! (:control @sim) :done)))

;;-----------------------------------------------------------------------------
;;-----------------------------------------------------------------------------

(defn main
  []
  (def g (make-game))
  (add-all g @shapes)
  (setup! g)
 (start! g)

 (def s (make-sim))
 (start-sim! s))

(set! (.-onload js/window) main)

(comment

  ;; Evaluate these to play with the sim.

  (def g (make-game))
  (add-all g @shapes)
  (setup! g)
  (start! g)

  (stop! g)

  (def s (make-sim))
  (start-sim! s)

  (stop-sim! s)


  ;; Paste these into an Emacs repl.
  ;; WARNING: Turn off pretty-printing.

  (require 'weasel.repl.websocket)

  (cemerick.piggieback/cljs-repl
   :repl-env (weasel.repl.websocket/repl-env
              :ip "0.0.0.0" :port 9001))
  ;;
  )
