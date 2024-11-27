-- Insert test data into lesi9952.customers
INSERT INTO lesi9952.customers (name, email, phone, address_street, address_city, address_country, address_zip_code)
VALUES
('John Doe', 'johndoe@example.com', '123-456-7890', '123 Elm St', 'Springfield', 'USA', '12345'),
('Jane Smith', 'janesmith@example.com', '987-654-3210', '456 Maple Ave', 'Shelbyville', 'USA', '54321'),
('Alice Johnson', 'alicej@example.com', '555-666-7777', '789 Oak Dr', 'Metropolis', 'USA', '67890'),
('Bob Brown', 'bobb@example.com', '444-555-6666', '321 Pine Ln', 'Gotham', 'USA', '98765'),
('Charlie White', 'charliew@example.com', '333-444-5555', '654 Cedar Rd', 'Star City', 'USA', '13579'),
('Diana Prince', 'dianap@example.com', '222-333-4444', '123 Amazon Way', 'Themyscira', 'USA', '24680'),
('Bruce Wayne', 'brucew@example.com', '111-222-3333', '1007 Mountain Dr', 'Gotham', 'USA', '10101');

-- Insert test data into lesi9952.products
INSERT INTO lesi9952.products (name, description, price, quantity)
VALUES
('Widget A', 'A basic widget.', 19.99, 100),
('Widget B', 'An advanced widget.', 29.99, 50),
('Gadget X', 'A useful gadget.', 49.99, 200),
('Gadget Y', 'An innovative gadget.', 59.99, 30),
('Tool Z', 'A versatile tool.', 24.99, 75),
('Tool X', 'An ergonomic tool.', 39.99, 60),
('Accessory P', 'A useful accessory.', 15.49, 150);

-- Insert test data into lesi9952.orders
INSERT INTO lesi9952.orders (customer_id, order_date, status, total_price, payment_date, payment_amount, payment_method)
VALUES
(1, '2024-11-01 10:00:00', 'Completed', 89.95, '2024-11-01 10:05:00', 89.95, 'Credit Card'),
(2, '2024-11-02 14:30:00', 'Pending', 59.98, NULL, NULL, NULL),
(3, '2024-11-03 09:15:00', 'Completed', 74.97, '2024-11-03 09:20:00', 74.97, 'PayPal'),
(4, '2024-11-04 11:45:00', 'Cancelled', 0.00, NULL, NULL, NULL),
(5, '2024-11-05 16:20:00', 'Completed', 29.99, '2024-11-05 16:25:00', 29.99, 'Debit Card'),
(6, '2024-11-06 18:00:00', 'Pending', 94.95, NULL, NULL, NULL),
(7, '2024-11-07 08:15:00', 'Completed', 124.93, '2024-11-07 08:20:00', 124.93, 'Credit Card');

-- Insert test data into lesi9952.order_products
INSERT INTO lesi9952.order_products (order_id, product_id, quantity)
VALUES
(1, 1, 2), -- Widget A x2 for order 1
(1, 3, 1), -- Gadget X x1 for order 1
(2, 2, 2), -- Widget B x2 for order 2
(3, 5, 3), -- Tool Z x3 for order 3
(5, 4, 1), -- Gadget Y x1 for order 5
(6, 6, 2), -- Tool X x2 for order 6
(7, 1, 1), -- Widget A x1 for order 7
(7, 3, 2), -- Gadget X x2 for order 7
(7, 7, 3); -- Accessory P x3 for order 7
