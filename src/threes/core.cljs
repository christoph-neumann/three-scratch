(ns threes.core
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<! timeout close! chan alts! put!]]
   [weasel.repl :as ws-repl]))

(enable-console-print!)

(try
  (ws-repl/connect "ws://localhost:9001")
  (catch js/Error e
    (println "REPL:" e)))

;;-----------------------------------------------------------------------------

(defn win-height
  []
  (.-innerHeight js/window))

(defn ^:public win-width
  []
  (.-innerWidth js/window))

(defn  mk-scene
  []
  (new js/THREE.Scene))

(defn mk-camera
  [z]
  (let [c (js/THREE.PerspectiveCamera. 75 (/ (win-width) (win-height)) 0.1 1000)]
    (set! (.. c -position -z) z)
    c))

(defn set-size!
  [thing width height]
  (doto thing
    (.setSize width height)))

(defn mk-renderer
  [width height]
  (set-size! (js/THREE.WebGLRenderer.) width height))

(defn mk-cube
  [x y z c]
  (let [geo (js/THREE.BoxGeometry. x y z)
        mat (js/THREE.MeshBasicMaterial. (clj->js {:color c
                                                   :wireframe true}))]
    (js/THREE.Mesh. geo mat)))

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
  (atom {:a {:pos {:x -1 :y 0 :z 0}
             :rot {:x 0 :y 0 :z 0}}
         :b {:pos {:x 0 :y 0 :z 0}
             :rot {:x 0 :y 0 :z 0}}}))

(def shapes
  (atom {:a (mk-cube 1 1 1 0x336699)
         :b (mk-cube 2 2 2 0x990000)}))

(defn make-game
  []
  {:scene (mk-scene)
   :camera (mk-camera 5)
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

(defn setup!
  [game]
  ;;
  ;; Hijack the dom.
  ;;
  (set! js/document.body (.createElement js/document "body"))
  (.appendChild js/document.body (.-domElement (:renderer game))))

(defn render
  ;;
  ;; All this does is iterate through the shapes and pull
  ;; out a game-state for that shape and apply it.
  ;;
  [{:keys [renderer scene camera] :as game}]
  (doseq [[id shape] @shapes]
    (let [state (id @states)]
      (set-pos! shape (:pos state))
      (set-rot! shape (:rot state))))
  (.render renderer scene camera))

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
;;-----------------------------------------------------------------------------
;; Simulate game-state coming in from the server.

(defn make-sim
  []
  (atom {:control (chan)
         :working? false}))

(defn bump-rot!
  [states oid op v]
  (swap! states (fn [s] (-> (update-in s [oid :rot :x] #(op % v))
                           (update-in [oid :rot :y] #(op % v))))))

(defn sim-work!
  [states]
  (doseq [oid (keys @states)]
    (case oid
      :a (bump-rot! states oid + .01)
      (bump-rot! states oid - .02))))

(def gap (int (/ 1000 60)))

(defn start-sim!
  [sim]
  (when-not (:working? @sim)
    (swap! sim assoc :working? true)
    (go (loop []
          (let [[val ch] (alts! [(:control @sim) (timeout gap)])]
            (when-not (= val :done)
              (sim-work! states)
              (recur)))))))

(defn stop-sim!
  [sim]
  (when (:working? @sim)
    (swap! sim assoc :working? false)
    (put! (:control @sim) :done)))

;;-----------------------------------------------------------------------------
;;-----------------------------------------------------------------------------

(comment

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

  (cemerick.piggieback/cljs-repla
   :repl-env (weasel.repl.websocket/repl-env
              :ip "0.0.0.0" :port 9001))
  ;;
  )
