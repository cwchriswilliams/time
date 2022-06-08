;; Copyright © 2022, JUXT LTD.

(ns juxt.time.holiday-calculator.calculations-test
  (:require  [clojure.test :refer [deftest testing is]]
             [tick.core :as t]
             [juxt.time.holiday-calculator.calculations :as sut]
             [tick.alpha.interval :as t.i]))

(def full-time-work-pattern ; 40 hrs
  {"MONDAY"
   {:juxt.home/beginning-local-time "09:00"
    :juxt.home/end-local-time "17:00"}
   "TUESDAY"
   {:juxt.home/beginning-local-time "09:00"
    :juxt.home/end-local-time "17:00"}
   "WEDNESDAY"
   {:juxt.home/beginning-local-time "09:00"
    :juxt.home/end-local-time "17:00"}
   "THURSDAY"
   {:juxt.home/beginning-local-time "09:00"
    :juxt.home/end-local-time "17:00"}
   "FRIDAY"
   {:juxt.home/beginning-local-time "09:00"
    :juxt.home/end-local-time "17:00"}})

(def part-time-work-pattern ; 20 hrs
  {"MONDAY"
   {:juxt.home/beginning-local-time "09:00"
    :juxt.home/end-local-time "17:00"}
   "TUESDAY"
   {:juxt.home/beginning-local-time "09:00"
    :juxt.home/end-local-time "17:00"}
   "WEDNESDAY"
   {:juxt.home/beginning-local-time "09:00"
    :juxt.home/end-local-time "13:00"}})

(def basic-full-time-staff-record [{:juxt.home/effective-from #inst "2019-04-01T00:00"
                                    :juxt.home/employment-type "EMPLOYEE"
                                    :juxt.home/status "ACTIVE"}])

(def terminated-full-time-staff-record [{:juxt.home/effective-from #inst "2019-04-01T00:00"
                                         :juxt.home/effective-to #inst "2019-11-30T00:00"
                                         :juxt.home/employment-type "EMPLOYEE"
                                         :juxt.home/status "ACTIVE"}
                                        {:juxt.home/effective-from #inst "2019-11-30T00:00"
                                         :juxt.home/employment-type "EMPLOYEE"
                                         :juxt.home/status "TERMINATED"}])

(deftest staff-member-record->interval-test
  (testing "If effective-to is not provided, end is set to ceiling date"
    (is (= {:juxt.home/effective-from (t/date-time "2019-03-05T00:00")
            :tick/beginning (t/date-time "2019-03-05T00:00")
            :tick/end (t/date-time "2020-04-06T00:00")}
           (sut/staff-member-record->interval
            {:juxt.home/effective-from (t/date-time "2019-03-05T00:00")}
            (t/date-time "2020-04-06T00:00"))))
    (is (= {:juxt.home/effective-from (t/date-time "2021-06-01T00:00")
            :tick/beginning (t/date-time "2021-06-01T00:00")
            :tick/end (t/date-time "2022-01-01T00:00")}
           (sut/staff-member-record->interval
            {:juxt.home/effective-from (t/date-time "2021-06-01T00:00")}
            (t/date-time "2022-01-01T00:00")))))
  (testing "If effective-to is provided and is before ceiling date, end is set to effective-to"
    (is (= {:juxt.home/effective-from (t/date-time "2019-03-05T00:00")
            :juxt.home/effective-to (t/date-time "2019-06-06T00:00")
            :tick/beginning (t/date-time "2019-03-05T00:00")
            :tick/end (t/date-time "2019-06-06T00:00")}
           (sut/staff-member-record->interval
            {:juxt.home/effective-from (t/date-time "2019-03-05T00:00")
             :juxt.home/effective-to (t/date-time "2019-06-06T00:00")}
            (t/date-time "2020-04-06T00:00"))))
    (is (= {:juxt.home/effective-from (t/date-time "2021-06-01T00:00")
            :juxt.home/effective-to (t/date-time "2021-09-04T00:00")
            :tick/beginning (t/date-time "2021-06-01T00:00")
            :tick/end (t/date-time "2021-09-04T00:00")}
           (sut/staff-member-record->interval
            {:juxt.home/effective-from (t/date-time "2021-06-01T00:00")
             :juxt.home/effective-to (t/date-time "2021-09-04T00:00")}
            (t/date-time "2022-01-01T00:00")))))
  (testing "If effective-to is provided and is after ceiling date, end is set to ceiling-date"
    (is (= {:juxt.home/effective-from (t/date-time "2019-03-05T00:00")
            :juxt.home/effective-to (t/date-time "2021-06-06T00:00")
            :tick/beginning (t/date-time "2019-03-05T00:00")
            :tick/end (t/date-time "2020-04-06T00:00")}
           (sut/staff-member-record->interval
            {:juxt.home/effective-from (t/date-time "2019-03-05T00:00")
             :juxt.home/effective-to (t/date-time "2021-06-06T00:00")}
            (t/date-time "2020-04-06T00:00"))))
    (is (= {:juxt.home/effective-from (t/date-time "2021-06-01T00:00")
            :juxt.home/effective-to (t/date-time "2022-09-04T00:00")
            :tick/beginning (t/date-time "2021-06-01T00:00")
            :tick/end (t/date-time "2022-01-01T00:00")}
           (sut/staff-member-record->interval
            {:juxt.home/effective-from (t/date-time "2021-06-01T00:00")
             :juxt.home/effective-to (t/date-time "2022-09-04T00:00")}
            (t/date-time "2022-01-01T00:00"))))))

