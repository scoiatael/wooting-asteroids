(ns asteroids.game
  (:require [asteroids.device :as device]))

(def ^:private rotation-speed 0.001)
(def ^:private acceleration 0.0001)
(def ^:private shield-decay 0.0001)

(def player-radius 50)
(def snitch-radius 10)
(def snitch-vel-rot 0.1)


(def ^:private snitch-towards-speed 0.02)
(def ^:private snitch-away-speed 0.06)
(def ^:private snitch-attraction-radius 70)
(def ^:private snitch-ignore-radius 200)
(def ^:private snitch-repelling-radius 800)
(def debris-lifetime 1000)

(def ^:private initial-vx [-0.0001 0.0002 -0.0003])
(def ^:private initial-vy [0.0001 -0.0004 -0.0001])

(def ^:private camera-offset-x (- (/ 640 2) 50))
(def ^:private camera-offset-y (- (/ 640 2) 50))

(def ^:private wanted-asteroids 30)

(def ^:private visible-distance 800)
(def ^:private culling-distance 1800)

(def ^:priate variants [:big :small-1 :small-2 :small-3 :small-4])

(defn distance-between [x1 y1 x2 y2]
  (let [dx (- x2 x1)
        dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn distance-from [player asteroid]
  (distance-between (:cur-x asteroid) (:cur-y asteroid) (:cur-x player) (:cur-y player)))

(defn direction-from [player snitch]
  [ (- (:cur-x player)  (:cur-x snitch)) (- (:cur-y player) (:cur-y snitch))])

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
  (let [seed (.floor js/Math (* 7 cur-time))
        [x y] (random-point-on-circle seed player (mod (+ seed 91) 1200))]
    (assoc game :snitch {:cur-x x
                         :cur-y y
                         :rotation 0
                         :vel-x 0
                         :vel-y 0})))

(def ^:private starting-state (-> {:timer-running false
                                   :score 0
                                   :destroyed false
                                   :snitch nil
                                   :shield 0
                                   :shield-being-used false
                                   :player {:cur-x camera-offset-x
                                            :cur-y camera-offset-y
                                            :rotation 0
                                            :vel-x 0
                                            :vel-y 0}
                                   :camera {:ox 0
                                            :oy 0}
                                   :cur-time 0
                                   :debris []
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

(defn- update-shield [{:keys [time-delta shield score] :as game} keyboard]
  (let [used (get keyboard "Space")
        can-use (> shield 0)
        used (and used can-use)]
    (assoc game
           :shield-used used
           :score (if used (max 0 (- score (* shield-decay time-delta))) score)
           :shield (if used (max 0 (- shield (* shield-decay time-delta))) shield))))

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

(defn- update-debris-field [{:keys [debris time-delta] :as game}]
  (assoc-in game [:debris] (map  (fn [{:keys [vel-x vel-y vel-rot cur-x cur-y rotation lifetime] :as rock}]
                                      (let [new-x (+ cur-x (* time-delta vel-x))
                                            new-rotation (+ rotation (* time-delta vel-rot))
                                            new-y (+ cur-y (* time-delta vel-y))]
                                        (assoc rock
                                               :lifetime (- lifetime time-delta)
                                               :rotation new-rotation
                                               :cur-x new-x
                                               :cur-y new-y))) debris)))

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

(defn- has-collision [player asteroid size]
  (let [radius (/ size 2)]
    (when (> (- size 10 (/ radius 3)) (distance-between (- (:cur-x player) radius) (- (:cur-y player) radius) (:cur-x asteroid) (:cur-y asteroid)))
      asteroid)))

(defn- check-collisions [{:keys [debris player asteroids shield-used] :as game}]
  (if-let [{:keys [id] :as asteroid} (some #(has-collision player % (asteroid-size (:variant %))) asteroids)]
    (if shield-used
      (assoc game :asteroids (filter #(not (= id (:id %))) asteroids)
             :debris (conj debris (assoc asteroid :lifetime debris-lifetime)))
      (assoc game
             :timer-running false
             :destroyed true))
    game))

(def ^:private snitch-size 30)

(defn- check-snitch-collision [{:keys [player snitch score] :as game}]
  (let [distance (distance-from player snitch)]
    (if (> snitch-size distance)
      (-> game (assoc :score (inc score)
                      :snitch nil)
          (spawn-snitch))
      game)))

(defn- apply-snitch-behaviour [behaviour [distance dx dy] snitch]
  (let [rotation (.atan2 js/Math dx dy)
        ndx (/ dx distance)
        ndy (/ dy distance)]
    (condp = behaviour
      :run-towards [0 (* snitch-towards-speed ndx) (* snitch-towards-speed ndy)]
      :run-away [0 (* -1 ndx snitch-away-speed) (* -1 ndy snitch-away-speed)]
      [0 0 0])))

(defn- update-snitch [{:keys [snitch time-delta player] :as game}]
  (let [[dx dy] (direction-from player snitch)
        distance (distance-from player snitch)
        behaviour (condp > distance snitch-attraction-radius :run-towards snitch-ignore-radius :do-nothing snitch-repelling-radius :run-away :do-nothing)
        [_ vx vy] (apply-snitch-behaviour behaviour [distance dx dy] snitch)]
    (update-in game [:snitch]
               (fn [{:keys [cur-x cur-y vel-x vel-y rotation]}]
                 (assoc snitch
                        :behaviour behaviour
                        :cur-x (+ cur-x (* time-delta vel-x))
                        :cur-y (+ cur-y (* time-delta vel-y))
                        :rotation (+ rotation snitch-vel-rot)
                        :vel-x vx
                        :vel-y vy)))))

(defn- alive [player asteroid]
  (> (distance-between (:cur-x asteroid) (:cur-y asteroid) (:cur-x player) (:cur-y player)) culling-distance))

(defn- prune-asteroids [{:keys [player asteroids] :as game}]
  (assoc game :asteroids (filter #(not (alive player %)) asteroids)))

(defn- prune-debris [{:keys [debris] :as game}]
  (assoc game :debris (filter #(> 0 (:lifetime %)) debris)))

(defn- time-update [timestamp state]
  (-> state
      (assoc
       :cur-time timestamp
       :time-delta (- timestamp (:cur-time state)))
      (update-rotation @device/keyboard-state)
      (update-vel @device/keyboard-state)
      (update-shield @device/keyboard-state)
      (check-collisions)
      (update-ship-position)
      (update-asteroids)
      (update-debris-field)
      (update-camera)
      (prune-asteroids)
      (spawn-asteroids)
      (check-snitch-collision)
      (update-snitch)))

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


(add-watch state :shield-booster (fn [_ _ old {:keys [score] :as new}] (when (and  (< (:score old)
                                                                                      score)
                                                                                   (>= score 1))
                                                                         (reset! state (assoc new :shield 1)))))
