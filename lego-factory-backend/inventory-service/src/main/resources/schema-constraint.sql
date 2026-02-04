-- Add CHECK constraint to prevent negative stock values
-- This is defense-in-depth: application logic + database constraint
-- Note: H2 supports CHECK constraints

-- Add non-negative constraint for stock_records table
-- JPA creates the table via ddl-auto, this adds additional constraints

-- The constraint will be applied after JPA schema creation
-- For H2 in-memory, this runs on each startup

ALTER TABLE stock_records
ADD CONSTRAINT IF NOT EXISTS check_non_negative_quantity
CHECK (quantity >= 0);

-- Also ensure the version column is initialized
-- (JPA @Version handles this, but explicit initialization doesn't hurt)
-- ALTER TABLE stock_records ALTER COLUMN version SET DEFAULT 0;
