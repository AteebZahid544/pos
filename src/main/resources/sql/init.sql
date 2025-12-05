CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255),
    product_name VARCHAR(255),
    price DECIMAL(15,2),
    quantity INT,
    total_price DECIMAL(15,2),
    vendor_name VARCHAR(255),
    product_entry_time DATETIME,
    record_updated_time DATETIME,
    record_deleted_time DATETIME,
    is_active BOOLEAN
);

CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(255),
    is_active BOOLEAN,
    deleted_by DATETIME
);

CREATE TABLE Company_Bill_Amount_Paid (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(255),
    bill_paid DECIMAL(19,2),
    billing_month VARCHAR(7),
    balance DECIMAL(19,2),
    pay_bill_time DATETIME
);

CREATE TABLE company_payment_time (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(255),
    amount_paid DECIMAL(19,2),
    payment_time DATETIME
);

CREATE TABLE Customers_Balance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255),
    bill_paid DECIMAL(19,2),
    billing_month VARCHAR(7),
    balance DECIMAL(19,2),
    pay_bill_time DATETIME,
    total_amount DECIMAL(19,2),
    bill_time VARCHAR(255),
    deleted_payment DECIMAL(19,2),
    deleted_payment_time DATETIME
);

CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity INT,
    purchase_price DECIMAL(19,2),
    category VARCHAR(255),
    total_price DECIMAL(19,2),
    product_name VARCHAR(255)
);

CREATE TABLE otp_request_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    request_time DATETIME
);

CREATE TABLE password_reset_otp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    otp VARCHAR(10),
    expiration_time DATETIME
);

CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255),
    product_name VARCHAR(255),
    price DECIMAL(19,2),
    quantity INT,
    total_price DECIMAL(19,2),
    vendor_name VARCHAR(255),
    product_entry_time DATETIME,
    record_updated_time DATETIME,
    record_deleted_time DATETIME,
    is_active BOOLEAN
);

CREATE TABLE product_name (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255),
    product_price DECIMAL(19,2),
    is_active BOOLEAN,
    deleted_by DATETIME,
    category_id BIGINT,
    CONSTRAINT fk_category
        FOREIGN KEY (category_id)
        REFERENCES category(id)
        ON DELETE CASCADE
);

CREATE TABLE product_sell (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product VARCHAR(255),
    category VARCHAR(255),
    quantity INT,
    price DECIMAL(19,2),
    discount DECIMAL(19,2),
    customer_name VARCHAR(255),
    total_amount DECIMAL(19,2),
    amount_paid DECIMAL(19,2),
    sell_time DATETIME
);


