(ns woodworking.store
  "SSoT for the ISCO-08 7523 independent woodworking machine
  operations actor (itonami actor pattern, ADR-2607011000 /
  CLAUDE.md Actors section; README's 'Robotics premise' — a
  material-feed robot performs lumber loading and cut-piece removal
  under this advisor/governor pair, which never dispatches hardware
  itself). Modeled on cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client — a registered organization (:client-id, :name)
    order  — a registered production order {:order-id :client-id
             :name :target-dimension-mm number :tolerance-mm number
             :quantity-ordered number}. `:target-dimension-mm` ±
             `:tolerance-mm` is the registered dimensional band a
             proposed cut's measured dimension must fall inside
             (dimensional tolerance is a band, not a suggestion);
             `:quantity-ordered` is the registered ceiling a proposed
             run's units produced must not exceed.
    record — a committed operating record (approved cut run) —
             written ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (order [s order-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-order! [s o])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (order [_ order-id] (get-in @a [:orders order-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-order! [s o]
    (swap! a assoc-in [:orders (:order-id o)] o) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :orders {} :records [] :ledger []}
                                   seed)))))
