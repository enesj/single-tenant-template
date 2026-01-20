-- Enforce desired ON DELETE behavior for key foreign keys.
-- - expense_items.expense_id -> expenses.id : ON DELETE CASCADE
-- - expenses.supplier_id     -> suppliers.id : ON DELETE RESTRICT
-- Idempotent: only adjusts constraints when they don't match.

DO 92710
BEGIN
  -- Ensure expense_items.expense_id has ON DELETE CASCADE
  IF EXISTS (
    SELECT 1
    FROM pg_constraint c
    JOIN pg_class r ON r.oid = c.conrelid
    WHERE r.relname = 'expense_items'
      AND c.conname = 'expense_items_expense_id_fkey'
      AND c.confdeltype <> 'c'  -- 'c' = CASCADE
  ) THEN
    ALTER TABLE public.expense_items
      DROP CONSTRAINT expense_items_expense_id_fkey;
    ALTER TABLE public.expense_items
      ADD CONSTRAINT expense_items_expense_id_fkey
        FOREIGN KEY (expense_id)
        REFERENCES public.expenses(id)
        ON DELETE CASCADE;
  END IF;
END 92710;

DO 92710
BEGIN
  -- Ensure expenses.supplier_id uses ON DELETE RESTRICT
  IF EXISTS (
    SELECT 1
    FROM pg_constraint c
    JOIN pg_class r ON r.oid = c.conrelid
    WHERE r.relname = 'expenses'
      AND c.conname = 'expenses_supplier_id_fkey'
      AND c.confdeltype <> 'r'  -- 'r' = RESTRICT
  ) THEN
    ALTER TABLE public.expenses
      DROP CONSTRAINT expenses_supplier_id_fkey;
    ALTER TABLE public.expenses
      ADD CONSTRAINT expenses_supplier_id_fkey
        FOREIGN KEY (supplier_id)
        REFERENCES public.suppliers(id)
        ON DELETE RESTRICT;
  END IF;
END 92710;

