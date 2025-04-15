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
   (str (display-magnitude rotation) " " (sgn rotation "left-thrust" "right-thrust"))))

(defn- render-player [{:keys [cur-x cur-y rotation]} keyboard]
    [:div.ship {:style {:top (px cur-y) :left (px cur-x) :rotate (gstring/format "%.3fdeg" (* 180 rotation))}}
     [:div.ship-effect {:class (thrust-classes keyboard)}]
     [:div.ship-effect {:class (rotation-classes keyboard)}]])

(defn- render-explosion [{:keys [cur-x cur-y]}]
    [:div.ship-explosion {:style {:top (px cur-y) :left (px cur-x)}}])

(defn- render-snitch [{:keys [cur-x cur-y rotation]}]
    [:div.snitch {:style {:top (px cur-y) :left (px cur-x)}}])

(defn- render-asteroid [{:keys [id cur-x cur-y rotation variant] :as asteroid}]
  [:div.asteroid {:key id :class (str "asteroid-" (name variant)) :style {:top (px cur-y) :left (px cur-x) :rotate (gstring/format "%.3fdeg" (* 180 rotation))}}])

(defn- translate-with-camera [{:keys [ox oy]} {:keys [cur-x cur-y] :as item}]
  (assoc item
         :cur-x (- cur-x ox)
         :cur-y (- cur-y oy)))

(defn- render-asteroids [camera asteroids]
    [:div.asteroid-field
     (map #(->> % (translate-with-camera camera) render-asteroid) asteroids)])

(defn- main-template [{:keys [destroyed score timer-running camera player asteroids snitch]} keyboard]
  (sab/html [:div.board
             [:h1.score score]
             (when timer-running
               [:div.debug-hud
                [:div
                 [:h4.debug "asteroids: " (count asteroids)]
                 (when snitch [:h4.debug "distance: " (game/distance-from player snitch)])]
                [:div
                 (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (or (keyboard key) 0))]) ["A" "W" "D" "S"])]
                [:div
                 (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (key camera))]) [:ox :oy])]
                (when snitch
                  [:div
                   (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (key snitch))]) [:cur-x :cur-y])])
                [:div
                 (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (key player))]) [:cur-x :cur-y :vel-x :vel-y :rotation])]])
             (if-not timer-running
               (sab/html [:a.start-button {:onClick #(do (device/start) (game/start))} "START"])
               (sab/html [:span]))
             (when timer-running
               (render-player (translate-with-camera camera player) keyboard))
             (when destroyed
               (render-explosion (translate-with-camera camera player)))
             (when snitch
               (render-snitch (translate-with-camera camera snitch)))
             (render-asteroids camera asteroids)]))

(defn render [full-state keyboard]
  (.render root (main-template full-state keyboard)))
