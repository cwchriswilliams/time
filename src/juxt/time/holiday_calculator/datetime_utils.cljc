;; Copyright © 2022, JUXT LTD.

(ns juxt.time.holiday-calculator.datetime-utils
  (:require [tick.core :as t]
            [tick.alpha.interval :as t.i]
            #?(:cljs [goog.string :as gstring])))

(defn date->local-date-time
  [inst]
  (assert inst)
  (t/date-time (t/in inst "UTC")))

(defn date->local-date
  [inst]
  (assert inst)
  (t/date (t/in inst "UTC")))

(defn date->local-date-exclusive
  "Return the java.time.LocalDate of the given #inst, but if midnight, return the
  previous day."
  [inst]
  (if (t/midnight? (date->local-date-time inst))
    (t/<< (date->local-date inst) (t/new-period 1 :days))
    (date->local-date inst)))

(defn ->interval [{:keys [start end]}]
  #:tick{:beginning (:tick/beginning (t.i/bounds (t/date start)))
         :end (:tick/end (t.i/bounds (t/date end)))})

(defn days-as-map [value]
  {:value value
   :units "days"
   :display-with-units #?(:clj (format "%02.1f days" (float value))
                          :cljs (gstring/format "%02.1f days" (float value)))})

(defn duration-as-map
  "Represent the duration in working-days, based up the number of
  working hours in a week (full-time-hours).

  Assumes 5 working days per week."
  [duration full-time-hours]
  {:value duration
   ;; TODO: Should use locale as a dynamic var
   :days (let [value (bigdec (/ (.toHours duration) (/ full-time-hours 5)))]
           (days-as-map value))})
