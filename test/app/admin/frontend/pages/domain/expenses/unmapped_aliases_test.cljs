(ns app.admin.frontend.pages.domain.expenses.unmapped-aliases-test
	(:require
		[app.admin.frontend.pages.domain.expenses.unmapped-aliases :as unmapped-aliases-page]
		[app.domain.frontend.expenses.events.unmapped-aliases :as unmapped-aliases-events]
		[app.template.frontend.events.list.ui-state :as ui-state])
	(:require-macros
		[cljs.test :refer [deftest is testing]]))

(deftest dispatch-admin-unmapped-aliases-refresh-configures-server-pagination-and-loads-first-page
	(testing "admin unmapped aliases refresh enables server pagination and loads page 1 with 50 rows"
		(let [dispatches (atom [])
					sync-dispatches (atom [])]
			(unmapped-aliases-page/dispatch-admin-unmapped-aliases-refresh!
				#(swap! dispatches conj %)
				#(swap! sync-dispatches conj %))

			(is (= [[::ui-state/set-pagination-mode :unmapped-aliases :server]
							[::ui-state/set-refresh-event :unmapped-aliases [::unmapped-aliases-events/load-list]]]
						@sync-dispatches))
			(is (= [[::unmapped-aliases-events/load-list {:page 1 :per-page 50}]]
						@dispatches)))))