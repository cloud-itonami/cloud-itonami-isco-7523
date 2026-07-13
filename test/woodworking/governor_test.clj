(ns woodworking.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [woodworking.store :as store]
            [woodworking.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Woodshop"})
    (store/register-order! st {:order-id "O-1" :client-id "client-1"
                               :name "cabinet-panel-run"
                               :target-dimension-mm 600.0
                               :tolerance-mm 0.5
                               :quantity-ordered 50})
    st))

(defn- cut-run [dim units]
  {:op :approve-cut-run :effect :propose :order-id "O-1"
   :measured-dimension-mm dim :units-produced units :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-tolerance-and-quantity
  (let [st (fresh-store)
        v (governor/check req {} (cut-run 600.2 40) st)]
    (is (:ok? v))))

(deftest ok-at-exact-tolerance-and-quantity-edges
  (testing "the tolerance band and production ceiling boundaries are inclusive"
    (let [st (fresh-store)]
      (is (:ok? (governor/check req {} (cut-run 600.5 50) st)))
      (is (:ok? (governor/check req {} (cut-run 599.5 50) st))))))

(deftest hard-on-dimension-out-of-tolerance
  (testing "dimensional tolerance is a band, not a suggestion"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (cut-run 605.0 40) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :dimension-out-of-tolerance (:rule %)) (:violations v))))))

(deftest hard-on-production-exceeds-order
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cut-run 600.0 80) :confidence 0.99) st)]
    (is (:hard? v))
    (is (some #(= :production-exceeds-order (:rule %)) (:violations v)))))

(deftest hard-on-unknown-order
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cut-run 600.0 40) :order-id "O-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-order (:rule %)) (:violations v)))))

(deftest hard-on-foreign-order
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (cut-run 600.0 40) st)]
      (is (:hard? v))
      (is (some #(= :order-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (cut-run 600.0 40) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cut-run 600.0 40) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-unguarded-blade-operation-even-at-high-confidence
  (testing "no unguarded-blade operation without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-unguarded-blade-operation :effect :propose
                                    :order-id "O-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-quality-clearance-even-at-high-confidence
  (testing "quality-inspection-failure clearance always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :clear-quality-inspection-failure :effect :propose
                                    :order-id "O-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cut-run 600.0 40) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
