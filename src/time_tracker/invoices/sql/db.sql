-- name: retrieve-all-invoices-query
-- Retrieves all the invoices.
SELECT invoice.* FROM invoice;

-- name: retrieve-invoice-query
-- Retrieves an invoice
SELECT invoice.* FROM invoice
WHERE invoice.id = :invoice_id;
