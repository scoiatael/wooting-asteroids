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

(defn- render-asteroid [{:keys [id cur-x cur-y rotation variant] :as asteroid}]
  [:div.asteroid {:key id :class (str "asteroid-" (name variant)) :style {:top (px cur-y) :left (px cur-x) :rotate (gstring/format "%.3fdeg" (* 180 rotation))}}])

(defn- translate-with-camera [{:keys [ox oy]} {:keys [cur-x cur-y] :as item}]
  (assoc item
         :cur-x (- cur-x ox)
         :cur-y (- cur-y oy)))

(defn- render-asteroids [camera asteroids]
    [:div.asteroid-field
     (map #(->> % (translate-with-camera camera) render-asteroid) asteroids)])

(defn- main-template [{:keys [timer-running camera player asteroids]} keyboard]
  (sab/html [:div.board
             (when timer-running
               [:div.debug-hud
                [:div
                 (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (or (keyboard key) 0))]) ["A" "W" "D" "S"])]
                [:div
                 (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (key camera))]) [:ox :oy])]
                [:div
                 (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (key player))]) [:cur-x :cur-y :vel-x :vel-y :rotation])]])
             (if-not timer-running
               (sab/html [:a.start-button {:onClick #(do (device/start) (game/start))} "START"])
               (sab/html [:span]))
             (when timer-running
               (render-player (translate-with-camera camera player) keyboard))
             (render-asteroids camera asteroids)]))

(defn render [full-state keyboard]
  (.render root (main-template full-state keyboard)))
