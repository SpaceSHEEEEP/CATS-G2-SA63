-- Tests ONLY: transient synthetic records for repository assertions. Never installed in production.
INSERT INTO user(user_id,name,username,password,email,role,designation,active,is_admin,
    actual_allowance,budgeted_allowance,actual_days,budgeted_days,manager_user_id) VALUES
 (1,'Fixture Manager','bobby','$2a$10$XX.htgpTqjfQG5DH0kCA5OtqbYh7t1pxWI3hkXChs8Qu9/LCUGDjy',
    'manager@example.invalid','ROLE_MANAGER','PROFESSIONAL',true,false,2000,2000,10,10,NULL),
 (2,'Fixture Team Lead','timmy','$2a$10$XX.htgpTqjfQG5DH0kCA5OtqbYh7t1pxWI3hkXChs8Qu9/LCUGDjy',
    'lead@example.invalid','ROLE_MANAGER','PROFESSIONAL',true,false,2000,2000,10,10,1),
 (3,'Fixture Ben','benny','$2a$10$XX.htgpTqjfQG5DH0kCA5OtqbYh7t1pxWI3hkXChs8Qu9/LCUGDjy',
    'employee1@example.invalid','ROLE_EMPLOYEE','PROFESSIONAL',true,false,2000,2000,10,10,2),
 (4,'Fixture Dan','danny','$2a$10$XX.htgpTqjfQG5DH0kCA5OtqbYh7t1pxWI3hkXChs8Qu9/LCUGDjy',
    'employee2@example.invalid','ROLE_EMPLOYEE','PROFESSIONAL',true,false,2000,2000,10,10,2),
 (5,'Fixture Solo','solo','$2a$10$XX.htgpTqjfQG5DH0kCA5OtqbYh7t1pxWI3hkXChs8Qu9/LCUGDjy',
    'employee3@example.invalid','ROLE_EMPLOYEE','PROFESSIONAL',true,false,2000,2000,10,10,NULL);
INSERT INTO training_provider(id,name,enabled) VALUES (1,'Fixture Provider',true);
INSERT INTO course(course_name,course_type,duration,start_date,end_date,
    location,training_provider,provider_id,fee,archived)
 VALUES ('Fixture Internal Course','INTERNAL','FULLDAY','2035-03-20','2035-03-21',
    'Training room','Fixture Provider',1,0,false);
INSERT INTO holiday(holiday_id,holiday_date,holiday_name) VALUES
 (1,'2027-10-28','Fixture holiday');
