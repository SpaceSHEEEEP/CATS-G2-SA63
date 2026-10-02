-- insert dummy data into user table
INSERT INTO user (name, budget, days, username, password, email, is_active, is_admin) VALUES
    ('Bob', 10000, 10.0, 'bobby', 'pw', 'bob@gmail.com', true, false),
    ('Tim', 10000, 15.0, 'timmy', 'pw', 'tim@gmail.com', true, false),
    ('Ben', 10000, 10.5, 'benny', 'pw', 'ben@gmail.com', true, false),
    ('Dan', 9500, 10.0, 'danny', 'pw', 'dan@gmail.com', true, false),
    ('Sam', 11000, 10.0, 'sammy', 'pw', 'sam@gmail.com', true, false),
    ('Tom', 12000, 10.0, 'tommy', 'pw', 'tom@gmail.com', true, false),
    ('Ron', 8500, 10.0, 'ronny', 'pw', 'ron@gmail.com', true, false),
    ('Son', 10500, 10.0, 'sonny', 'pw', 'son@gmail.com', true, false),
    ('Ken', 10000, 10.0, 'kenny', 'pw', 'ken@gmail.com', true, false);
    
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
    ('Java Programming', 'INTERNAL', 'HALFDAYAM', '2026-10-05', '2026-10-05', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Web Application Development', 'INTERNAL', 'FULLDAY', '2026-10-12', '2026-10-14', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Database Design', 'INTERNAL', 'HALFDAYPM', '2026-11-02', '2026-11-02', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Cloud Computing Fundamentals', 'INTERNAL', 'FULLDAY', '2026-11-10', '2026-11-11', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Software Quality Assurance', 'INTERNAL', 'HALFDAYAM', '2026-12-01', '2026-12-01', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('Enterprise Architecture', 'INTERNAL', 'FULLDAY', '2026-12-10', '2026-12-14', 'NUS-ISS', 'NUS-ISS', 0.00),
    ('SQL Fundam', 'EXTERNAL', 'FULLDAY', '2026-10-06', '2026-10-07', 'Centre A', 'A Training Provider', 400.00),
    ('Java PRO Certification', 'PROFESSIONAL', 'FULLDAY', '2026-10-19', '2026-10-20', 'Centre B', 'B Cert Provider', 600.00);
-- insert dummy data into public holiday table
INSERT INTO holiday (holiday_date, holiday_name) VALUES
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
 
