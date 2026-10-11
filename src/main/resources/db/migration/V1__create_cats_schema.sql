CREATE TABLE user (
  user_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL,
  role VARCHAR(32),
  designation VARCHAR(32),
  active BOOLEAN,
  is_admin BOOLEAN NOT NULL DEFAULT FALSE,
  manager_user_id INTEGER,
  actual_allowance DECIMAL(38,2),
  budgeted_allowance DECIMAL(38,2),
  actual_days DOUBLE,
  budgeted_days DOUBLE,
  CONSTRAINT fk_user_manager FOREIGN KEY (manager_user_id) REFERENCES user(user_id)
);
CREATE INDEX idx_user_manager ON user (manager_user_id);

CREATE TABLE course_category (
  category_code VARCHAR(32) NOT NULL PRIMARY KEY,
  display_name VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);
-- Domain reference values mandated by the assignment, not demonstration records.
INSERT INTO course_category(category_code,display_name,enabled) VALUES
  ('INTERNAL','Internal Training',TRUE),
  ('EXTERNAL','External Course',TRUE),
  ('PROFESSIONAL','Professional Certification',TRUE);

CREATE TABLE training_provider (
  id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL UNIQUE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE course (
  course_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  course_name VARCHAR(255) NOT NULL,
  course_type VARCHAR(32) NOT NULL,
  duration VARCHAR(32) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  location VARCHAR(255) NOT NULL,
  training_provider VARCHAR(255) NOT NULL,
  provider_id INTEGER,
  fee DECIMAL(38,2) NOT NULL,
  archived BOOLEAN,
  CONSTRAINT fk_course_provider FOREIGN KEY (provider_id) REFERENCES training_provider(id),
  CONSTRAINT fk_course_category FOREIGN KEY (course_type) REFERENCES course_category(category_code)
);
CREATE INDEX idx_course_period ON course(start_date, end_date);
CREATE INDEX idx_course_provider ON course(provider_id);

CREATE TABLE application (
  id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  status VARCHAR(32) NOT NULL,
  user_reason VARCHAR(1000) NOT NULL,
  work_dissemination VARCHAR(1000),
  manager_reason VARCHAR(255),
  has_been_paid BOOLEAN,
  course_course_id INTEGER,
  user_id INTEGER,
  CONSTRAINT fk_application_course FOREIGN KEY (course_course_id) REFERENCES course(course_id),
  CONSTRAINT fk_application_employee FOREIGN KEY (user_id) REFERENCES user(user_id)
);
CREATE INDEX idx_application_owner ON application(user_id, status);

CREATE TABLE comment (
  id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  comment_text VARCHAR(255),
  course_course_id INTEGER,
  application_id INTEGER UNIQUE,
  CONSTRAINT fk_comment_course FOREIGN KEY (course_course_id) REFERENCES course(course_id),
  CONSTRAINT fk_comment_application FOREIGN KEY (application_id) REFERENCES application(id)
);

CREATE TABLE holiday (
  holiday_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  holiday_date DATE NOT NULL UNIQUE,
  holiday_name VARCHAR(255) NOT NULL
);

CREATE TABLE training_year (
  id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INTEGER NOT NULL,
  calendar_year INTEGER NOT NULL,
  day_limit DOUBLE NOT NULL,
  fee_limit DECIMAL(12,2) NOT NULL,
  CONSTRAINT uk_training_year UNIQUE (user_id, calendar_year),
  CONSTRAINT fk_training_year_user FOREIGN KEY (user_id) REFERENCES user(user_id)
);

CREATE TABLE fee_claim (
  id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  application_id INTEGER NOT NULL,
  employee_user_id INTEGER NOT NULL,
  amount DECIMAL(38,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  submitted_on DATE NOT NULL,
  manager_reason VARCHAR(255),
  paid_on DATE,
  payment_reference VARCHAR(120),
  receipt_reference VARCHAR(255),
  certificate_reference VARCHAR(255),
  receipt_data LONGBLOB,
  certificate_data LONGBLOB,
  CONSTRAINT fk_fee_claim_application FOREIGN KEY (application_id) REFERENCES application(id),
  CONSTRAINT fk_fee_claim_employee FOREIGN KEY (employee_user_id) REFERENCES user(user_id)
);
CREATE INDEX idx_fee_claim_employee ON fee_claim(employee_user_id, submitted_on);
CREATE INDEX idx_fee_claim_status ON fee_claim(status);

CREATE TABLE notification_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  recipient VARCHAR(255) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  body VARCHAR(4000) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  next_attempt_at DATETIME(6) NOT NULL,
  sent_at DATETIME(6),
  attempts INTEGER NOT NULL DEFAULT 0,
  last_error VARCHAR(255)
);
CREATE INDEX idx_outbox_pending ON notification_outbox(sent_at, next_attempt_at, id);
