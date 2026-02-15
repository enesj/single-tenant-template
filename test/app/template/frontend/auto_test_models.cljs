(ns app.template.frontend.auto-test-models
  "Inline database models for frontend test data generation.")

(def models
  {:users {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                    [:email [:varchar 255] {:null false}]
                    [:full_name [:varchar 255]]
                    [:role [:enum :user-role] {:null false :default "member"}]
                    [:status [:enum :user-status] {:null false :default "active"}]
                    [:created_at :timestamptz {:default [:now]}]
                    [:updated_at :timestamptz {:default [:now]}]]
           :types [[:user-role :enum {:choices ["admin" "member" "viewer" "unassigned"]}]
                   [:user-status :enum {:choices ["active" "inactive" "suspended"]}]]}

   :suppliers {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                        [:display_name [:varchar 255] {:null false}]
                        [:normalized_key [:varchar 255] {:null false}]
                        [:created_at :timestamptz {:default [:now]}]
                        [:updated_at :timestamptz {:default [:now]}]]}

   :payers {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                     [:type [:enum :payer-type] {:null false}]
                     [:label [:varchar 255] {:null false}]

                     [:is_default :boolean {:default false}]
                     [:created_at :timestamptz {:default [:now]}]
                     [:updated_at :timestamptz {:default [:now]}]]
            :types [[:payer-type :enum {:choices ["cash" "card" "account" "person"]}]]}

   :receipts {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                       [:user_id :uuid {:foreign-key :users/id :on-delete :set-null}]
                       [:storage_key :text {:null false}]
                       [:file_hash [:varchar 64] {:null false}]
                       [:status [:enum :receipt-status] {:null false :default "uploaded"}]
                       [:created_at :timestamptz {:default [:now]}]
                       [:updated_at :timestamptz {:default [:now]}]]
              :types [[:receipt-status :enum {:choices ["uploaded" "parsing" "parsed" "extracting" "extracted" "review_required" "approved" "posted" "failed"]}]]}

   :expenses {:fields [[:id :uuid {:primary-key true :default [:gen_random_uuid]}]
                       [:user_id :uuid {:foreign-key :users/id :on-delete :set-null}]
                       [:receipt_id :uuid {:foreign-key :receipts/id :on-delete :set-null}]
                       [:supplier_id :uuid {:foreign-key :suppliers/id :null false}]
                       [:payer_id :uuid {:foreign-key :payers/id :null false}]
                       [:purchased_at :timestamptz {:null false}]
                       [:total_amount [:decimal 12 2] {:null false :check [:> :total_amount 0]}]
                       [:currency [:enum :currency] {:null false :default "BAM"}]
                       [:is_posted :boolean {:default true :null false}]
                       [:created_at :timestamptz {:default [:now]}]
                       [:updated_at :timestamptz {:default [:now]}]]
              :types [[:currency :enum {:choices ["BAM" "EUR" "USD"]}]]}})
