(ns asteroids.device)

(defonce keyboard-state (atom {}))

(defn initialize []
  ;; https://github.com/AnalogSense/JavaScript-SDK/blob/senpai/demo.html#L40
  (js/Promise. (fn [res]
                (.then (.getDevices js/analogsense)
                       (fn [devices] (if-let [device (first devices)]
                                       (res device)
                                       (.then (.requestDevice js/analogsense) res)))))))

(defn handle-device [device]
  (.log js/console "Listening for events of " (.getProductName device))
  (.startListening device (fn [active_keys]
                            (as-> active_keys v
                              (js->clj v :keywordize-keys true)
                              (map (fn [{:keys [scancode value]}] [(.scancodeToString js/analogsense scancode) value]) v)
                              (into {} v)
                              (if (.hasFocus js/document)
                                (reset! keyboard-state v))))))

(defn start []
  (.then (initialize) handle-device))