(deftest working-pattern->interval-test
  (let [sunday (t/date "2022-06-05")
        monday (t/date "2022-06-06")
        tuesday (t/date "2022-06-07")]

    (testing "Returns nil when employee does not work on the given date"
      (is (nil? (sut/working-pattern->interval
                 {}
                 sunday)))
      (is (nil? (sut/working-pattern->interval
                 {"MONDAY" #:juxt.home {:beginning-local-time "09:00"
                                        :end-local-time "17:00"}}
                 sunday)))
      (is (nil? (sut/working-pattern->interval
                 {"MONDAY" #:juxt.home {:beginning-local-time "09:00"
                                        :end-local-time "17:00"}}
                 tuesday)))
      (is (nil? (sut/working-pattern->interval
                 {"SUNDAY" #:juxt.home {:beginning-local-time "09:00"
                                        :end-local-time "17:00"}
                  "TUESDAY" #:juxt.home {:beginning-local-time "09:00"
                                         :end-local-time "17:00"}}
                 monday))))

    (testing "Returns tick/beginning and tick/end version of working time for date"
      (is (= {:tick/beginning (t/date-time "2022-06-06T09:00")
              :tick/end (t/date-time "2022-06-06T17:00")}
             (sut/working-pattern->interval
                 {"MONDAY" #:juxt.home {:beginning-local-time "09:00"
                                        :end-local-time "17:00"}}
                 monday)))
      (is (= {:tick/beginning (t/date-time "2022-06-07T09:00")
              :tick/end (t/date-time "2022-06-07T17:00")}
             (sut/working-pattern->interval
                 {"TUESDAY" #:juxt.home {:beginning-local-time "09:00"
                                        :end-local-time "17:00"}}
                 tuesday)))
      (is (= {:tick/beginning (t/date-time "2022-06-06T10:00")
              :tick/end (t/date-time "2022-06-06T16:00")}
             (sut/working-pattern->interval
                 {"MONDAY" #:juxt.home {:beginning-local-time "10:00"
                                        :end-local-time "16:00"}}
                 monday))))))

(deftest round-half-down-test
  (testing "Rounds down for values <= 0.5"
    (is (= 1.0 (sut/round-half-down (bigdec 1.0))))
    (is (= 1.0 (sut/round-half-down (bigdec 1.5))))
    (is (= 1.0 (sut/round-half-down (- (bigdec 1.5) 0.0000000000000000000001M)))))
  (testing "Rounds up for values > 0.5"
    (is (= 2.0 (sut/round-half-down (bigdec 1.9999999999999999999))))
    (is (= 2.0 (sut/round-half-down (bigdec 1.6))))
    (is (= 2.0 (sut/round-half-down (+ (bigdec 1.5) 0.0000000000000000000001M))))))

