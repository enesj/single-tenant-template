-- Make expenses.supplier_id nullable for flexible manual expense entry.
-- Previously supplier was required; now expenses can be created with just
-- a category, store, or article as context.
ALTER TABLE expenses ALTER COLUMN supplier_id DROP NOT NULL;

-- Change ON DELETE from RESTRICT to SET NULL (consistent with other nullable FKs).
ALTER TABLE expenses DROP CONSTRAINT IF EXISTS expenses_supplier_id_fkey;
ALTER TABLE expenses ADD CONSTRAINT expenses_supplier_id_fkey
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL;
