-- Test data for Read-Easy integration tests

-- Insert test users
INSERT INTO users (id, name, email, status, created_at) VALUES
(1, 'John Doe', 'john.doe@example.com', 'ACTIVE', '2024-01-15 10:00:00'),
(2, 'Jane Smith', 'jane.smith@example.com', 'ACTIVE', '2024-01-16 11:30:00'),
(3, 'Bob Wilson', 'bob.wilson@example.com', 'INACTIVE', '2024-01-17 09:15:00'),
(4, 'Alice Brown', 'alice.brown@example.com', 'ACTIVE', '2024-01-18 14:45:00'),
(5, 'Charlie Davis', 'charlie.davis@example.com', 'PENDING', '2024-01-19 16:00:00');

-- Insert test products
INSERT INTO products (id, name, description, price, category, stock_quantity) VALUES
(1, 'Laptop Pro', 'High-performance laptop', 1299.99, 'Electronics', 50),
(2, 'Wireless Mouse', 'Ergonomic wireless mouse', 49.99, 'Electronics', 200),
(3, 'USB-C Hub', 'Multi-port USB-C hub', 79.99, 'Electronics', 150),
(4, 'Desk Chair', 'Ergonomic office chair', 399.99, 'Furniture', 30),
(5, 'Monitor Stand', 'Adjustable monitor stand', 129.99, 'Furniture', 75);

-- Insert test orders
INSERT INTO orders (id, user_id, total_amount, status, created_at) VALUES
(1, 1, 1349.98, 'COMPLETED', '2024-02-01 10:30:00'),
(2, 1, 79.99, 'COMPLETED', '2024-02-05 14:15:00'),
(3, 2, 449.98, 'SHIPPED', '2024-02-10 09:00:00'),
(4, 3, 1299.99, 'PENDING', '2024-02-12 11:45:00'),
(5, 4, 129.98, 'PROCESSING', '2024-02-15 16:30:00');

-- Insert order items
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
(1, 1, 1, 1299.99),
(1, 2, 1, 49.99),
(2, 3, 1, 79.99),
(3, 4, 1, 399.99),
(3, 2, 1, 49.99),
(4, 1, 1, 1299.99),
(5, 5, 1, 129.99);
