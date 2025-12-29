create TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255),
    product_name VARCHAR(255),
    price DECIMAL(18, 2),
    quantity INT,
    total_price DECIMAL(18, 2),
    vendor_name VARCHAR(255),
    product_entry_time DATETIME,
    record_updated_time DATETIME,
    record_deleted_time DATETIME,
    is_active BOOLEAN,
    size DECIMAL(18, 2),
    ktae DECIMAL(18, 2),
    gram DECIMAL(18, 2),
    invoice_number INT
);


create TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(255),
    is_active BOOLEAN,
    deleted_by DATETIME
);

create TABLE Company_Bill_Amount_Paid (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(255),
    bill_paid DECIMAL(19,2),
    balance DECIMAL(19,2),
);

create TABLE company_payment_time (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(255),
    amount_paid DECIMAL(19,2),
    payment_time DATETIME
);

create TABLE Customers_Balance (
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

create TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity INT,
    purchase_price DECIMAL(19,2),
    category VARCHAR(255),
    total_price DECIMAL(19,2),
    product_name VARCHAR(255)
);

create TABLE otp_request_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    request_time DATETIME
);

create TABLE password_reset_otp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    otp VARCHAR(10),
    expiration_time DATETIME
);

create TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255),
    product_name VARCHAR(255),
    price DECIMAL(19,2),
    quantity INT,
    total_price DECIMAL(19,2),
    product_entry_time DATETIME,
    record_updated_time DATETIME,
    record_deleted_time DATETIME,
    is_active BOOLEAN,
    size DECIMAL(19,2),
    ktae DECIMAL(19,2),
    gram DECIMAL(19,2),
    invoice_number INT
);


create TABLE product_name (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255),
    product_price DECIMAL(19,2),
    is_active BOOLEAN,
    deleted_by DATETIME,
    category_id BIGINT,
    CONSTRAINT fk_category
        FOREIGN KEY (category_id)
        REFERENCES category(id)
        ON delete CASCADE
);

create TABLE product_sell (
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

create TABLE vendors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE
);

create TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE
);

create TABLE company_invoice_amount (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number INT,
    grand_total DECIMAL(15,2),
    amount_paid DECIMAL(15,2),
    discount DECIMAL(15,2),
    rent DECIMAL(15,2),
    vendor_name VARCHAR(255)
    is_active BOOLEAN,
    description VARCHAR(255)
);

create TABLE customer_invoice_record (
    id INT AUTO_INCREMENT PRIMARY KEY,

    invoice_number INT,

    grand_total DECIMAL(19,2),
    amount_paid DECIMAL(19,2),

    discount DECIMAL(19,2),
    rent DECIMAL(19,2),

    description VARCHAR(255),
    customer_name VARCHAR(255),
    billing_month VARCHAR(7)
    is_active BOOLEAN
);
create TABLE sale (
    id INT AUTO_INCREMENT PRIMARY KEY,

    category VARCHAR(255),
    product_name VARCHAR(255),

    price DECIMAL(19,2),
    quantity INT,
    total_price DECIMAL(19,2),

    customer_name VARCHAR(255),

    sale_entry_time DATETIME,
    record_updated_time DATETIME,
    record_deleted_time DATETIME,

    is_active BOOLEAN,

    size DECIMAL(19,2),
    ktae DECIMAL(19,2),
    gram DECIMAL(19,2),

    invoice_number INT
);

create TABLE customer_payment_time (
    id INT AUTO_INCREMENT PRIMARY KEY,

    customer_name VARCHAR(255) NOT NULL,

    amount_paid DECIMAL(19,2) NOT NULL,

    payment_time DATETIME NOT NULL,

    billing_month VARCHAR(7) NOT NULL,
    -- stored as YYYY-MM because of YearMonthAttributeConverter

    invoice_number INT NOT NULL
);

CREATE TABLE company_stock_return (
    id INT AUTO_INCREMENT PRIMARY KEY,

    invoice_number INT NOT NULL,

    vendor_name VARCHAR(255) NOT NULL,

    product_name VARCHAR(255) NOT NULL,

    category VARCHAR(255) NOT NULL,

    returned_quantity INT NOT NULL,

    price DECIMAL(19,2) NOT NULL,

    total_amount DECIMAL(19,2) NOT NULL,

    return_time DATETIME NOT NULL
);





