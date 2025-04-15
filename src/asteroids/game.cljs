(ns asteroids.game)


(def ^:private starting-state {:timer-running false})

(defonce state (atom starting-state))

(defn- fresh-state [_ cur-time]
  (-> starting-state
      (assoc
          :start-time cur-time
          :asteroids-start-time cur-time
          :timer-running true)))

(defn- time-update [timestamp state]
  (-> state
      (assoc
          :cur-time timestamp
          :time-delta (- timestamp (:asteroids-start-time state)))))

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
