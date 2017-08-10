-- name: retrieve-all-invoices-query
-- Retrieves all the invoices.
SELECT invoice.* FROM invoice;

-- name: retrieve-invoice-query
-- Retrieves an invoice
SELECT invoice.* FROM invoice
WHERE invoice.id = :invoice_id;

-- name: update-invoice-paid-query!
-- Updates an invoice.
UPDATE invoice
SET paid = :paid
WHERE invoice.id = :invoice_id;

-- name: update-invoice-usable-query!
-- Updates an invoice.
UPDATE invoice
SET usable = :usable
WHERE invoice.id = :invoice_id;
