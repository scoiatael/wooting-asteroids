(ns asteroids.game
  (:require [asteroids.device :as device]))

(def ^:private rotation-speed 0.001)
(def ^:private acceleration 0.0001)

(def ^:private initial-vx [-0.0001 0.0002 -0.0003])
(def ^:private initial-vy [0.0001 -0.0004 -0.0001])

(def ^:private camera-offset-x (- (/ 640 2) 50))
(def ^:private camera-offset-y (- (/ 640 2) 50))

(def ^:private wanted-asteroids 30)

(def ^:private visible-distance 800)
(def ^:private culling-distance 1800)

(def ^:priate variants [:big :small-1 :small-2 :small-3 :small-4])

(defn- distance-between [x1 y1 x2 y2]
  (let [dx (- x2 x1)
        dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn distance-from [player asteroid]
  (distance-between (:cur-x asteroid) (:cur-y asteroid) (:cur-x player) (:cur-y player)))

(defn- visible-from [player asteroid]
  (< (distance-from player asteroid) visible-distance))

(defn- random-point-on-circle [seed player radius]
  (let [angle (* (mod (/ (mod seed 9781) 100) 1) 2 Math/PI)
        x (+ (:cur-x player) (* radius (Math/cos angle)))
        y (+ (:cur-y player) (* radius (Math/sin angle)))]
    [x y]))

(defn- new-asteroid [min-distance cur-time {:keys [cur-x cur-y vel-x vel-y] :as player} idx]
  (let [seed (.floor js/Math (* 13 (+ cur-time idx)))
        seed3 (mod seed 3)
        seed4 (mod seed 4)
        seed5 (mod seed 5)
        [x y] (random-point-on-circle seed player (mod seed 1200))
        cur-x (+ x (- (mod (mod seed 127319) 128) 64))
        cur-y (+ y (- (mod (mod seed 981721) 128) 64))
        distance (distance-from player {:cur-x cur-x :cur-y cur-y})
        [cur-x cur-y] (if (< distance min-distance) (random-point-on-circle seed player min-distance) [cur-x cur-y])]
    {:cur-x cur-x
     :cur-y cur-y
     :rotation (/ seed3 4)
     :id (str seed)
     :distance distance
     :variant (get variants seed5)
     :vel-rot (* 0.00002 idx)
     :vel-x (+ (/ vel-x 20) (get initial-vx seed3))
     :vel-y (+ (/ vel-y 20) (get initial-vy seed3))}))

(defn- spawn-asteroids [{:keys [cur-time player asteroids] :as game}]
  (let [on-screen (count (filter #(visible-from player %) asteroids))
        target (- wanted-asteroids on-screen)
        new-asteroids (map-indexed #(new-asteroid (if (= 0 cur-time) 200 800) cur-time player %1) (repeat target {}))]
    (assoc game :asteroids (apply conj asteroids new-asteroids))))

(defn- spawn-snitch [{:keys [cur-time player asteroids] :as game}]
  (let [seed (.floor js/Math (* 13 cur-time))
        [x y] (random-point-on-circle seed player (mod seed 1200))]
    (assoc game :snitch {:cur-x x
                         :cur-y y
                         :rotation 0
                         :vel-x 0
                         :vel-y 0})))

(def ^:private starting-state (-> {:timer-running false
                                   :score 0
                                   :destroyed false
                                   :snitch nil
                                   :player {:cur-x camera-offset-x
                                            :cur-y camera-offset-y
                                            :rotation 0
                                            :vel-x 0
                                            :vel-y 0}
                                   :camera {:ox 0
                                            :oy 0}
                                   :cur-time 0
                                   :asteroids []}
                                  spawn-snitch
                                  spawn-asteroids))

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


(defn- update-camera [{:keys [time-delta camera player] :as game}]
  (let [{:keys [cur-x cur-y]} player
        {:keys [ox oy vel-x vel-y]} camera
        px (- cur-x camera-offset-x)
        py (- cur-y camera-offset-y)]
    (assoc game :camera {:ox px :oy py})))

(defn- asteroid-size [variant]
  (condp = variant
    :big 100
    50))

(defn- has-collision [object player size]
  (let [radius (/ size 2)]
    (> (- size 10) (distance-between (- (:cur-x object) radius) (- (:cur-y object) radius) (- (:cur-x player) 50) (- (:cur-y player) 50)))))

(defn- check-collisions [{:keys [player asteroids] :as game}]
  (if (some #(has-collision player % (asteroid-size (:variant %))) asteroids)
    (assoc game
           :timer-running false
           :destroyed true)
    game))

(def ^:private snitch-size 20)

(defn- check-snitch-collision [{:keys [player snitch score] :as game}]
  (if (has-collision player snitch snitch-size)
    (assoc game :score
           (inc score)
           :snitch nil)
    game))

(defn- alive [player asteroid]
  (> (distance-between (:cur-x asteroid) (:cur-y asteroid) (:cur-x player) (:cur-y player)) culling-distance))

(defn- prune-asteroids [{:keys [player asteroids] :as game}]
  (assoc game :asteroids (filter #(not (alive player %)) asteroids)))

(defn- time-update [timestamp state]
  (-> state
      (assoc
       :cur-time timestamp
       :time-delta (- timestamp (:cur-time state)))
      (update-rotation @device/keyboard-state)
      (update-vel @device/keyboard-state)
      (check-collisions)
      (update-ship-position)
      (update-asteroids)
      (update-camera)
      (prune-asteroids)
      (spawn-asteroids)
      (check-snitch-collision)))

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
