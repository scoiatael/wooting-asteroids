(ns asteroids.render
  (:require
   [goog.string.format]
   [goog.string :as gstring]
   [asteroids.device :as device]
   [asteroids.game :as game]
   [sablono.core :as sab :include-macros true]))

(defonce ^:private root (.createRoot js/ReactDOM (.getElementById js/document "board-area")))

(defn px [n] (str n "px"))

(defn- render-player [{:keys [cur-x cur-y rotation]} keyboard]
  (let [thrust (game/added-speed keyboard)
        thrust-degree (condp > (.abs js/Math thrust) 0.1 "" 0.3 "small-thrust" 0.7 "big-thrust" "great-thrust")
        is-forward-thrust (> 0 thrust)
        extra-rotation (game/added-rotation keyboard)
        rotation-degree (condp > (.abs js/Math extra-rotation) 0.1 "" 0.3 "small-thrust" 0.7 "big-thrust" "great-thrust")
        is-left-rotation (< 0 extra-rotation)
        thrust-direction (if is-forward-thrust "forward-thrust" "reverse-thrust")
        rotation-direction (if is-left-rotation "left-thrust" "right-thrust")]
    [:div.ship {:style {:top (px cur-y) :left (px cur-x) :rotate (gstring/format "%.3fdeg" (* 180 rotation))}}
     [:div.ship-effect {:class (str rotation-degree " " rotation-direction)}]
     [:div.ship-effect {:class (str thrust-degree " " thrust-direction )}]]))

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
