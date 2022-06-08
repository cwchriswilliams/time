;; Copyright © 2022, JUXT LTD.

(ns juxt.time.holiday-calculator.datetime-utils-test
  (:require [juxt.time.holiday-calculator.datetime-utils :as sut]
            [tick.core :as t]
            [clojure.test :refer [deftest testing is] ]))

(deftest deration-as-map-test
  (testing "Given duration == full time hours / 5 returns 1 day"
    (is (= 1.0 (get-in (sut/duration-as-map (t/new-duration 8 :hours) 40) [:days :value])))
    (is (= 1.0 (get-in (sut/duration-as-map (t/new-duration 4 :hours) 20) [:days :value]))))

  (testing "Given duration == full time hours / 10 returns 0.5 day"
    (is (= 0.5 (get-in (sut/duration-as-map (t/new-duration 4 :hours) 40) [:days :value])))
    (is (= 0.5 (get-in (sut/duration-as-map (t/new-duration 2 :hours) 20) [:days :value])))))


(deftest date->local-date-time-test
  (testing "Given a datetime when BST is not in effect, returns without offset"
    (is (= (t/date-time "2021-01-05T00:00") (sut/date->local-date-time #inst "2021-01-05T00:00:00")))
    (is (= (t/date-time "2021-01-05T01:00") (sut/date->local-date-time #inst "2021-01-05T01:00:00")))
    (is (= (t/date-time "2021-01-05T23:00") (sut/date->local-date-time #inst "2021-01-05T23:00:00"))))
  (testing "Given a datetime when BST is in effect, still returns without offset"
    (is (= (t/date-time "2021-06-05T00:00") (sut/date->local-date-time #inst "2021-06-05T00:00:00")))
    (is (= (t/date-time "2021-06-05T01:00") (sut/date->local-date-time #inst "2021-06-05T01:00:00")))
    (is (= (t/date-time "2021-06-05T23:00") (sut/date->local-date-time #inst "2021-06-05T23:00:00")))))

(deftest date->local-date-test
  (testing "Given a datetime when BST is not in effect, returns without offset"
    (is (= (t/date "2021-01-05") (sut/date->local-date #inst "2021-01-05T00:00:00")))
    (is (= (t/date "2021-01-05") (sut/date->local-date #inst "2021-01-05T01:00:00")))
    (is (= (t/date "2021-01-05") (sut/date->local-date #inst "2021-01-05T23:00:00"))))
  (testing "Given a datetime when BST is in effect, still returns without offset"
    (is (= (t/date "2021-06-05") (sut/date->local-date #inst "2021-06-05T00:00:00")))
    (is (= (t/date "2021-06-05") (sut/date->local-date #inst "2021-06-05T01:00:00")))
    (is (= (t/date "2021-06-05") (sut/date->local-date #inst "2021-06-05T23:00:00")))))

(deftest date->local-date-exclusive-test
  (testing "If the date is midnight, return the previous day"
    (is (= (t/date "2021-06-04") (sut/date->local-date-exclusive #inst "2021-06-05T00:00:00"))))
  (testing "If the date is not midnight, return the current day"
    (is (= (t/date "2021-06-05") (sut/date->local-date-exclusive #inst "2021-06-05T00:01:00")))
    (is (= (t/date "2021-06-05") (sut/date->local-date-exclusive #inst "2021-06-05T06:30:30")))
    (is (= (t/date "2021-06-05") (sut/date->local-date-exclusive #inst "2021-06-05T23:59:59")))))
