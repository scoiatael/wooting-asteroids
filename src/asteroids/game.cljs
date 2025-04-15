(ns asteroids.game
  (:require [asteroids.device :as device]))

(def ^:private rotation-speed 0.001)
(def ^:private acceleration 0.0001)

(def ^:private initial-xs [128 467 600])
(def ^:private initial-ys [53 128 400])

(def ^:private initial-vx [-0.0001 0.0002 -0.0003])
(def ^:private initial-vy [0.0001 -0.0004 -0.0001])

(def ^:private camera-offset-x (- (/ 640 2) 50))
(def ^:private camera-offset-y (- (/ 640 2) 50))

(def ^:private starting-state {:timer-running false
                               :player {:cur-x camera-offset-x
                                        :cur-y camera-offset-y
                                        :rotation 0
                                        :vel-x 0
                                        :vel-y 0}
                               :camera {:ox 0
                                        :oy 0}
                               :asteroids (map-indexed  (fn [idx variant]
                                                          {:cur-x (get initial-xs (mod idx 3))
                                                           :cur-y (get initial-ys (mod idx 3))
                                                           :rotation (/ idx 4)
                                                           :id (str idx)
                                                           :variant variant
                                                           :vel-rot (* 0.00002 idx)
                                                           :vel-x (get initial-vx (mod idx 3))
                                                           :vel-y (get initial-vy (mod idx 3))})
                                                        [:big :small-1 :small-2 :small-3 :small-4])})

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

(defn- update-ship-position [{:keys [time-delta] :as game}]
  (update-in game [:player] (fn [{:keys [vel-x vel-y cur-x cur-y] :as player}]
                                  (let [new-x (+ cur-x (* time-delta vel-x))
                                        new-y (+ cur-y (* time-delta vel-y))]
                                    (assoc player
                                           :cur-x new-x
                                           :cur-y new-y)))))

(defn- update-asteroids [{:keys [asteroids time-delta] :as game}]
  (assoc-in game [:asteroids] (map  (fn [{:keys [vel-x vel-y vel-rot cur-x cur-y rotation] :as asteroid}]
                                      (let [new-x (+ cur-x (* time-delta vel-x))
                                            new-rotation (+ rotation (* time-delta vel-rot))
                                            new-y (+ cur-y (* time-delta vel-y))]
                                        (assoc asteroid
                                               :rotation new-rotation
                                               :cur-x new-x
                                               :cur-y new-y))) asteroids)))


(defn- distance-between [x1 y1 x2 y2]
  (let [dx (- x2 x1)
        dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn- update-camera [{:keys [time-delta camera player] :as game}]
  (let [{:keys [cur-x cur-y]} player
        {:keys [ox oy vel-x vel-y]} camera
        px (- cur-x camera-offset-x)
        py (- cur-y camera-offset-y)]
    (assoc game :camera {:ox px :oy py})))

(defn- time-update [timestamp state]
  (-> state
      (assoc
       :cur-time timestamp
       :time-delta (- timestamp (:cur-time state)))
      (update-rotation @device/keyboard-state)
      (update-vel @device/keyboard-state)
      (update-ship-position)
      (update-asteroids)
      (update-camera)))

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
