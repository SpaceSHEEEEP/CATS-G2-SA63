-- Initial schema managed by Flyway.
-- Keep future schema changes in new versioned migrations; do not edit V1 after release.

CREATE TABLE application (
    application_id INTEGER NOT NULL AUTO_INCREMENT,
    course_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    application_status ENUM(
        'APPLIED',
        'APPROVED',
        'CANCELLED',
        'COMPLETED',
        'DELETED',
        'REJECTED',
        'UPDATED'
    ) NULL,
    user_reason VARCHAR(255) NULL,
    manager_reason VARCHAR(255) NULL,
    has_been_paid BIT NULL,
    PRIMARY KEY (application_id)
) ENGINE=InnoDB;

CREATE TABLE `user` (
    user_id INTEGER NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NULL,
    budget DECIMAL(38, 2) NULL,
    username VARCHAR(255) NULL,
    password VARCHAR(255) NULL,
    is_active BIT NOT NULL,
    is_admin BIT NOT NULL,
    email VARCHAR(255) NULL,
    reports_to_id INTEGER NOT NULL,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB;
