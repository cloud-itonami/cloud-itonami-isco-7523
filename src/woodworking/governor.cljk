(ns woodworking.governor
  "WoodworkingMachineGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  the robot-dispensed physical work (lumber loading, cut-piece
  removal) an advisor may propose. The governor never dispatches
  hardware itself. Modeled on cloud-itonami-isco-4311's
  bookkeeping.governor. Cut twist: a proposed cut's measured
  dimension must fall inside the registered [target - tolerance,
  target + tolerance] band — dimensional tolerance is a band, not a
  suggestion — and a proposed run's units produced is arithmetic
  comparison against the registered ordered quantity.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose (the
                           governor never dispatches hardware; it only
                           gates what the robot may execute).
    3. order basis          — a cut approval must cite a REGISTERED
                           order belonging to this client.
    4. dimensional tolerance — the proposed measured dimension must
                           fall inside the order's registered
                           [target - tolerance, target + tolerance]
                           band (interval containment, not a
                           suggestion).
    5. production ceiling   — the proposed units produced must not
                           exceed the order's registered
                           :quantity-ordered.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-unguarded-blade-operation (no unguarded-blade
                           operation without the governor gate).
    7. :op :clear-quality-inspection-failure (quality-inspection-
                           failure clearance always requires human
                           sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [woodworking.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-unguarded-blade-operation
                                     :clear-quality-inspection-failure})

(defn- hard-violations [{:keys [request proposal]} client-record o]
  (let [{:keys [op measured-dimension-mm units-produced]} proposal
        cut? (= :approve-cut-run op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and cut? (nil? o))
      (conj {:rule :unknown-order :detail "未登録 order への切断承認は不可"})

      (and cut? o (not= (:client-id o) (:client-id request)))
      (conj {:rule :order-wrong-client :detail "order が別 client のもの"})

      (and cut? o (number? measured-dimension-mm)
           (or (< measured-dimension-mm (- (:target-dimension-mm o) (:tolerance-mm o)))
               (> measured-dimension-mm (+ (:target-dimension-mm o) (:tolerance-mm o)))))
      (conj {:rule :dimension-out-of-tolerance
             :detail (str "測定寸法 " measured-dimension-mm "mm が許容帯 ["
                          (- (:target-dimension-mm o) (:tolerance-mm o)) ", "
                          (+ (:target-dimension-mm o) (:tolerance-mm o))
                          "]mm の外（寸法公差は帯域であって提案ではない）")})

      (and cut? o (number? units-produced) (> units-produced (:quantity-ordered o)))
      (conj {:rule :production-exceeds-order
             :detail (str "生産数量 " units-produced " > 登録済み受注数量 "
                          (:quantity-ordered o))}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `woodworking.store/Store`. Pure — never mutates
  the store, never dispatches the robot."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        o (some->> (:order-id proposal) (store/order store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record o)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
