(ns threes.sim
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<! timeout close! chan alts! put!]]
   [threes.dom :as dom]))

(def gap 16.66667)

(defn- neg
  [v]
  (* -1 v))

(defn- screen-h
  [width]
  (/ (* width 3) 4))

;;-----------------------------------------------------------------------------

(defn make-sim
  [states]
  (atom {:control (chan)
         :states states
         :working? false}))

(defn slide!
  [states oid width radius]
  (let [pos   (:pos (oid @states))
        speed (:speed (oid @states))]
    (swap! states update-in [oid :pos :x]
           (fn [x]
             (cond
               (> (int (:x pos)) radius) (neg radius)
               (< (int (:x pos)) (neg radius)) radius
               :else (+ x speed))))))

(defn slide-reverse!
  [states oid width radius]
  (let [pos   (:pos (oid @states))
        speed (:speed (oid @states))]
    (swap! states update-in [oid]
           (fn [{:keys [pos speed] :as state}]
             (cond-> state
               (> (:x pos) radius)       (assoc :speed (neg speed)
                                                :pos (assoc pos :x (- radius speed)))
               (< (:x pos) (neg radius)) (assoc :speed (neg speed)
                                                :pos (assoc pos :x (+ (neg radius) (neg speed))))
               true                      (update-in [:pos :x] #(+ % speed)))))))

(defn sim-work!
  [states]
  (let [w (dom/win-width)
        radius (/ w 2)]
    (doseq [oid (keys @states)]
      (slide-reverse! states oid w radius))))

(defn debug-state!
  [states]
  (dom/set-html! (dom/by-id "status")
                 (str {:dim (str (dom/win-width) "x" (screen-h (dom/win-width)))
                       :rat (/ (dom/win-width) (screen-h (dom/win-width)))
                       :radius (/ (dom/win-width) 2)}
                      ;; "<br/>"
                      ;; (apply str (map (fn [[k v]]
                      ;;                   (str [k (:x (:pos v))] "<br/>"))
                      ;;                 @states))
                      "<br/>"
                      (apply str (map (fn [[k v]] (str [k v] "<br/>")) @states))
                      )))


(defn start-sim!
  [sim]
  (when-not (:working? @sim)
    (swap! sim assoc :working? true)
    (go (loop []
          (let [[val ch] (alts! [(:control @sim) (timeout gap)])]
            (when-not (= val :done)
              (sim-work! (:states @sim))
              #_(debug-state! (:states @sim))
              (recur)))))))

(defn stop-sim!
  [sim]
  (when (:working? @sim)
    (swap! sim assoc :working? false)
    (put! (:control @sim) :done)))
