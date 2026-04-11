(ns app.template.frontend.ui.z-scale
  "Canonical z-index scale for frontend stacking contexts.

  Raw z-index values should not be used directly in components. Every layer
  that can compete with another in the GLOBAL stacking context (the document
  body) is named here so the ordering between subsystems — table sticky
  chrome, dropdown portals, modal backdrop, modal-internal portals — is
  explicit and cannot silently drift.

  ## Why a single scale?

  Before this namespace existed, each subsystem picked its own z-index in
  isolation: the table had `z-[19000]` / `22000` / `15000`, dropdowns used
  `9999` / `20000`, modals defaulted to `9999`, smart-input used `99999`.
  Whoever bumped a number last won the stacking war, and a refactor that
  raised the table sticky header from a small value into the 19000+ range
  silently broke every modal that opened over a table — including the
  receipt-detail modal whose backdrop sat at 120 (see commit history).
  Centralising these here makes the invariants reviewable.

  ## Two kinds of layers

  1. **Global layers** — elements that compete in the document's root
     stacking context (e.g. `<tbody>` sticky cells, modal backdrops, dropdowns
     portal'd to `document.body`). Their absolute values matter relative to
     each other.

  2. **Local layers** — elements inside a parent that already established
     a stacking context (e.g. `<th>` cells inside the sticky `<thead>`).
     Their values only matter relative to siblings in the same context;
     the parent caps them. We still name them so raw numbers don't creep
     back into component code.

  ## Ordering (global, ascending)

      content                 0
      table-sticky-body      20    left sticky body cell (select column)
      table-sticky-actions  150    right sticky body cell (actions column)
                                   — must overlap left sticky body at row ends
      table-sticky-header   300    sticky `<thead>` wrapper
      sticky-nav            400    page-level sticky nav (reserved)
      dropdown-inline      1000    in-flow dropdown wrapper / popover-API
      dropdown-portal      2000    dropdown portal'd to <body>
      modal-backdrop       3000    modal overlay — MUST cover all table chrome
      modal-portal-child   4000    portal overlays ABOVE an open modal
                                   (smart-input autocomplete, select menus
                                   inside modal forms)
      toast                9000    top-level notifications (reserved)

  Gaps are intentional so new layers can slot in without renumbering.

  ## Editing rules

  - Preserve the ORDER. If you raise table-sticky-header above
    modal-backdrop, every modal opened over a table will get its sticky
    chrome poked through it again.
  - Tailwind v4 JIT scans literal class strings in `.cljs` files; arbitrary
    values like `z-[300]` only work when the literal substring `z-[300]`
    appears in source. Building class strings with `(str \"z-[\" const \"]\")`
    will silently produce no CSS — prefer `:style {:z-index ...}` instead.
  - Inside a sticky parent that already created a stacking context, the
    child's absolute z-index does not compete globally. Use the
    `local-low` / `local-high` constants for those rather than reaching
    into the global scale and getting confused later.")

;; --- Global stacking context ---

(def content              0)
(def table-sticky-body   20)
(def table-sticky-actions 150)
(def table-sticky-header 300)
(def sticky-nav          400)
(def dropdown-inline    1000)
(def dropdown-portal    2000)
(def modal-backdrop     3000)
(def modal-portal-child 4000)
(def toast              9000)

;; --- Local stacking context (relative to a parent that already
;; established its own stacking context — values do not compete globally) ---

(def local-low  10)
(def local-high 20)
