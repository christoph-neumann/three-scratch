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
  (set-size! (new js/THREE.CSS3DRenderer) width height))

(defn mk-box
  [width height color]
  (let [el (.createElement js/document "div")]
    (set! (.-innerHTML el) "Testing")
    (set! (.. el -style -cssText) (str "font-size: 34px; border: 1px solid black; width: " width "px; height: " height "px; background-color: " color ";"))
    (new js/THREE.CSS3DObject el)))


;;-----------------------------------------------------------------------------

(defn make-scene
  []
  (let [box (mk-box 300 100 "#336699")
        scene {:scene (mk-scene)
               :camera (mk-camera)
               :renderer (mk-renderer (win-width) (win-height))
               :animating? (atom nil)
               :box box
               :xform (fn []
                        (set! (.. box -rotation -x) (+ (.. box -rotation -x) 0.04))
                        (set! (.. box -rotation -y) (+ (.. box -rotation -y) 0.07)))}]
    (doto (:scene scene)
      (.add box))
    (set! (.. (:camera scene) -position -z) 400)
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
