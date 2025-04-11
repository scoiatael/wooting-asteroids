(ns flappy-bird-demo.core
  (:require
   [cljsjs.react]
   [cljsjs.react.dom]
   [goog.string.format]
   [goog.string :as gstring]
   [sablono.core :as sab :include-macros true]))

(defn floor [x] (.floor js/Math x))

(defn translate [start-pos vel time]
  (floor (+ start-pos (* time vel))))

(def horiz-vel -0.15)
(def gravity 0.04)
(def jump-vel 11)
(def start-y 312)
(def bottom-y 561)
(def flappy-x 212)
(def flappy-width 57)
(def flappy-roundness 10)
(def flappy-height 41)
(def pillar-spacing 324)
(def pillar-gap 158) ;; 158
(def pillar-width 86)
(def max-analog-width 300)
(def analog-jump-vel 2)
(def rotate-angle 2)

(defonce ^:private root (.createRoot js/ReactDOM (.getElementById js/document "board-area")))

(def starting-state {:timer-running false
                     :jump-count 0
                     :initial-vel 0
                     :cur-vel 0
                     :start-time 0
                     :flappy-start-time 0
                     :flappy-y   start-y
                     :pillar-list [{ :start-time 0
                                    :pos-x 900
                                    :cur-x 900
                                    :gap-top 200 }]})

(defn reset-state [_ cur-time]
  (-> starting-state
      (update :pillar-list (partial map #(assoc % :start-time cur-time)))
      (assoc
          :start-time cur-time
          :flappy-start-time cur-time
          :timer-running true)))

(defonce flap-state (atom starting-state))

(defn curr-pillar-pos [cur-time {:keys [pos-x start-time]}]
  (translate pos-x horiz-vel (- cur-time start-time)))

(defn in-pillar? [{:keys [cur-x]}]
  (and (>= (+ flappy-x flappy-width) (+ cur-x flappy-roundness))
       (< (+ flappy-x flappy-roundness) (+ cur-x pillar-width))))

(defn in-pillar-gap? [{:keys [flappy-y]} {:keys [gap-top]}]
  (and (< gap-top (+ flappy-y flappy-roundness))
       (> (+ gap-top pillar-gap)
          (+ flappy-y flappy-height))))

(defn bottom-collision? [{:keys [flappy-y]}]
  (>= flappy-y (- bottom-y flappy-height)))

(defn collision? [{:keys [pillar-list] :as st}]
  (if (some #(or (and (in-pillar? %)
                      (not (in-pillar-gap? st %)))
                 (bottom-collision? st)) pillar-list)
    (assoc st :timer-running false)
    st))

(defn new-pillar [cur-time pos-x]
  {:start-time cur-time
   :pos-x      pos-x
   :cur-x      pos-x
   :gap-top    (+ 60 (rand-int (- bottom-y 120 pillar-gap)))})

