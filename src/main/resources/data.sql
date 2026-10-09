    -- insert dummy data into user table
INSERT INTO user (name, budgeted_allowance, actual_allowance, budgeted_days, actual_days, username, password, email, is_admin) VALUES
    ('Bob', 10000, 10000, 10.0, 10.0, 'bobby', 'pw', 'bob@gmail.com', false),
    ('Tim', 10000, 10000, 15.0, 15.0, 'timmy', 'pw', 'tim@gmail.com', false),
    ('Ben', 10000, 10000, 10.5, 10.5, 'benny', 'pw', 'ben@gmail.com', false),
    ('Dan', 9500, 9500, 10.0, 10.0, 'danny', 'pw', 'dan@gmail.com', false),
    ('Sam', 11000, 11000, 10.0, 10.0, 'sammy', 'pw', 'sam@gmail.com', false),
    ('Tom', 12000, 12000, 10.0, 10.0, 'tommy', 'pw', 'tom@gmail.com', false),
    ('Ron', 8500, 8500, 10.0, 10.0, 'ronny', 'pw', 'ron@gmail.com', false),
    ('Son', 10500, 10500, 10.0, 10.0, 'sonny', 'pw', 'son@gmail.com', false),
    ('Ken', 10000, 10000, 10.0, 10.0, 'kenny', 'pw', 'ken@gmail.com', false),
    ('Min', 10000, 10000, 10.0, 10.0, 'minny', 'pw', 'min@gmail.com', true); -- Admin User
    
-- Management hierarchy
-- Tim and Tom report to Bob (CEO)
UPDATE user employee
	JOIN user manager ON manager.username = 'bobby'
	SET employee.manager_user_id = manager.user_id
	WHERE employee.username IN ('timmy', 'tommy');
-- Ben, Dan and Sam report to Tim
UPDATE user employee
	JOIN user manager ON manager.username = 'timmy'
	SET employee.manager_user_id = manager.user_id
	WHERE employee.username IN ('benny', 'danny', 'sammy');
-- Ron, Son and Ken report to Tom
UPDATE user employee
	JOIN user manager ON manager.username = 'tommy'
	SET employee.manager_user_id = manager.user_id
	WHERE employee.username IN ('ronny', 'sonny', 'kenny');

