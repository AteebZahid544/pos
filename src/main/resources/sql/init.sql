
create TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(255),
    is_active BOOLEAN,
    deleted_by DATETIME
);

CREATE TABLE client_requirements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(255),
    contact_person VARCHAR(255),
    contact_number VARCHAR(255),
    requirements VARCHAR(2000),
    is_active BOOLEAN,
    INDEX (is_active)
);

CREATE TABLE Company_Bill_Amount_Paid (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(255),
    billing_month VARCHAR(7), -- Storing as "YYYY-MM" format
    balance DECIMAL(19, 2)
);

CREATE TABLE company_invoice_amount (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number INT,
    grand_total DECIMAL(19, 2),
    amount_paid DECIMAL(19, 2),
    discount DECIMAL(19, 2),
    rent DECIMAL(19, 2),
    description TEXT,
    vendor_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    status VARCHAR(50),
    invoice_image VARCHAR(255), -- Path to stored image
    gst_percentage DECIMAL(5, 2), -- Precision: 5 total digits, 2 decimal places (e.g., 18.00)
    gst_amount DECIMAL(19, 2),
    total_before_gst DECIMAL(19, 2),
    billing_month VARCHAR(7), -- Format: YYYY-MM (from YearMonthAttributeConverter)
    invoice_date DATETIME,
    purchase_date DATETIME);

    CREATE TABLE company_payment_time (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(255),
    amount_paid DECIMAL(19, 2),
    payment_time DATETIME,
    billing_month VARCHAR(7),
    invoice_number INT,
    is_active BOOLEAN DEFAULT TRUE);

CREATE TABLE Customer_Bill_Amount_Paid (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255),
    billing_month VARCHAR(7),
    balance DECIMAL(19, 2)
);

sql
CREATE TABLE customer_invoice_record (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number INT UNIQUE,
    grand_total DECIMAL(19, 2),
    total_purchase_cost DECIMAL(19, 2),
    amount_paid DECIMAL(19, 2),
    discount DECIMAL(19, 2),
    rent DECIMAL(19, 2),
    description TEXT,
    customer_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    billing_month VARCHAR(7),
    status VARCHAR(50),
    invoice_image VARCHAR(500),
    gst_percentage DECIMAL(5, 2),
    gst_amount DECIMAL(19, 2),
    total_before_gst DECIMAL(19, 2),
    invoice_date DATETIME,
    sale_date DATETIME);

    CREATE TABLE customer_payment_time (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255),
    amount_paid DECIMAL(19, 2),
    payment_time DATETIME,
    billing_month VARCHAR(7),
    invoice_number INT,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255),
    contact_number VARCHAR(255),
    address TEXT,
    is_active BOOLEAN
);

CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employ_name VARCHAR(255),
    address TEXT,
    designation VARCHAR(100),
    contact_number VARCHAR(255),
    salary DECIMAL(19, 2),
    id_card_image VARCHAR(500),
    salary_type VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE employee_salary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT,
    base_salary DECIMAL(19, 2),
    bonus DECIMAL(19, 2) DEFAULT 0.00,
    overtime DECIMAL(19, 2) DEFAULT 0.00,
    deduction DECIMAL(19, 2) DEFAULT 0.00,
    total_paid DECIMAL(19, 2),
    status VARCHAR(50),
    salary_month DATE,
    salary_type VARCHAR(50),
    advance_given DECIMAL(19, 2),
    advance_adjusted DECIMAL(19, 2),
    remaining_advance DECIMAL(19, 2),
    payment_type VARCHAR(50),
    paid_on DATETIME,
    is_active BOOLEAN,
    salary_date DATE
);

CREATE TABLE expense_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255),
    description TEXT,
    amount DOUBLE,
    expense_date DATE,
    expense_time TIME,
    is_active BOOLEAN,
    expense_type VARCHAR(50)
);

CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity INTEGER,
    purchase_price DECIMAL(19, 2),
    category VARCHAR(255),
    total_price DECIMAL(19, 2),
    product_name VARCHAR(255),
    size DECIMAL(10, 2),
    ktae DECIMAL(10, 2),
    gram DECIMAL(10, 2),
    added_month VARCHAR(7)
);

CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255),
    product_name VARCHAR(255),
    price DECIMAL(19, 2),
    quantity INTEGER,
    returned_quantity INTEGER,
    total_price DECIMAL(19, 2),
    product_entry_time DATETIME,
    record_updated_time DATETIME,
    record_deleted_time DATETIME,
    stock_return_time DATETIME,
    is_Active BOOLEAN,
    size DECIMAL(10, 2),
    ktae DECIMAL(10, 2),
    gram DECIMAL(10, 2),
    invoice_number INTEGER,
    status VARCHAR(50)
);

CREATE TABLE production_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT,
    product_name VARCHAR(255),
    employee_name VARCHAR(255),
    total_quantity INTEGER,
    company_name VARCHAR(255),
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    user_id BIGINT,
    session_token VARCHAR(255),
    last_updated_by BIGINT,
    pause_time DATETIME,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    total_elapsed_seconds BIGINT DEFAULT 0
);

CREATE TABLE product_manufacture (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255)
);

CREATE TABLE product_name (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255),
    purchase_price DECIMAL(19, 2),
    sell_price DECIMAL(19, 2),
    is_active BOOLEAN,
    deleted_by DATETIME,
    category_id BIGINT
);

CREATE TABLE production_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    step_name VARCHAR(255),
    step_order INTEGER,
    product_id BIGINT
);

CREATE TABLE sale (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255),
    product_name VARCHAR(255),
    price DECIMAL(19, 2),
    purchase_price DECIMAL(19, 2),
    purchase_cost DECIMAL(19, 2),
    quantity INTEGER,
    total_price DECIMAL(19, 2),
    customer_name VARCHAR(255),
    sale_entry_time DATETIME,
    record_updated_time DATETIME,
    record_deleted_time DATETIME,
    is_Active BOOLEAN,
    size DECIMAL(10, 2),
    ktae DECIMAL(10, 2),
    gram DECIMAL(10, 2),
    invoice_number INTEGER,
    returned_quantity INTEGER,
    return_time DATETIME,
    status VARCHAR(50)
);

CREATE TABLE step_time (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    step_name VARCHAR(255),
    step_order INTEGER,
    start_time DATETIME,
    end_time DATETIME,
    duration_in_seconds BIGINT,
    elapsed_seconds BIGINT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'NOT_STARTED',
    production_record_id BIGINT
);

CREATE TABLE vendors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(255),
    contact_number VARCHAR(255),
    address TEXT,
    is_active BOOLEAN
);

CREATE TABLE customer_invoice_record (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number INT,
    grand_total DECIMAL(19,2),
    total_purchase_cost DECIMAL(19,2),
    amount_Paid DECIMAL(19,2),
    discount DECIMAL(19,2),
    rent DECIMAL(19,2),
    description TEXT,
    customer_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    billing_month VARCHAR(7), -- For YearMonth (YYYY-MM)
    Status VARCHAR(50),
    invoiceImage VARCHAR(500),
    gst_percentage DECIMAL(5,2) DEFAULT 0.00,
    gst_amount DECIMAL(19,2) DEFAULT 0.00,
    total_before_gst DECIMAL(19,2) DEFAULT 0.00,
    invoice_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    sale_date DATETIME DEFAULT CURRENT_TIMESTAMP
);




