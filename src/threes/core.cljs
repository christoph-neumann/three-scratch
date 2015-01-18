(ns threes.core
  (:require
   [threes.dom :as dom]
   [threes.sim :as sim]
   [threes.fx :as fx]
   [weasel.repl :as ws-repl]))

;;-----------------------------------------------------------------------------

(enable-console-print!)
(ws-repl/connect "ws://localhost:9001")

;;-----------------------------------------------------------------------------

(def states
  (atom {:a {:pos {:x 0 :y (* 32 1) :z 100}  :speed 2}
         :b {:pos {:x 0 :y (* 32 2) :z 0}    :speed -2.33}
         :c {:pos {:x 0 :y (* 32 3) :z -100} :speed 1.5}
         :d {:pos {:x 0 :y (* 32 4) :z -200} :speed -1.6}
         :e {:pos {:x 0 :y (* 32 4) :z 0}    :speed 1.7}}))

(def shapes
  (atom {:a (fx/mk-brick "a" 100 30 "#336699")
         :b (fx/mk-brick "b" 100 30 "#990000")
         :c (fx/mk-brick "c" 100 30 "#009900")
         :d (fx/mk-brick "d" 100 30 "#000099")
         :e (fx/mk-brick "e" 100 30 "#990099")}))

(defn make-game
  []
  (let [[w h] (dom/aspect)]
   {:scene (fx/mk-scene)
    :camera (fx/mk-camera 400)
    :renderer (fx/mk-renderer w h)
    :animating? (atom nil)}))

(defn add-all
  [{:keys [scene] :as game} shapes]
  (doseq [[id shape] shapes]
    (.add scene shape)))

(defn render-brick!
  [brick state]
  (when (:pos state)
    (fx/set-pos! brick (:pos state))))

(defn render
  [{:keys [renderer scene camera] :as game}]
  (doseq [[id shape] @shapes]
    (let [state (id @states)]
      (render-brick! shape state)))
  (.render renderer scene camera))

(defn resize!
  [evt {:keys [camera renderer] :as game}]
  (let [[w h] (dom/aspect)]
    (set! (.. camera -aspect) (/ w h))
    (.updateProjectionMatrix camera)
    (.setSize renderer w h)
    (render game)))

(defn setup!
  [{:keys [camera] :as game}]
  (set! js/document.body (.createElement js/document "body"))
  (dom/append-child! (.-domElement (:renderer game)) js/document.body)
  (-> (dom/mk-element "footer" "status")
      (dom/set-html! "debug")
      (dom/set-style! (:status fx/styles))
      (dom/append-child! js/document.body))
  (dom/listen! "resize" #(resize! % game))
  (render game))

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

(defn main
  []
  (def g (make-game))
  (add-all g @shapes)
  (setup! g)
  (start! g)

  (def s (sim/make-sim states))
  (sim/start-sim! s))

(set! (.-onload js/window) main)

;;-----------------------------------------------------------------------------

(comment

  ;; Evaluate these to play with the sim.

  (def g (make-game))
  (add-all g @shapes)
  (setup! g)
  (start! g)

  (stop! g)

  (def s (sim/make-sim states))
  (sim/start-sim! s)

  (sim/stop-sim! s)


  ;; Paste these into an Emacs repl.
  ;; WARNING: Turn off pretty-printing.

  (require 'weasel.repl.websocket)

  (cemerick.piggieback/cljs-repl
   :repl-env (weasel.repl.websocket/repl-env
              :ip "0.0.0.0" :port 9001))
  ;;
  )
