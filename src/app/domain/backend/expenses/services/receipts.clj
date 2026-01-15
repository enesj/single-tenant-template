(ns app.domain.backend.expenses.services.receipts
  "Receipt upload, status transitions, and approval workflow.

  This namespace re-exports selected public functions from sub-modules for
  backward compatibility. New code may require sub-modules directly:

  - receipts.parsing   - Value parsing utilities
  - receipts.storage   - File storage & hashing
  - receipts.status    - Status transitions & claims
  - receipts.queries   - List/get/delete operations
  - receipts.approval  - Review, approve & post workflows"
  (:require
    [app.domain.backend.expenses.services.receipts.approval :as approval]
    [app.domain.backend.expenses.services.receipts.queries :as queries]
    [app.domain.backend.expenses.services.receipts.status :as status]
    [app.domain.backend.expenses.services.receipts.storage :as storage]))

;; storage
(def resolve-local-receipt-file storage/resolve-local-receipt-file)
(def upload-receipt! storage/upload-receipt!)

;; status
(def update-status! status/update-status!)
(def claim-for-parsing! status/claim-for-parsing!)
(def claim-for-extracting! status/claim-for-extracting!)
(def mark-failed! status/mark-failed!)
(def retry-extraction! status/retry-extraction!)
(def reset-for-ocr! status/reset-for-ocr!)
(def store-extraction-results! status/store-extraction-results!)

;; queries
(def get-receipt queries/get-receipt)
(def delete-receipt! queries/delete-receipt!)
(def list-receipts queries/list-receipts)
(def list-user-receipts queries/list-user-receipts)
(def get-user-receipt queries/get-user-receipt)
(def list-pending-for-processing queries/list-pending-for-processing)

;; approval
(def save-review! approval/save-review!)
(def approve-and-post! approval/approve-and-post!)
(def approve-and-post-for-user! approval/approve-and-post-for-user!)
(def approve-and-post-for-user-any! approval/approve-and-post-for-user-any!)
