(ns web.handlers.item)

(defn- add
  [state k id]
  (update state k
          (fn [v] (if id
                    (set (conj v id))
                    v))))
(defn- delete
  [state k id]
  (update state k
          (fn [v] (set (remove #(= id %) v)))))

(def handlers
  {:item/top
   (fn [state id]
     {:state (-> state
                 (add :toped id))
      :http {:params [:item/top {:id id}]
             :on-load [:item/vote-ready id]}})

   :item/vote-ready
   (fn [state id result]
     {:state (update-in state [:by-id id] merge result)})

   :item/untop
   (fn [state id]
     {:state (-> state
                 (delete :toped id))
      :http {:params [:item/untop {:id id}]
             :on-load [:item/vote-ready id]}})

   :item/down
   (fn [state id]
     {:state (-> state
                 (add :downed id))
      :http {:params [:item/down {:id id}]
             :on-load [:item/vote-ready id]}})

   :item/undown
   (fn [state id]
     {:state (-> state
                 (delete :downed id))
      :http {:params [:item/undown {:id id}]
             :on-load [:item/vote-ready id]}})

   })
