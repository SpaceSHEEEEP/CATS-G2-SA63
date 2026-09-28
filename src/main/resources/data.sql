-- insert dummy data into user table
INSERT INTO user (name, budget, username, password, email, is_active, is_admin) VALUES
    ('Bob', 10000, 'bobby', 'password', 'bob@gmail.com', true, false),
    ('Tim', 10000, 'timmy', 'password', 'tim@gmail.com', true, false),
    ('Ben', 10000, 'benny', 'password', 'ben@gmail.com', true, false),
    ('Dan', 9500, 'danny', 'password', 'dan@gmail.com', true, false),
    ('Sam', 11000, 'sammy', 'password', 'sam@gmail.com', true, false),
    ('Tom', 12000, 'tommy', 'password', 'tom@gmail.com', true, false),
    ('Ron', 8500, 'ronny', 'password', 'ron@gmail.com', true, false),
    ('Son', 10500, 'sonny', 'password', 'son@gmail.com', true, false),
    ('Ken', 10000, 'kenny', 'password', 'ken@gmail.com', true, false);

-- insert dummy data into course table
INSERT INTO course (course_name, course_type, start_date, end_date, location, training_provider, fee) VALUES 
    ('Java Programming', 'INTERNAL', '2026-10-01', '2026-10-05', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Web Application Development', 'INTERNAL', '2026-10-10', '2026-10-15', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Database Design', 'INTERNAL', '2026-11-01', '2026-11-05', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Cloud Computing Fundamentals', 'INTERNAL', '2026-11-10', '2026-11-12', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Software Quality Assurance', 'INTERNAL', '2026-12-01', '2026-12-04', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Enterprise Architecture', 'INTERNAL', '2026-12-10', '2026-12-14', 'NUS-ISS', 'NUS-ISS', 0.00),
    
    ('SQL Fundam', 'EXTERNAL', '2026-10-06', '2026-10-07','Centre A', 'A Training Provider', 400.00),

    ('Java PRO Certification', 'PROFESSIONAL','2026-10-19', '2026-10-20','Centre B', 'B Cert Provider', 600.00);
