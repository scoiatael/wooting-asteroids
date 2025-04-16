(ns asteroids.render
  (:require
   [goog.string.format]
   [goog.string :as gstring]
   [asteroids.device :as device]
   [asteroids.game :as game]
   [sablono.core :as sab :include-macros true]))

(defonce ^:private root (.createRoot js/ReactDOM (.getElementById js/document "board-area")))

(defn px [n] (str n "px"))

(defn- display-magnitude [thrust]
        (condp > (.abs js/Math thrust) 0.1 "" 0.3 "small-thrust" 0.7 "big-thrust" "great-thrust"))

(defn- sgn [v a b] (if (> 0 v) a b))

(defn- thrust-classes [keyboard]
  (let [thrust (game/added-speed keyboard)]
   (str (display-magnitude thrust) " " (sgn thrust "forward-thrust" "reverse-thrust"))))

(defn- rotation-classes [keyboard]
  (let [rotation (game/added-rotation keyboard)]
   (str (display-magnitude rotation) " " (sgn rotation "right-thrust" "left-thrust"))))

(defn- render-player [{:keys [cur-x cur-y rotation]} keyboard]
  [:div.ship {:style {:top (px (- cur-y game/player-radius)) :left (px (- cur-x game/player-radius)) :rotate (gstring/format "%.3fdeg" (* 180 rotation))}}
   [:div.ship-effect {:class (thrust-classes keyboard)}]
   [:div.ship-effect {:class (rotation-classes keyboard)}]])

(defn- render-explosion [{:keys [cur-x cur-y]}]
    [:div.ship-explosion {:style {:top (px (- cur-y game/player-radius)) :left (px (- cur-x game/player-radius))}}])

(defn- render-player-shield [{:keys [cur-x cur-y]}]
    [:div.player-shield {:style {:top (px (- cur-y game/player-radius 5)) :left (px (- cur-x game/player-radius 5))}}])

(defn- render-snitch [{:keys [cur-x cur-y rotation]}]
  [:div.snitch {:style {:top (px (- cur-y game/snitch-radius)) :left (px (- cur-x game/snitch-radius)) :rotate (gstring/format "%.3frad" rotation)}}])

(defn- render-snitch-tracker [snitch {:keys [cur-x cur-y] :as player}]
  (let [[dx dy] (game/direction-from player snitch)
        rotation (* -1 (.atan2 js/Math dx dy))
        distance (game/distance-from player snitch)]
    (if (> distance 100)
      [:div.snitch-tracker {:style {:top (px (- cur-y game/player-radius)) :left (px (- cur-x game/player-radius)) :rotate (gstring/format "%.3frad" rotation)}}]
      [:div])))

(defn- render-debris [{:keys [id cur-x cur-y rotation variant lifetime] :as debris}]
  [:div.debris {:key id :id id :class (str "debris-" (name variant)) :style {:opacity (/ lifetime game/debris-lifetime ) :top (px cur-y) :left (px cur-x) :rotate (gstring/format "%.3fdeg" (* 180 rotation))}}])

(defn- render-asteroid [{:keys [id cur-x cur-y rotation variant] :as asteroid}]
  [:div.asteroid {:key id :id id :class (str "asteroid-" (name variant)) :style {:top (px cur-y) :left (px cur-x) :rotate (gstring/format "%.3fdeg" (* 180 rotation))}}])

(defn- translate-with-camera [{:keys [ox oy]} {:keys [cur-x cur-y] :as item}]
  (assoc item
         :cur-x (- cur-x ox)
         :cur-y (- cur-y oy)))

(defn- render-asteroids [camera asteroids]
    [:div.asteroid-field
     (map #(->> % (translate-with-camera camera) render-asteroid) asteroids)])

(defn- render-debris-field [camera debris]
    [:div.debris-field
     (map #(->> % (translate-with-camera camera) render-debris) debris)])

(defn- render-shield [{:keys [shield shield-used]}]
  [:div.shield
   {:style {:height (px (* 240 shield))} :class (str (if shield-used "shield-used" ""))}])

(defn- main-template [{:keys [debris destroyed score timer-running camera player asteroids snitch shield-used] :as game} keyboard]
  (sab/html [:div.board
             [:h1.score (gstring/format "%.1f" score)]
             (when timer-running
               [:div.debug-hud
                [:div
                 [:h4.debug "asteroids: " (count asteroids)]
                 [:h4.debug (gstring/format  "shield: %.3f " (:shield game))]
                 (when snitch [:h4.debug (gstring/format "distance: %.3f" (game/distance-from player snitch))])]
                [:div
                 [:h4.debug "keyboard"]
                 (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (or (keyboard key) 0))]) ["A" "W" "D" "S"])]
                [:div
                 [:h4.debug "camera"]
                 (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (key camera))]) [:ox :oy])]
                (when snitch
                  [:div
                   [:h4.debug "snitch"]
                   (if-let [behaviour (:behaviour snitch)]
                     [:h4.debug (str "behaviour: " (name behaviour)) ])
                   (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (key snitch))]) [:cur-x :cur-y :rotation])])
                [:div
                 [:h4.debug "player"]
                 (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (key player))]) [:cur-x :cur-y :vel-x :vel-y :rotation])]])
             (if-not timer-running
               (sab/html [:a.start-button {:onClick #(do (device/start) (game/start))} "START"])
               (sab/html [:span]))
             (when timer-running
               (render-player (translate-with-camera camera player) keyboard))
             (when shield-used
               (render-player-shield (translate-with-camera camera player)))
             (when destroyed
               (render-explosion (translate-with-camera camera player)))
             (when snitch
               (render-snitch (translate-with-camera camera snitch)))
             (when (and snitch timer-running)
               (render-snitch-tracker (translate-with-camera camera snitch) (translate-with-camera camera player)))
             (when timer-running
               (render-shield game))
             (render-asteroids camera asteroids)
             (render-debris-field camera debris)]))

(defn render [full-state keyboard]
  (.render root (main-template full-state keyboard)))
