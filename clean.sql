-- Drop triggers and functions
DROP TRIGGER IF EXISTS refresh_popular_products_trigger ON lesi9952.order_products;
DROP FUNCTION IF EXISTS refresh_popular_products();

DROP TRIGGER IF EXISTS check_stock ON lesi9952.order_products;
DROP FUNCTION IF EXISTS enforce_stock();

DROP TRIGGER IF EXISTS check_total_price ON lesi9952.orders;
DROP FUNCTION IF EXISTS enforce_total_price();

-- Drop materialized views
DROP MATERIALIZED VIEW IF EXISTS lesi9952.popular_products;

-- Drop views
DROP VIEW IF EXISTS lesi9952.customer_orders;
DROP VIEW IF EXISTS lesi9952.order_details_with_products;

-- Drop indexes
DROP INDEX IF EXISTS idx_products_name;
DROP INDEX IF EXISTS idx_customers_email;

-- Drop tables
DROP TABLE IF EXISTS lesi9952.order_products;
DROP TABLE IF EXISTS lesi9952.orders;
DROP TABLE IF EXISTS lesi9952.products;
DROP TABLE IF EXISTS lesi9952.customers;
