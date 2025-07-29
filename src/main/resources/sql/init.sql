CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255),
    selling_price DECIMAL(19,2),
    is_active BOOLEAN,
    amount_paid INT,
    purchasing_price DECIMAL(19,2),
    stock_entry_time DATETIME
);
