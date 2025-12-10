# Services DRY Refactoring - Phase 2 Plan

## Overview
Continue applying the factory pattern to medium and complex services in the expenses domain, building on the successful Phase 1 foundation.

## Phase 1 Summary (Completed)
✅ **Factory System**: Created `services_factory.clj` with generic CRUD operations
✅ **Configuration**: Built `service_configs.clj` with 9 entity configs
✅ **Simple Services**: Refactored `article_aliases` (96→43 lines) and `price_observations` (111→53 lines)
✅ **Compilation**: Zero errors, all services registering successfully
✅ **Backward Compatibility**: Legacy function names for existing routes

## Phase 2: Medium Complexity Services

### 1. Receipts Service (`receipts.clj`)
**Current Analysis**: ~300 lines with specialized business logic
- **CRUD Operations**: Standard get, list, update, delete
- **Specialized Logic**: File upload, duplicate detection, status workflow
- **Business Rules**: Receipt processing, extraction results, retry mechanisms

**Refactoring Strategy**:
```clojure
;; Configuration Enhancements
(def receipt-config
  {:table-name "receipts"
   :primary-key :id
   :required-fields [:storage_key :file_hash]
   :allowed-order-by {...}
   :field-transformers {:file_hash compute-file-hash}
   :before-insert (fn [data]
                    (-> data
                        (assoc :id (UUID/randomUUID)
                               :status "uploaded")
                        (update :status #(vector :cast % :receipt_status))))
   :custom-operations {:upload-receipt! upload-receipt-factory
                       :update-status! status-update-factory
                       :mark-failed! failure-handler-factory}})
```

**Implementation Steps**:
1. Extract file hashing logic to utility function
2. Create factory for duplicate detection
3. Build status transition handlers
4. Preserve specialized upload workflow
5. Maintain extraction result storage

**Expected Reduction**: 300→120 lines (60% reduction)

### 2. Price History Service (`price_history.clj`)
**Current Analysis**: ~200 lines with time-series logic
- **Core Function**: `record-observation!` with currency casting
- **Queries**: Time-based filtering, supplier filtering
- **Business Logic**: Price observation tracking

**Refactoring Strategy**:
```clojure
(def price-history-config
  {:table-name "price_history"
   :required-fields [:article_id :supplier_id :price_date :unit_price]
   :field-transformers {:currency #(when % [:cast % :currency])}
   :custom-operations {:record-observation! custom-observation-factory}})
```

**Expected Reduction**: 200→80 lines (60% reduction)

## Phase 3: High Complexity Services

### 3. Suppliers Service (`suppliers.clj`)
**Current Analysis**: ~219 lines with normalization and search
- **Normalization**: `normalize-supplier-key` for deduplication
- **Search**: Autocomplete with fuzzy matching
- **Business Logic**: Find-or-create patterns

**Refactoring Strategy**:
```clojure
(def supplier-config
  {:table-name "suppliers"
   :required-fields [:display_name]
   :field-transformers {:normalized_key normalize-supplier-key}
   :before-insert (fn [data]
                    (-> data
                        (assoc :id (UUID/randomUUID))
                        (assoc :normalized_key
                               (normalize-supplier-key (:display_name data)))))
   :before-update (fn [id updates]
                    (if (:display_name updates)
                      (-> updates
                          (assoc :normalized_key
                                 (normalize-supplier-key (:display_name updates)))
                          (assoc :updated_at [:now]))
                      updates))
   :custom-operations {:find-by-normalized-key search-factory
                       :find-or-create-supplier! upsert-factory})
   :search-fields [:display_name :normalized_key]})
```

**Expected Reduction**: 219→90 lines (59% reduction)

### 4. Payers Service (`payers.clj`)
**Current Analysis**: ~150 lines with enum validation
- **Validation**: Payer type enums (card, cash, etc.)
- **Business Logic**: Account number validation by type

**Refactoring Strategy**:
```clojure
(def payer-config
  {:table-name "payers"
   :required-fields [:display_name :payer_type]
   :field-transformers {:account_number validate-account-by-type}
   :custom-validation {:payer_type #(contains? payer-types %)}
   :before-insert (fn [data]
                    (-> data
                        (assoc :id (UUID/randomUUID))
                        (validate-payer-type)))
   :search-fields [:display_name :payer_type]})
```

**Expected Reduction**: 150→65 lines (57% reduction)

### 5. Articles Service (`articles.clj`)
**Current Analysis**: ~250 lines with canonicalization and aliases
- **Normalization**: `normalize-article-key` for deduplication
- **Alias Management**: Create and manage article aliases
- **Business Logic**: Complex article-alias relationships

**Refactoring Strategy**:
```clojure
(def article-config
  {:table-name "articles"
   :required-fields [:canonical_name]
   :field-transformers {:canonical_name normalize-article-key
                        :normalized_key #(normalize-article-key %)}
   :before-insert (fn [data]
                    (-> data
                        (assoc :id (UUID/randomUUID))
                        (assoc :normalized_key
                               (normalize-article-key (:canonical_name data)))))
   :custom-operations {:create-alias! alias-creation-factory
                       :normalize-canonical-name normalize-article-key})
   :search-fields [:canonical_name :description]})
```

**Expected Reduction**: 250→100 lines (60% reduction)