(deftest to-displayable-float-test
  (testing "Generates floats to 4 significant figures"
    (is (= 1.0 (sut/to-displayable-float (bigdec 1.0))))
    (is (= 1.5 (sut/to-displayable-float (bigdec 1.5))))
    (is (= 1.1 (sut/to-displayable-float (bigdec 1.1))))
    (is (= 1.001 (sut/to-displayable-float (bigdec 1.001))))
    (is (= 1.0 (sut/to-displayable-float (bigdec 1.0001))))
    (is (= 1.001 (sut/to-displayable-float (bigdec 1.0005))))
    (is (= 1.123 (sut/to-displayable-float (bigdec 1.1234567))))
    (is (= 123500.0 (sut/to-displayable-float (bigdec 123456.1234567))))))

(deftest monthly-holiday-accrual-rate-test
  (testing "Calculates accrual rate for given entitlement, working pattern and full time hours for full time employees"
    (is (= 1 (sut/monthly-holiday-accrual-rate #:juxt.home {:holiday-entitlement 12 :working-pattern full-time-work-pattern :full-time-hours 40})))
    (is (= 1/2 (sut/monthly-holiday-accrual-rate #:juxt.home {:holiday-entitlement 6 :working-pattern full-time-work-pattern :full-time-hours 40})))
    (is (= 2 (sut/monthly-holiday-accrual-rate #:juxt.home {:holiday-entitlement 12 :working-pattern full-time-work-pattern :full-time-hours 20}))))

  (testing "Calculates accrual rate for given entitlement, working pattern and full time hours for part-time employees"
    (is (= 1/2 (sut/monthly-holiday-accrual-rate #:juxt.home {:holiday-entitlement 12 :working-pattern part-time-work-pattern :full-time-hours 40})))
    (is (= 1/4 (sut/monthly-holiday-accrual-rate #:juxt.home {:holiday-entitlement 6 :working-pattern part-time-work-pattern :full-time-hours 40})))
    (is (= 1 (sut/monthly-holiday-accrual-rate #:juxt.home {:holiday-entitlement 12 :working-pattern part-time-work-pattern :full-time-hours 20})))))


(deftest calculate-deductions-this-period-test
  (testing "Given the start of period, returns today's deduction"
    (is (zero? (sut/calculate-deductions-this-period {} {:deduction {:days {:value 0}}} true)))
    (is (= 1 (sut/calculate-deductions-this-period {} {:deduction {:days {:value 1}}} true)))
    (is (= 5 (sut/calculate-deductions-this-period {} {:deduction {:days {:value 5}}} true))))

  (testing "Given not the start of period, returns today's deduction plus yesterday's deductions-this-period"
    (is (zero? (sut/calculate-deductions-this-period {:total-of-deductions-this-period 0} {:deduction {:days {:value 0}}} false)))
    (is (= 1 (sut/calculate-deductions-this-period {:total-of-deductions-this-period 1} {:deduction {:days {:value 0}}} false)))
    (is (= 1 (sut/calculate-deductions-this-period {:total-of-deductions-this-period 0} {:deduction {:days {:value 1}}} false)))
    (is (= 3 (sut/calculate-deductions-this-period {:total-of-deductions-this-period 2} {:deduction {:days {:value 1}}} false)))))

(deftest calculate-carry-test
  (testing "Given the start of the year, a balance less than or equal to max-carry (5) and no deductions returns balance"
    (is (zero? (sut/calculate-carry {:balance 0} {:start-of-period? true :start-of-year? true})))
    (is (= 3 (sut/calculate-carry {:balance 3} {:start-of-period? true :start-of-year? true})))
    (is (= 5 (sut/calculate-carry {:balance 5} {:start-of-period? true :start-of-year? true}))))

  (testing "Given the start of the year, a balance more than max-carry (5) and no deductions returns max-carry"
    (is (= 5 (sut/calculate-carry {:balance 6} {:start-of-period? true :start-of-year? true})))
    (is (= 5 (sut/calculate-carry {:balance 7} {:start-of-period? true :start-of-year? true}))))

  (testing "Given the start of period, but not start of year returns yesterday's balance"
    (is (zero? (sut/calculate-carry {:balance 0} {:start-of-period? true :start-of-year? false})))
    (is (= 1 (sut/calculate-carry {:balance 1} {:start-of-period? true :start-of-year? false})))
    (is (= 9 (sut/calculate-carry {:balance 9} {:start-of-period? true :start-of-year? false}))))

  (testing "Given not the start of a period, returns yesterday's carry"
    (is (zero? (sut/calculate-carry {:carry 0} {:start-of-period? false :start-of-year? false})))
    (is (= 1 (sut/calculate-carry {:carry 1} {:start-of-period? false :start-of-year? false})))
    (is (= 9 (sut/calculate-carry {:carry 9} {:start-of-period? false :start-of-year? false})))))

(deftest calculate-balance-test
  (testing "Given no holidays accrued, no deductions and no carry, returns 0"
    (is (zero? (sut/calculate-balance {} 0 0))))

  (testing "Returns the accrual this period, plus the carry, minus the deductions this period"
    (is (= 1 (sut/calculate-balance {} 0 1)))
    (is (= 2 (sut/calculate-balance {} 0 2)))
    (is (= 1 (sut/calculate-balance {} 1 2)))
    (is (zero? (sut/calculate-balance {} 2 2)))
    (is (= 3 (sut/calculate-balance {:closing-holiday-days-accrued-since-period-beginning {:value 2}} 0 1)))
    (is (= 9 (sut/calculate-balance {:closing-holiday-days-accrued-since-period-beginning {:value 6}} 0 3)))
    (is (= 5 (sut/calculate-balance {:closing-holiday-days-accrued-since-period-beginning {:value 6}} 4 3)))))

(deftest staff-records->periods-test
  (with-precision 4 :rounding java.math.MathContext/HALF_DOWN
    (testing "Periods begin at start date to end of the year"
      (let [periods (vec (sut/staff-records->periods basic-full-time-staff-record (t/year "2019") [] []))]
        (is (= 1 (count periods)))
        (is (= (t/date-time "2019-04-01T00:00") (:tick/beginning (first periods))))
        (is (= (t/date-time "2020-01-01T00:00") (:tick/end (first periods))))))

    (testing "If ceiling year is > employee history start date, periods are split at year boundary"
      (let [periods (vec (sut/staff-records->periods basic-full-time-staff-record (t/year "2020") [] []))]
        (is (= 2 (count periods)))
        (is (= (t/date-time "2019-04-01T00:00") (:tick/beginning (first periods))))
        (is (= (t/date-time "2020-01-01T00:00") (:tick/end (first periods))))
        (is (= (t/date-time "2020-01-01T00:00") (:tick/beginning (second periods))))
        (is (= (t/date-time "2021-01-01T00:00") (:tick/end (second periods))))))

    (testing "If employee is terminated before ceiling year end, period extends to termination"
      (let [periods (-> terminated-full-time-staff-record
                        (sut/staff-records->periods (t/year "2019") [] [])
                        vec)]
        (is (= 2 (count periods)))
        (is (= (t/date-time "2019-04-01T00:00") (:tick/beginning (first periods))))
        (is (= (t/date-time "2019-11-30T00:00") (:tick/end (first periods))))
        (is (= (t/date-time "2019-11-30T00:00") (:tick/beginning (second periods))))
        (is (= (t/date-time "2020-01-01T00:00") (:tick/end (second periods))))))

    (testing "usual-working-from and usual-working-to are assigned from working-interval for date"
      (let [period (-> basic-full-time-staff-record
                       (assoc-in [0 :juxt.home/working-pattern] part-time-work-pattern)
                       (sut/staff-records->periods (t/year "2019") [] [])
                       first
                       :dates
                       ((fn [dates] (reduce
                           (fn [acc n] (assoc acc (:date n) (select-keys n [:usual-working-from :usual-working-to])))
                           {}
                           dates))))]
        (is (= {:usual-working-from "09:00"
                :usual-working-to "17:00"}
               (get period (t/date "2019-04-02"))))
        (is (= {:usual-working-from "09:00"
                :usual-working-to "13:00"}
               (get period (t/date "2019-04-03"))))))))
