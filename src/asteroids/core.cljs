(ns asteroids.core
  (:require
   [asteroids.device :as device]
   [asteroids.render :as render]
   [asteroids.game :as game]
   [cljsjs.react]
   [cljsjs.react.dom]
   [goog.string.format]
   [goog.string :as gstring]
   [sablono.core :as sab :include-macros true]))

(enable-console-print!)

;; (defn floor [x] (.floor js/Math x))

;; (defn translate [start-pos vel time]
;;   (floor (+ start-pos (* time vel))))


;; (defn reset-state [_ cur-time]
;;   (-> starting-state
;;       (assoc
;;           :start-time cur-time
;;           :asteroids-start-time cur-time
;;           :timer-running true)))

;; (defn update-pillars [{:keys [pillar-list cur-time] :as st}]
;;   (let [pillars-with-pos (map #(assoc % :cur-x (curr-pillar-pos cur-time %)) pillar-list)
;;         pillars-in-world (sort-by
;;                           :cur-x
;;                           (filter #(> (:cur-x %) (- pillar-width)) pillars-with-pos))]
;;     (assoc st
;;       :pillar-list
;;       (if (< (count pillars-in-world) 3)
;;         (conj pillars-in-world
;;               (new-pillar
;;                cur-time
;;                (+ pillar-spacing
;;                   (:cur-x (last pillars-in-world)))))
;;         pillars-in-world))))

;; (defn update-asteroids [{:keys [time-delta initial-vel asteroids-y jump-count] :as st}]
;;   (if (pos? jump-count)
;;     (let [cur-vel (- initial-vel (* time-delta gravity))
;;           new-y   (- asteroids-y cur-vel)
;;           new-y   (if (> new-y (- bottom-y asteroids-height))
;;                     (- bottom-y asteroids-height)
;;                     new-y)]
;;       (assoc st
;;               :cur-vel cur-vel
;;              :asteroids-y new-y))
;;     (sine-wave st)))

;; (defn time-update [timestamp state]
;;   (-> state
;;       (assoc
;;           :cur-time timestamp
;;           :time-delta (- timestamp (:asteroids-start-time state)))))

;; ;; derivatives

;; (defn border [{:keys [cur-time] :as state}]
;;   (-> state
;;       (assoc :border-pos (mod (translate 0 horiz-vel cur-time) 23))))

;; (defn pillar-offset [{:keys [gap-top] :as p}]
;;   (assoc p
;;     :upper-height gap-top
;;     :lower-height (- bottom-y gap-top pillar-gap)))

;; (defn pillar-offsets [state]
;;   (update-in state [:pillar-list]
;;              (fn [pillar-list]
;;                (map pillar-offset pillar-list))))

;; (defn world [state]
;;   (-> state
;;       border
;;       pillar-offsets))

;; (defn px [n] (str n "px"))

;; (defn pillar [{:keys [start-time cur-x pos-x upper-height lower-height]}]
;;   (sab/html
;;    [:div.pillars {:key (str pos-x start-time)}
;;     [:div.pillar.pillar-upper {:style {:left (px cur-x)
;;                                        :height upper-height}}]
;;     [:div.pillar.pillar-lower {:style {:left (px cur-x)
;;                                        :height lower-height}}]]))

;; (defn time-loop [time]
;;   (let [new-state (swap! game-state (partial time-update time))]
;;     (when (:timer-running new-state)
;;       (.requestAnimationFrame js/window time-loop))))

;; (defn start-with-device [device]
;;   (do
;;     (.log js/console "Listening for events of " (.getProductName device))
;;     (.startListening device (fn [active_keys]
;;                               (if-let [{:keys [scancode value]} (js->clj (first active_keys) :keywordize-keys true)]
;;                                 (let [key (.scancodeToString js/analogsense scancode)]
;;                                   (if (= key "Space")
;;                                     (swap! game-state (jump-analog value))))
;;                                 (swap! game-state #(assoc % :last-value nil))))))
;;   (.requestAnimationFrame
;;    js/window
;;    (fn [time]
;;      (reset! game-state (reset-state @game-state time))
;;      (time-loop time))))

;; (defn start-game []
;;   ;; https://github.com/AnalogSense/JavaScript-SDK/blob/senpai/demo.html#L40
;;   (.then (.getDevices js/analogsense)
;;          (fn [devices] (if-let [device (first devices)]
;;                          (start-with-device device)
;;                          (.then (.requestDevice js/analogsense) start-with-device)))))

;; (add-watch device/keyboard-state
;;            :debug
;;            (fn [key atom old-state new-state]
;;              (prn new-state)))

(add-watch game/state :renderer (fn [_ _ _ n]
                                  (render/render n @device/keyboard-state)))

(reset! game/state @game/state)
