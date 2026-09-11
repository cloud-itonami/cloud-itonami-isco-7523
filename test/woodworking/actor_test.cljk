(ns woodworking.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [woodworking.actor :as actor]
            [woodworking.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Woodshop"})
    (store/register-order! st {:order-id "O-1" :client-id "client-1"
                               :name "cabinet-panel-run"
                               :target-dimension-mm 600.0
                               :tolerance-mm 0.5
                               :quantity-ordered 50})
    st))

(deftest commits-an-in-tolerance-in-quantity-cut-run
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-cut-run :stake :low
                 :order-id "O-1" :measured-dimension-mm 600.2 :units-produced 40}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-out-of-tolerance-cut-run
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-cut-run :stake :low
                 :order-id "O-1" :measured-dimension-mm 610.0 :units-produced 40}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-clears-quality-failure-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :clear-quality-inspection-failure :stake :low
                 :order-id "O-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