-- insert dummy data into course table
    INSERT INTO course (course_name, course_type, duration, start_date, end_date, location, training_provider, fee) VALUES 
    ('Java OOPJ', 'INTERNAL', 'HALFDAYAM', '2026-10-14', '2026-10-14', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Java EE', 'INTERNAL', 'HALFDAYPM', '2026-10-14', '2026-10-14', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Web Application Development', 'INTERNAL', 'FULLDAY', '2026-10-15', '2026-10-19', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Database Design', 'INTERNAL', 'HALFDAYPM', '2026-11-02', '2026-11-02', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Cloud Computing Fundamentals', 'INTERNAL', 'FULLDAY', '2026-11-10', '2026-11-11', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Software Quality Assurance', 'INTERNAL', 'HALFDAYAM', '2026-12-01', '2026-12-01', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Enterprise Architecture', 'INTERNAL', 'FULLDAY', '2026-12-10', '2026-12-14', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('SQL Fundam', 'EXTERNAL', 'FULLDAY', '2026-10-20', '2026-10-21', 'Centre A', 'A Training Provider', 400.00),
    ('Java PRO Certification', 'PROFESSIONAL', 'FULLDAY', '2026-10-22', '2026-10-23', 'Centre B', 'B Cert Provider', 600.00),
    ('Advanced Python Programming', 'INTERNAL', 'FULLDAY', '2026-10-26', '2026-10-27', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Machine Learning Basics', 'INTERNAL', 'FULLDAY', '2026-10-28', '2026-10-29', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Cybersecurity Essentials', 'EXTERNAL', 'FULLDAY', '2026-11-03', '2026-11-05', 'Centre A', 'SecureTech Academy', 850.00),
    ('Agile Scrum Master', 'PROFESSIONAL', 'FULLDAY', '2026-11-10', '2026-11-11', 'Virtual', 'Agile Global', 1200.00),
    ('UI/UX Design Principles', 'INTERNAL', 'HALFDAYAM', '2026-11-12', '2026-11-12', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('DevOps Pipeline Implementation', 'EXTERNAL', 'FULLDAY', '2026-11-13', '2026-11-17', 'Centre B', 'DevOps Hub', 950.00),
    ('Data Analytics with R', 'INTERNAL', 'FULLDAY', '2026-11-18', '2026-11-19', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Project Management Professional', 'PROFESSIONAL', 'FULLDAY', '2026-11-20', '2026-11-24', 'Centre C', 'PMI Institute', 1500.00),
    ('Microservices Architecture', 'INTERNAL', 'FULLDAY', '2026-11-25', '2026-11-27', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Docker and Kubernetes', 'EXTERNAL', 'FULLDAY', '2026-11-30', '2026-12-01', 'Centre A', 'CloudNative Ltd', 700.00),
    ('Business Intelligence Tools', 'INTERNAL', 'HALFDAYPM', '2026-12-02', '2026-12-02', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Blockchain Fundamentals', 'EXTERNAL', 'FULLDAY', '2026-12-03', '2026-12-04', 'Centre B', 'CryptoEdu', 1100.00),
    ('Linux System Administration', 'INTERNAL', 'FULLDAY', '2026-12-07', '2026-12-08', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Certified Information Systems Auditor', 'PROFESSIONAL', 'FULLDAY', '2026-12-09', '2026-12-11', 'Centre C', 'ISACA', 1800.00),
    -- ('API Design and Development', 'INTERNAL', 'HALFDAYAM', '2026-12-14', '2026-12-14', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Artificial Intelligence Ethics', 'INTERNAL', 'HALFDAYPM', '2026-12-15', '2026-12-15', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('IT Service Management (ITIL)', 'PROFESSIONAL', 'FULLDAY', '2026-12-16', '2026-12-17', 'Centre A', 'AXELOS Partner', 800.00),
    -- ('NoSQL Database Management', 'EXTERNAL', 'FULLDAY', '2026-12-18', '2026-12-21', 'Centre B', 'DataStack', 650.00),
    -- ('React Frontend Framework', 'INTERNAL', 'FULLDAY', '2026-12-22', '2026-12-23', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Angular Enterprise Apps', 'EXTERNAL', 'FULLDAY', '2026-12-28', '2026-12-29', 'Centre A', 'WebPros', 750.00),
    -- ('Network Security Operations', 'PROFESSIONAL', 'FULLDAY', '2027-01-04', '2027-01-06', 'Centre C', 'CyberSec Academy', 1350.00),
    -- ('Digital Transformation Strategy', 'INTERNAL', 'HALFDAYAM', '2027-01-07', '2027-01-07', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Big Data Processing with Spark', 'EXTERNAL', 'FULLDAY', '2027-01-08', '2027-01-12', 'Centre B', 'DataFlow', 1250.00),
    -- ('Certified Cloud Solutions Architect', 'PROFESSIONAL', 'FULLDAY', '2027-01-13', '2027-01-15', 'Centre C', 'Cloud Certs', 1600.00),
    -- ('Mobile App Dev with Flutter', 'INTERNAL', 'FULLDAY', '2027-01-18', '2027-01-19', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Test Driven Development', 'INTERNAL', 'HALFDAYPM', '2027-01-20', '2027-01-20', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Continuous Integration and Deployment', 'EXTERNAL', 'FULLDAY', '2027-01-21', '2027-01-22', 'Centre A', 'DevOps Pros', 800.00),
    -- ('Enterprise Data Governance', 'INTERNAL', 'FULLDAY', '2027-01-25', '2027-01-26', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Ethical Hacking and Penetration Testing', 'PROFESSIONAL', 'FULLDAY', '2027-01-27', '2027-01-29', 'Centre C', 'SecOps Global', 1450.00),
    -- ('Quality Engineering Foundations', 'INTERNAL', 'HALFDAYAM', '2027-02-01', '2027-02-01', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Business Analysis Fundamentals', 'EXTERNAL', 'FULLDAY', '2027-02-02', '2027-02-03', 'Centre B', 'BA Institute', 600.00),
    -- ('Deep Learning with PyTorch', 'INTERNAL', 'FULLDAY', '2027-02-04', '2027-02-05', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Enterprise Networking', 'EXTERNAL', 'FULLDAY', '2027-02-09', '2027-02-10', 'Centre A', 'NetOps Academy', 700.00),
    -- ('Serverless Computing Architecture', 'INTERNAL', 'HALFDAYPM', '2027-02-11', '2027-02-11', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Certified Kubernetes Administrator', 'PROFESSIONAL', 'FULLDAY', '2027-02-12', '2027-02-16', 'Centre C', 'Cloud Native Academy', 1500.00),
    -- ('Advanced SQL Tuning', 'INTERNAL', 'HALFDAYAM', '2027-02-17', '2027-02-17', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Rust Programming Language', 'EXTERNAL', 'FULLDAY', '2027-02-18', '2027-02-19', 'Centre B', 'Systems Dev Hub', 900.00),
    -- ('Customer Experience Design', 'INTERNAL', 'FULLDAY', '2027-02-22', '2027-02-23', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Financial Technology Overview', 'EXTERNAL', 'HALFDAYPM', '2027-02-24', '2027-02-24', 'Centre A', 'FinTech Guild', 450.00),
    -- ('IT Risk Management', 'PROFESSIONAL', 'FULLDAY', '2027-02-25', '2027-02-26', 'Centre C', 'Risk Governance', 1100.00),
    -- ('Infrastructure as Code', 'INTERNAL', 'FULLDAY', '2027-03-01', '2027-03-02', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Natural Language Processing', 'EXTERNAL', 'FULLDAY', '2027-03-03', '2027-03-04', 'Centre B', 'AI Labs', 1000.00),
    -- ('Scrum Product Owner', 'PROFESSIONAL', 'FULLDAY', '2027-03-05', '2027-03-08', 'Centre A', 'Agile Alliance', 950.00),
    -- ('Data Warehousing Concepts', 'INTERNAL', 'HALFDAYAM', '2027-03-09', '2027-03-09', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('GraphQL API Design', 'EXTERNAL', 'HALFDAYPM', '2027-03-10', '2027-03-10', 'Centre B', 'Modern Web', 350.00),
    -- ('Enterprise Software Testing', 'INTERNAL', 'FULLDAY', '2027-03-11', '2027-03-12', 'NUS-ISS', 'NUS-ISS', 0.00),
    -- ('Certified Information Security Manager', 'PROFESSIONAL', 'FULLDAY', '2027-03-15', '2027-03-17', 'Centre C', 'ISACA', 1900.00),
    -- ('Quantum Computing Basics', 'EXTERNAL', 'FULLDAY', '2027-03-18', '2027-03-19', 'Centre A', 'Future Tech', 1300.00),
    -- ('Green IT and Sustainability', 'INTERNAL', 'HALFDAYAM', '2027-03-22', '2027-03-22', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Advanced TypeScript Development', 'EXTERNAL', 'FULLDAY', '2027-03-23', '2027-03-24', 'Centre B', 'JS Masters', 650.00);

-- insert dummy data into public holiday table for 2026
INSERT INTO holiday (holiday_date, holiday_name) VALUES
    -- ('2026-01-01', 'New Year''s Day'),
    -- ('2026-02-17', 'Chinese New Year Day1'),
    -- ('2026-02-18', 'Chinese New Year Day2'),
    -- ('2026-03-21', 'Hari Raya Puasa'),
    -- ('2026-04-03', 'Good Friday'),
    -- ('2026-05-01', 'Labour Day'),
    -- ('2026-05-27', 'Hari Raya Haji'),
    -- ('2026-05-31', 'Vesak Day'),
    -- ('2026-06-01', 'Vesak Day Additional PH'),
    -- ('2026-08-09', 'National Day'),
    -- ('2026-08-10', 'National Day Additional PH'),
    ('2026-11-08', 'Deepavali'),
    ('2026-11-09', 'Deepavali Additional PH'),
    ('2026-12-25', 'Christmas Day'),

    ('2027-01-01', 'New Year''s Day'),
    ('2027-02-06', 'Chinese New Year Day1'),
    ('2027-02-07', 'Chinese New Year Day2'),
    ('2027-02-08', 'Chinese New Year Additional PH'),
    ('2027-03-10', 'Hari Raya Puasa'),
    ('2027-03-26', 'Good Friday'),
    ('2027-05-01', 'Labour Day'),
    ('2027-05-17', 'Hari Raya Haji'),
    ('2027-05-20', 'Vesak Day'),
    ('2027-08-09', 'National Day'),
    ('2027-10-28', 'Deepavali'),
    ('2027-12-25', 'Christmas Day');
    
    
 -- Calendar demo applications: October 2026.
-- Resolve users and courses by their seeded names instead of fixed IDs.
INSERT INTO application (
    user_id,
    course_course_id,
    status,
    user_reason,
    manager_reason,
    has_been_paid
)
SELECT
    u.user_id,
    c.course_id,
    demo.application_status,
    'Develop skills relevant to my work.',
    CASE
        WHEN demo.application_status = 'APPROVED'
        THEN 'Approved for relevant staff development.'
        ELSE NULL
    END,
    false
FROM (
    SELECT
        'benny' AS username,
        'Java Programming' AS course_name,
        'APPROVED' AS application_status

    UNION ALL
    SELECT 'benny', 'Web Application Development', 'APPLIED'

    UNION ALL
    SELECT 'benny', 'Java PRO Certification', 'APPROVED'

    UNION ALL
    SELECT 'danny', 'Java Programming', 'APPROVED'

    UNION ALL
    SELECT 'danny', 'SQL Fundam', 'UPDATED'
) AS demo
JOIN user u ON u.username = demo.username
JOIN course c ON c.course_name = demo.course_name;

-- SQL seeding bypasses ApplicationService, so reserve the matching amounts.
-- Ben: SGD 600 and 5.5 training days reserved.
UPDATE user
SET budgeted_allowance = 9400.00,
    budgeted_days = 5.0
WHERE username = 'benny';

-- Dan: SGD 400 and 2.5 training days reserved.
UPDATE user
SET budgeted_allowance = 9100.00,
    budgeted_days = 7.5
WHERE username = 'danny';
