(ns asteroids.game
  (:require [asteroids.device :as device]))

(def ^:private rotation-speed 0.001)
(def ^:private acceleration 0.0001)

(def ^:private starting-state {:timer-running false
                               :player {:cur-x (- (/ 640 2) 50)
                                        :cur-y (- (/ 640 2) 50)
                                        :rotation 0
                                        :vel-x 0
                                        :vel-y 0}})

(defonce state (atom starting-state))

(defn- fresh-state [_ cur-time]
  (-> starting-state
      (assoc
          :cur-time cur-time
          :timer-running true)))

(defn- key-or [device k o]
  (or (device k) o))

(defn added-rotation [device] (- (key-or device "D" 0) (key-or device "A" 0)))

(defn added-speed [device] (- (key-or device "S" 0) (key-or device "W" 0)))

(defn- update-rotation [{:keys [time-delta] :as game} device]
  (update-in game [:player :rotation] (fn [rotation]
                                        (+ rotation (* rotation-speed time-delta (added-rotation device))))))

(defn- update-vel [{:keys [time-delta] :as game} device]
  (update-in game [:player] (fn [{:keys [vel-x vel-y rotation] :as player}]
                              (let [rotation-in-rad (* rotation (.-PI js/Math))
                                    x-part (.sin js/Math rotation-in-rad)
                                    y-part (.cos js/Math rotation-in-rad)
                                    total-speed (* acceleration time-delta (added-speed device))
                                    new-x (- vel-x (* x-part total-speed))
                                    new-y (+ vel-y (* y-part total-speed))]
                                (assoc player
                                       :vel-x new-x
                                       :vel-y new-y)))))

(defn update-ship-position [{:keys [time-delta] :as game}]
  (update-in game [:player] (fn [{:keys [vel-x vel-y cur-x cur-y] :as player}]
                                  (let [new-x (+ cur-x (* time-delta vel-x))
                                        new-x (min new-x (- 640 50))
                                        new-x (max new-x (- 0 50))
                                        new-y (+ cur-y (* time-delta vel-y))
                                        new-y (min new-y (- 640 50))
                                        new-y (max new-y (- 0 50))]
                                    (assoc player
                                           :cur-x new-x
                                           :cur-y new-y)))))

(defn- time-update [timestamp state]
  (-> state
      (assoc
       :cur-time timestamp
       :time-delta (- timestamp (:cur-time state)))
      (update-rotation @device/keyboard-state)
      (update-vel @device/keyboard-state)
      (update-ship-position)))

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
