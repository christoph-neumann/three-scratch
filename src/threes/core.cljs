(ns threes.core
  (:require [weasel.repl :as ws-repl]))

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
  []
  (js/THREE.PerspectiveCamera. 75 (/ (win-width) (win-height)) 0.1 1000))

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

(defn make-scene
  []
  (let [cube (mk-cube 1 1 1 0x336699)
        scene {:scene (mk-scene)
               :camera (mk-camera)
               :renderer (mk-renderer (win-width) (win-height))
               :animating? (atom nil)
               :cube cube
               :xform (fn []
                        (set! (.. cube -rotation -x) (+ (.. cube -rotation -x) 0.1))
                        (set! (.. cube -rotation -y) (+ (.. cube -rotation -y) 0.1)))}]
    (doto (:scene scene)
      (.add cube))
    (set! (.. (:camera scene) -position -z) 5)
    scene))

(defn animate!
  [{:keys [animating? xform renderer scene camera] :as obj}]
  ((fn f []
     (when-not (nil? @animating?)
       (js/requestAnimationFrame f))
     (xform)
     (.render renderer scene camera))))

(defn stop-scene!
  [scene]
  (reset! (:animating? scene) nil))

(defn start-scene!
  [scene]
  (reset! (:animating? scene) :yep)
  (set! js/document.body (.createElement js/document "body"))
  (.appendChild js/document.body (.-domElement (:renderer scene)))
  (animate! scene))

(println "Hello world!")
(def testy (make-scene))
(start-scene! testy)


(comment
  ;; Paste these into an Emacs repl.

  ;; WARNING: Turn off pretty-printing.

  (require 'weasel.repl.websocket)

  (cemerick.piggieback/cljs-repl
   :repl-env (weasel.repl.websocket/repl-env
              :ip "0.0.0.0" :port 9001))
  ;;
  )