(defn update-pillars [{:keys [pillar-list cur-time] :as st}]
  (let [pillars-with-pos (map #(assoc % :cur-x (curr-pillar-pos cur-time %)) pillar-list)
        pillars-in-world (sort-by
                          :cur-x
                          (filter #(> (:cur-x %) (- pillar-width)) pillars-with-pos))]
    (assoc st
      :pillar-list
      (if (< (count pillars-in-world) 3)
        (conj pillars-in-world
              (new-pillar
               cur-time
               (+ pillar-spacing
                  (:cur-x (last pillars-in-world)))))
        pillars-in-world))))

(defn sine-wave [st]
  (assoc st
    :flappy-y
    (+ start-y (* 30 (.sin js/Math (/ (:time-delta st) 300))))))

(defn update-flappy [{:keys [time-delta initial-vel flappy-y jump-count] :as st}]
  (if (pos? jump-count)
    (let [cur-vel (- initial-vel (* time-delta gravity))
          new-y   (- flappy-y cur-vel)
          new-y   (if (> new-y (- bottom-y flappy-height))
                    (- bottom-y flappy-height)
                    new-y)]
      (assoc st
              :cur-vel cur-vel
             :flappy-y new-y))
    (sine-wave st)))

(defn score [{:keys [cur-time start-time] :as st}]
  (let [score (- (.abs js/Math (floor (/ (- (* (- cur-time start-time) horiz-vel) 544)
                               pillar-spacing)))
                 4)]
  (assoc st :score (if (neg? score) 0 score))))

(defn time-update [timestamp state]
  (-> state
      (assoc
          :cur-time timestamp
          :time-delta (- timestamp (:flappy-start-time state)))
      update-flappy
      update-pillars
      collision?
      score))

(defn jump [{:keys [cur-time jump-count] :as state}]
  (-> state
      (assoc
          :jump-count (inc jump-count)
          :flappy-start-time cur-time
          :initial-vel jump-vel)))

(defn scale-analog-value [value]
  ;;  value is between 0 and 1, ~linear
  ;;  we wanna make it less linear
  (->> value
       (.tan js/Math)
       (* analog-jump-vel)))

(defn jump-analog [value] (fn  [{:keys [cur-time cur-vel jump-count] :as state}]
                            (-> state
                                (assoc
                                 :jump-count (inc jump-count)
                                 :flappy-start-time cur-time
                                 :last-value value
                                 :initial-vel (+  (scale-analog-value value) (max 0 cur-vel))))))

;; derivatives

(defn border [{:keys [cur-time] :as state}]
  (-> state
      (assoc :border-pos (mod (translate 0 horiz-vel cur-time) 23))))

(defn pillar-offset [{:keys [gap-top] :as p}]
  (assoc p
    :upper-height gap-top
    :lower-height (- bottom-y gap-top pillar-gap)))

(defn pillar-offsets [state]
  (update-in state [:pillar-list]
             (fn [pillar-list]
               (map pillar-offset pillar-list))))

(defn world [state]
  (-> state
      border
      pillar-offsets))

(defn px [n] (str n "px"))

(defn pillar [{:keys [start-time cur-x pos-x upper-height lower-height]}]
  (sab/html
   [:div.pillars {:key (str pos-x start-time)}
    [:div.pillar.pillar-upper {:style {:left (px cur-x)
                                       :height upper-height}}]
    [:div.pillar.pillar-lower {:style {:left (px cur-x)
                                       :height lower-height}}]]))

(defn time-loop [time]
  (let [new-state (swap! flap-state (partial time-update time))]
    (when (:timer-running new-state)
      (.requestAnimationFrame js/window time-loop))))

(defn start-with-device [device]
  (do
    (.log js/console "Listening for events of " (.getProductName device))
    (.startListening device (fn [active_keys]
                              (if-let [{:keys [scancode value]} (js->clj (first active_keys) :keywordize-keys true)]
                                (let [key (.scancodeToString js/analogsense scancode)]
                                  (if (= key "Space")
                                    (swap! flap-state (jump-analog value))))
                                (swap! flap-state #(assoc % :last-value nil))))))
  (.requestAnimationFrame
   js/window
   (fn [time]
     (reset! flap-state (reset-state @flap-state time))
     (time-loop time))))

(defn start-game []
  ;; https://github.com/AnalogSense/JavaScript-SDK/blob/senpai/demo.html#L40
  (.then (.getDevices js/analogsense)
         (fn [devices] (if-let [device (first devices)]
                         (start-with-device device)
                         (.then (.requestDevice js/analogsense) start-with-device)))))

(defn main-template [{:keys [score last-value cur-time jump-count
                             timer-running border-pos initial-vel cur-vel
                             flappy-y pillar-list]}]
  (sab/html [:div.board { :onMouseDown (fn [e]
                                         (swap! flap-state jump)
                                         (.preventDefault e))}
             [:h1.score score]
             [:h4.debug (gstring/format "%.3f" initial-vel)]
             [:h4.debug (gstring/format "%.3f" cur-vel)]
             (if (and timer-running
                      last-value)
               [:div.analog
                [:div.analog-value {:style {:width (* last-value max-analog-width)}}]
                [:h2.analog-text (gstring/format "%.3f" last-value)]])
             (if-not timer-running
               (sab/html [:a.start-button {:onClick #(start-game)}
                (if (< 1 jump-count) "RESTART" "START")])
               (sab/html [:span]))
             [:div (map pillar pillar-list)]
             [:div.flappy {:style {:top (px flappy-y) :rotate (gstring/format "%.3fdeg" (- 0 (* rotate-angle cur-vel)))}}]
             [:div.scrolling-border {:style { :background-position-x (px border-pos)}}]]))

(defn renderer [full-state]
  (.render root (main-template full-state)))

(add-watch flap-state :renderer (fn [_ _ _ n]
                                  (renderer (world n))))

(reset! flap-state @flap-state)
