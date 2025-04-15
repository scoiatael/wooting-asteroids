(ns asteroids.render
  (:require
   [goog.string.format]
   [goog.string :as gstring]
   [asteroids.device :as device]
   [asteroids.game :as game]
   [sablono.core :as sab :include-macros true]))

(defonce ^:private root (.createRoot js/ReactDOM (.getElementById js/document "board-area")))

(defn- main-template [{:keys [timer-running]} keyboard]
  (sab/html [:div.board
             (when timer-running
               [:div
                (map (fn [key] [:h4.debug {:key key} (gstring/format "%s: %.3f" key (or (keyboard key) 0))]) ["A" "W" "D" "S"])])
             (if-not timer-running
               (sab/html [:a.start-button {:onClick #(do (device/start) (game/start))} "START"])
               (sab/html [:span]))
             ;; [:div (map pillar pillar-list)]
             ;; [:div.asteroids {:style {:top (px asteroids-y) :rotate (gstring/format "%.3fdeg" (- 0 (* rotate-angle cur-vel)))}}]
             ;; [:div.scrolling-border {:style { :background-position-x (px border-pos)}}]
             ]))

(defn render [full-state keyboard]
  (.render root (main-template full-state keyboard)))
