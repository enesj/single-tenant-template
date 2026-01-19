<!-- ai: {:tags [:libs] :kind :reference} -->

# Common UIx Migration Errors and Solutions

This document outlines common errors encountered when migrating from Reagent to UIx, along with their solutions and best practices.

## 1. Subscription Handling Errors

### Error: No protocol method IDeref.-deref defined for type null
```clojure
;; Error example
(let [value (urf/use-subscribe [::subs/my-sub])]
  ($ :div value))  ; Trying to use value without dereferencing

;; Solution
(let [value (urf/use-subscribe [::subs/my-sub])]
  ($ :div value))
```

### Error: No protocol method IDeref.-deref defined for type cljs.core/LazySeq
```clojure
;; Error example
(let [items (urf/use-subscribe [::subs/items])  ; items is a lazy sequence
      filtered-items (filter some-pred items)]    ; creates new lazy sequence
  ($ :div filtered-items))                        ; trying to deref lazy sequence

;; Solution
(let [items (urf/use-subscribe [::subs/items])
      filtered-items (vec (filter some-pred items))]  ; convert to vector
  ($ :div filtered-items))
```

Best Practices:
- Always use `@` to deref subscription values in UIx components
- Convert lazy sequences to vectors before using in components
- Handle nil values with defaults: `(or @sub-value default-value)`

## 2. React Children Errors

### Error: Objects are not valid as a React child
```clojure
;; Error example
($ :div {:class "container"}
   (map #($ :div %) items))  ; Trying to directly use map result

;; Solution
($ :div {:class "container"}
   (for [item items]         ; Use for comprehension
     ($ :div {:key (:id item)} item)))
```

Best Practices:
- Always provide unique `:key` props for dynamic children
- Use `for` instead of `map` for generating React elements
- Convert collections to proper React elements before rendering

## 3. Collection Handling Issues

### Error: No protocol method ICollection.-conj defined for type object
```clojure
;; Error example
(conj (make-table-headers data)
      ($ :div "Actions"))  ; Trying to conj React element

;; Solution
(vec (concat (make-table-headers data)
            [($ :div "Actions")]))  ; Use concat and vec
```

Best Practices:
- Use `concat` for combining sequences of React elements
- Convert results to vectors using `vec`
- Handle collections before passing to React components

## 4. Component Conversion Issues

### Mixing Reagent and UIx Syntax
```clojure
;; Error example - mixing syntaxes
(defui my-component [props]
  [:div  ; Reagent syntax
   ($ :span "Hello")])  ; UIx syntax

;; Solution
(defui my-component [props]
  ($ :div
     ($ :span "Hello")))
```

Best Practices:
- Use `defui` for UIx components
- Use `$` macro for all element creation in UIx
- Keep components either all Reagent or all UIx until migration is complete

## 5. Props Handling

### Common Props Issues
```clojure
;; Error example - incorrect props passing
($ my-component props)  ; Passing raw props object

;; Solution
($ my-component {:key "unique-key"
                :prop1 value1
                :prop2 value2})
```

Best Practices:
- Use proper destructuring in component definitions
- Pass props as maps with `$` syntax
- Handle JavaScript/ClojureScript conversion at boundaries

## 6. Event Handling

### Error: Cannot read properties of undefined (reading 'preventDefault')
```clojure
;; Error example
($ :form {:on-submit (fn [e]
                      (.preventDefault e)
                      (handle-submit))}
   ...)

;; Solution
($ :form {:on-submit (fn [^js e]
                      (.preventDefault e)
                      (handle-submit))}
   ...)
```

Best Practices:
- Type hint JavaScript event objects with `^js`
- Use kebab-case for event names (`:on-click` not `:onClick`)
- Return nil from event handlers to prevent React warnings

## 7. Hook Usage

### Error: Invalid hook call
```clojure
;; Error example - conditional hook usage
(defui my-component [{:keys [condition]}]
  (when condition
    (urf/use-subscribe [::subs/data])))  ; Hook in conditional

;; Solution
(defui my-component [{:keys [condition]}]
  (let [data (urf/use-subscribe [::subs/data])]
    (when condition
      @data)))
```