### 6. Expenses Service (`expenses.clj`)
**Current Analysis**: ~300 lines with complex joins and validation
- **CRUD**: Complex expense with items management
- **Validation**: Multi-field validation rules
- **Joins**: Supplier, payer, receipt relationships

**Refactoring Strategy**:
```clojure
(def expense-config
  {:table-name "expenses"
   :required-fields [:supplier_id :payer_id :expense_date]
   :joins [[:suppliers :s] [:= :s/id :supplier_id]
           [:payers :p] [:= :p/id :payer_id]
           [:receipts :r] [:= :r/id :receipt_id]]
   :select-fields [[:e.*]
                   [:s/display_name :supplier_display_name]
                   [:p/display_name :payer_display_name]
                   [:r.original_filename :receipt_filename]]
   :before-insert (fn [data]
                    (-> data
                        (assoc :id (UUID/randomUUID))
                        (validate-expense-data)))
   :custom-validation {:total-amount validate-total-amount
                       :expense-date validate-date-not-future})
   :search-fields [:s/display_name :p/display_name]})
```

**Expected Reduction**: 300→120 lines (60% reduction)

## Implementation Phases

### Phase 2.1: Medium Services (Days 1-2)
1. **Receipts Service**
   - Extract file utilities to shared module
   - Create upload factory with duplicate detection
   - Build status workflow handlers
   - Test with existing routes

2. **Price History Service**
   - Simplify observation recording
   - Extract currency casting logic
   - Maintain time-series query capabilities

### Phase 2.2: Complex Services (Days 3-5)
3. **Suppliers Service**
   - Preserve normalization logic
   - Build search/find-or-create factories
   - Maintain autocomplete functionality

4. **Payers Service**
   - Extract validation logic
   - Build enum validation factory
   - Maintain account type validation

5. **Articles Service**
   - Preserve canonicalization
   - Build alias management factories
   - Maintain complex relationships

6. **Expenses Service**
   - Handle complex joins in configuration
   - Build validation factories
   - Maintain expense-items relationships

## Technical Considerations

### 1. Custom Operations Factory
```clojure
(defn build-custom-operations
  "Build custom operations that don't fit standard CRUD patterns"
  [config]
  (cond-> {}
    (:upload-receipt-factory config)
    (assoc :upload-receipt! ((:upload-receipt-factory config) config))

    (:find-or-create-factory config)
    (assoc :find-or-create! ((:find-or-create-factory config) config))))
```

### 2. Validation Enhancements
```clojure
(defn build-validator
  "Build entity-specific validator from configuration"
  [field-validators custom-validators]
  (fn validate-entity [data]
    (reduce-kv
      (fn [errors k v]
        (if-let [validator (get field-validators k)]
          (if (validator v)
            errors
            (assoc errors k (str "Invalid " (name k))))
          errors))
      {}
      data)))
```

### 3. Relationship Management
```clojure
(defn build-relationship-handler
  "Handle complex entity relationships during CRUD operations"
  [relationship-config]
  (fn handle-relationships [db entity operation]
    (doseq [rel (:relationships relationship-config)]
      (case (:type rel)
        :has-many (handle-has-many db entity rel)
        :belongs-to (handle-belongs-to db entity rel)
        :has-one (handle-has-one db entity rel)))))
```

## Testing Strategy

### 1. Unit Tests for Factory
- Test generic CRUD operations
- Test custom operation factories
- Test validation transformers

### 2. Integration Tests
- Test service compatibility with existing routes
- Test custom business logic preservation
- Test backward compatibility

### 3. Performance Tests
- Ensure no performance regression
- Test query optimization
- Memory usage validation

## Expected Outcomes

### Code Reduction Summary:
- **Total Current Lines**: ~1,350 lines across 9 services
- **Phase 1 Completed**: ~207 lines → 96 lines (53% reduction)
- **Phase 2 Target**: ~1,143 lines → ~550 lines (52% reduction)
- **Overall Target**: ~1,350 lines → ~650 lines (52% total reduction)

### Quality Improvements:
- **Consistency**: Standardized patterns across all services
- **Maintainability**: Centralized business logic in configurations
- **Testability**: Factory-based services easier to unit test
- **Extensibility**: New entities can be added with minimal code

### Architectural Benefits:
- **DRY Principle**: Eliminated code duplication
- **Configuration-Driven**: Entity behavior defined declaratively
- **Plugin Architecture**: Easy to add new operations via factories
- **Separation of Concerns**: Business logic separate from boilerplate

## Risk Mitigation

### 1. Backward Compatibility
- Maintain all existing function signatures
- Preserve custom business logic
- Ensure routes continue working

### 2. Incremental Approach
- Refactor one service at a time
- Test each service thoroughly
- Rollback capability for each phase

### 3. Documentation
- Update service documentation
- Add factory usage examples
- Document custom operation patterns

## Next Steps

1. **Start with Receipts** - Most complex medium service
2. **Extract Shared Utilities** - File hashing, validation helpers
3. **Build Custom Factories** - Upload, status, search operations
4. **Test Integration** - Ensure route compatibility
5. **Iterate Through Services** - Apply lessons learned to each service

This plan provides a clear roadmap for completing the services DRY refactoring while maintaining functionality and improving code quality.