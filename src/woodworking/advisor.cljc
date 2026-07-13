(ns woodworking.advisor
  "WoodworkingAdvisor — the advisor named in this repository's
  README, proposing a production operation (approve a cut run,
  approve unguarded-blade operation, clear a quality-inspection
  failure) from a production order, material spec and safety guard
  protocol. Swappable mock/llm; the advisor ONLY proposes —
  `woodworking.governor` checks the dimensional tolerance and
  production ceiling independently and always escalates unguarded-
  blade/quality-clearance decisions. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-cut-run|:approve-unguarded-blade-operation|:clear-quality-inspection-failure
               :effect :propose :order-id str
               :measured-dimension-mm number :units-produced number
               :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake order-id measured-dimension-mm units-produced] :as request}]
  {:op op
   :effect :propose
   :order-id order-id
   :measured-dimension-mm measured-dimension-mm
   :units-produced units-produced
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a woodworking-machine-operations advisor. Given a request,
   propose an :op, the :order-id, :measured-dimension-mm and
   :units-produced, an honest :confidence and a :stake. Never call an
   out-of-tolerance cut or an over-order production run conforming —
   the governor checks both against the registered order record.
   Unguarded-blade and quality-clearance decisions always require
   human sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