Best Practices:
- Always call hooks at the top level
- Don't call hooks inside loops or conditions
- Use `use-effect` for side effects

## 8. State Management

### Error: Too many re-renders
```clojure
;; Error example - infinite loop
(defui counter []
  (let [count (uix.core/use-state 0)]
    ($ :div
       ($ :button
          {:on-click #(reset! count (inc @count))}
          "Increment"))))

;; Solution
(defui counter []
  (let [[count set-count] (uix.core/use-state 0)]
    ($ :div
       ($ :button
          {:on-click #(set-count inc)}
          "Increment"))))
```

Best Practices:
- Use `use-state` for local component state
- Prefer `use-memo` for expensive computations
- Use `use-callback` for memoized callbacks

## 9. Form Handling

### Error: Uncontrolled component warnings
```clojure
;; Error example
(defui input [{:keys [value on-change]}]
  ($ :input {:value value}))  ; Missing on-change handler

;; Solution
(defui input [{:keys [value on-change]}]
  ($ :input
     {:value value
      :on-change #(on-change (.. % -target -value))}))
```

Best Practices:
- Always provide both value and on-change for controlled inputs
- Use `use-state` for form state management
- Handle form submission with `prevent-default`

## 10. Performance Optimization

### Memory Leaks and Cleanup
```clojure
;; Error example - no cleanup
(defui timer []
  (let [[time set-time] (uix.core/use-state 0)]
    (js/setInterval #(set-time inc) 1000)
    ($ :div time)))

;; Solution
(defui timer []
  (let [[time set-time] (uix.core/use-state 0)]
    (uix.core/use-effect
      (fn []
        (let [interval (js/setInterval #(set-time inc) 1000)]
          #(js/clearInterval interval)))
      [])
    ($ :div time)))
```

Best Practices:
- Clean up subscriptions and timers in `use-effect`
- Use dependency arrays effectively
- Profile components for unnecessary re-renders

## Migration Checklist

When migrating a component from Reagent to UIx:

1. **Component Definition**
   - [ ] Convert `defn` to `defui`
   - [ ] Update namespace requires to include UIx
   - [ ] Convert Reagent syntax (`[]`) to UIx syntax (`$`)

2. **Props Handling**
   - [ ] Update props destructuring
   - [ ] Fix prop passing syntax
   - [ ] Add key props for dynamic elements

3. **Subscriptions**
   - [ ] Add `@` to all subscription calls
   - [ ] Handle nil values with defaults
   - [ ] Convert lazy sequences to vectors

4. **Collections**
   - [ ] Replace `map` with `for` for React elements
   - [ ] Use `concat` instead of `conj`
   - [ ] Convert collections to vectors when needed

5. **Testing**
   - [ ] Test with nil/empty values
   - [ ] Verify dynamic updates work
   - [ ] Check performance with large collections

## Resources

- [UIx Documentation](https://github.com/roman01la/uix)
- [React Keys Documentation](https://reactjs.org/docs/lists-and-keys.html)
- [ClojureScript Best Practices](https://guide.clojure.style/)

## Common Patterns

### 1. Conditional Rendering
```clojure
;; Preferred approach
(defui conditional-component [{:keys [condition]}]
  ($ :div
     (when condition
       ($ child-component))
     (if other-condition
       ($ component-a)
       ($ component-b))))
```

### 2. List Rendering
```clojure
;; Preferred approach
(defui list-component [{:keys [items]}]
  ($ :ul
     (for [item items]
       ($ :li {:key (:id item)}
          (:name item)))))
```

### 3. Event Delegation
```clojure
;; Preferred approach
(defui delegated-events []
  ($ :div {:on-click (fn [e]
                      (when-let [action (.. e -target -dataset -action)]
                        (handle-action action)))}
     ($ :button {:data-action "edit"} "Edit")
     ($ :button {:data-action "delete"} "Delete")))
```

## Additional Resources

- [React Hooks FAQ](https://reactjs.org/docs/hooks-faq.html)
- [React Performance Optimization](https://reactjs.org/docs/optimizing-performance.html)
- [ClojureScript and React Best Practices](https://guide.clojure.style/)
