<!-- ai: {:tags [:libs :uix] :kind :guide} -->

# Migrating Components from Reagent to UIX

This guide outlines best practices and common patterns for migrating ClojureScript components from Reagent to UIX, based on our experience migrating the select component.

## Component Structure

### UIX Component Pattern
```clojure
(defui my-component [{:keys [prop1 prop2] :as props}]
  (let [processed-value (process-props props)]
    ($ :div
       ($ child-component processed-value))))
```

### Reagent Wrapper Pattern
```clojure
(defn reagent-wrapper [props]
  (let [props (js->clj props :keywordize-keys true)]
    ($ uix-component props)))
```

## Key Principles

### 1. Props Handling
- Convert JS props to CLJS in the Reagent wrapper using `js->clj` with `:keywordize-keys true`
- Keep UIX components pure CLJS, handling only CLJS data structures
- Don't handle JS objects directly in UIX components

### 2. React Hooks Usage
```clojure
;; Good - Hooks at top level, conditions in arguments
(urf/use-subscribe [::sub (when condition? value)])

;; Bad - Hooks inside conditions
(when condition?
  (urf/use-subscribe [::sub value]))
```

### 3. Component Communication
- Use `r/as-element` when needed for Reagent->UIX conversion
- Use the `$` macro for UIX components
- Pass callbacks and event handlers properly

## Common Pitfalls

1. **Props Nesting**
   - Watch out for props nested under `:children` in JS objects
   - Handle both direct props and nested children cases

2. **Data Conversion**
   - Loss of data when converting between JS and CLJS
   - Mixing JS and CLJS data structures
   - Incorrect handling of function references

3. **React Hooks**
   - Rules violations (conditional calls)
   - Improper subscription handling

## Best Practices

1. **Clean Boundaries**
   - Keep JS/CLJS conversion at the component boundary (Reagent wrapper)
   - Use destructuring for cleaner code
   - Maintain consistent data types throughout the component chain

2. **Debugging**
   - Add debug logging during migration
   - Log props at each step of the component chain
   - Verify data types and structures

3. **Testing**
   - Test both direct props and nested children cases
   - Ensure proper event handling
   - Verify subscription and hook behavior

## Example Migration

Here's a complete example of migrating a select component:

```clojure
;; UIX Component
(defui select-input-uix
  [{:keys [id label options value on-change errors required] :as props}]
  ($ :div {:class "tw-mb-4"}
     ($ common/label {:text label
                     :for id
                     :required required})
     ($ common/select (merge
                      {:id id
                       :value value
                       :options options
                       :class (validation/get-field-class {:errors errors})
                       :required required}
                      (clean-props props)
                      {:on-change #(on-change (.. % -target -value))}))
     (when errors
       ($ :div {:class "error"} errors))))

;; Reagent Wrapper
(defn select-input
  [props]
  (let [props (js->clj props :keywordize-keys true)]
    ($ select-input-uix props)))
```

## Migration Checklist

1. [ ] Create UIX component with proper prop destructuring
2. [ ] Create Reagent wrapper with JS->CLJS conversion
3. [ ] Add debug logging
4. [ ] Test direct props passing
5. [ ] Test nested children props
6. [ ] Verify event handlers
7. [ ] Check hook compliance
8. [ ] Test entity and non-entity cases
9. [ ] Verify data flow
10. [ ] Remove debug logging when stable

This pattern can be reused for migrating other components from Reagent to UIX, following the same principles of clean data conversion and proper component boundaries.
