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

(defn- main-template [{:keys [timer-running player]} keyboard]
  (sab/html [:div.board
             (when timer-running
               [:div
                (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (key player))]) [:cur-x :cur-y :vel-x :vel-y :rotation])
                (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (or (keyboard key) 0))]) ["A" "W" "D" "S"])])
             (if-not timer-running
               (sab/html [:a.start-button {:onClick #(do (device/start) (game/start))} "START"])
               (sab/html [:span]))
             (when timer-running
               (render-player player keyboard))
             ;; [:div (map pillar pillar-list)]
             ;; [:div.asteroids {:style {:top (px asteroids-y) :rotate (gstring/format "%.3fdeg" (- 0 (* rotate-angle cur-vel)))}}]
             ;; [:div.scrolling-border {:style { :background-position-x (px border-pos)}}]
             ]))

(defn render [full-state keyboard]
  (.render root (main-template full-state keyboard)))
