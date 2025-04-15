(ns asteroids.game
  (:require [asteroids.device :as device]))

(def ^:private rotation-speed 0.0000001)

(def ^:private starting-state {:timer-running false
                               :player {:cur-x (- (/ 480 2) 50)
                                        :cur-y (- (/ 640 2) 50)
                                        :rotation 0
                                        :vel-x 0
                                        :vel-y 0}})

(defonce state (atom starting-state))

(defn- fresh-state [_ cur-time]
  (-> starting-state
      (assoc
          :start-time cur-time
          :asteroids-start-time cur-time
          :timer-running true)))

(defn- key-or [device k o]
  (or (device k) o))

(defn- added-rotation [device] (- (key-or device "D" 0) (key-or device "A" 0)))

(defn- update-rotation [{:keys [time-delta] :as game} device]
  (update-in game [:player :rotation] (fn [rotation]
                                        (+ rotation (* rotation-speed time-delta (added-rotation device))))))

(defn- time-update [timestamp state]
  (-> state
      (assoc
       :cur-time timestamp
       :time-delta (- timestamp (:asteroids-start-time state)))
      (update-rotation @device/keyboard-state)))

(defn- time-loop [time]
  (let [new-state (swap! state (partial time-update time))]
    (when (:timer-running new-state)
      (.requestAnimationFrame js/window time-loop))))

(defn start []
  (.requestAnimationFrame
   js/window
   (fn [time]
     (reset! state (fresh-state @state time))
     (time-loop time))))
