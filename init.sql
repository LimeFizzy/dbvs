-- Table: lesi9952.customers
CREATE TABLE lesi9952.customers (
    customer_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone TEXT NOT NULL,
    address_street TEXT,
    address_city TEXT,
    address_country TEXT,
    address_zip_code TEXT
);

-- Table: lesi9952.products
CREATE TABLE lesi9952.products (
    product_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    quantity INTEGER NOT NULL CHECK (quantity >= 0)
);

-- Table: lesi9952.orders
CREATE TABLE lesi9952.orders (
    order_id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL REFERENCES lesi9952.customers(customer_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status TEXT NOT NULL CHECK (status IN ('Pending', 'Completed', 'Cancelled')),
    total_price DECIMAL(10, 2) NOT NULL CHECK (total_price >= 0),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_amount DECIMAL(10, 2),
    payment_method TEXT
);

-- Table: lesi9952.order_products
CREATE TABLE lesi9952.order_products (
    order_id INTEGER NOT NULL REFERENCES lesi9952.orders(order_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    product_id INTEGER NOT NULL REFERENCES lesi9952.products(product_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, product_id)
);

-- Unique Index: idx_customers_email
CREATE UNIQUE INDEX idx_customers_email ON lesi9952.customers(email);

-- Regular Index: idx_products_name
CREATE INDEX idx_products_name ON lesi9952.products(name);

-- Function: enforce_total_price
CREATE OR REPLACE FUNCTION enforce_total_price()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.total_price <> (
        SELECT SUM(p.price * op.quantity)
        FROM lesi9952.order_products op
        JOIN lesi9952.products p ON op.product_id = p.product_id
        WHERE op.order_id = NEW.order_id
    )) THEN
        RAISE EXCEPTION 'Total price does not match the sum of products';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: check_total_price
CREATE TRIGGER check_total_price
AFTER INSERT OR UPDATE ON lesi9952.orders
FOR EACH ROW EXECUTE FUNCTION enforce_total_price();

-- Function: enforce_stock
CREATE OR REPLACE FUNCTION enforce_stock()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.quantity > (
        SELECT quantity FROM lesi9952.products WHERE product_id = NEW.product_id
    )) THEN
        RAISE EXCEPTION 'Insufficient stock for product %', NEW.product_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: check_stock
CREATE TRIGGER check_stock
BEFORE INSERT OR UPDATE ON lesi9952.order_products
FOR EACH ROW EXECUTE FUNCTION enforce_stock();

-- View: lesi9952.order_details_with_products
CREATE OR REPLACE VIEW lesi9952.order_details_with_products AS
SELECT 
    o.order_id,
    c.name AS customer_name,
    o.order_date,
    o.status,
    o.total_price,
    p.product_id,
    p.name AS product_name,
    p.price AS product_price,
    op.quantity,
    (p.price * op.quantity) AS subtotal
FROM lesi9952.orders o
JOIN lesi9952.customers c ON o.customer_id = c.customer_id
JOIN lesi9952.order_products op ON o.order_id = op.order_id
JOIN lesi9952.products p ON op.product_id = p.product_id;

-- View: lesi9952.customer_orders
CREATE OR REPLACE VIEW lesi9952.customer_orders AS
SELECT 
    c.name AS customer_name,
    o.order_id,
    o.total_price,
    o.status,
    o.order_date
FROM lesi9952.customers c
JOIN lesi9952.orders o ON c.customer_id = o.customer_id;

-- Materialized View: lesi9952.popular_products
CREATE MATERIALIZED VIEW lesi9952.popular_products AS
SELECT 
    p.product_id,
    p.name,
    SUM(op.quantity) AS total_sold
FROM lesi9952.products p
JOIN lesi9952.order_products op ON p.product_id = op.product_id
GROUP BY p.product_id, p.name
ORDER BY total_sold DESC;

REFRESH MATERIALIZED VIEW lesi9952.popular_products;

-- Function: refresh_popular_products
CREATE OR REPLACE FUNCTION refresh_popular_products()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW lesi9952.popular_products;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger: refresh_popular_products_trigger
CREATE TRIGGER refresh_popular_products_trigger
AFTER INSERT OR DELETE OR UPDATE ON lesi9952.order_products
FOR EACH STATEMENT
EXECUTE FUNCTION refresh_popular_products();
