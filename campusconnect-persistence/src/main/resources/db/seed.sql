-- Synthetic seed data. Every name, email and id below is invented.
USE campusconnect;

INSERT INTO advisor (customer_code, staff_id, first_name, last_name, email, department, active, max_caseload) VALUES
 ('NORTHLAKE','NL-1001','Dana','Whitfield','dwhitfield@northlake.example.edu','Arts & Sciences',1,300),
 ('NORTHLAKE','NL-1002','Marcus','Oyelaran','moyelaran@northlake.example.edu','Engineering',1,250),
 ('RIVERTON','RVT-2201','Priya','Raghavan','praghavan@riverton.example.edu','Student Success',1,400),
 ('RIVERTON','RVT-2202','Tom','Beaulieu','tbeaulieu@riverton.example.edu','Athletics Advising',1,120),
 ('SUMMIT','SUM-31','Alicia','Ferreira','aferreira@summit.example.edu','Counseling',1,180),
 ('SUMMIT','SUM-32','Wen','Zhao','wzhao@summit.example.edu','Transfer Services',1,180);

INSERT INTO student (customer_code, sis_id, first_name, last_name, email, phone, gpa, credits_completed,
                     enrollment_status, program_code, athletics_code, home_campus, birth_date, advisor_id,
                     created_at, updated_at) VALUES
 ('NORTHLAKE','NL-S-4401','Hannah','Iverson','hiverson@northlake.example.edu','555-0110',3.42,62,'ACTIVE','BA-HIST',NULL,NULL,'1996-03-14',1,NOW(),NOW()),
 ('NORTHLAKE','NL-S-4402','Omar','Haddad','ohaddad@northlake.example.edu','555-0111',1.88,28,'ACTIVE','BS-ME',NULL,NULL,'1997-11-02',2,NOW(),NOW()),
 ('NORTHLAKE','NL-S-4403','Grace','Lindqvist','glindqvist@northlake.example.edu','555-0112',2.05,44,'PROBATION','BA-PSY',NULL,NULL,'1996-07-29',1,NOW(),NOW()),
 ('RIVERTON','RVT-S-88120','Devon','Marsh','dmarsh@riverton.example.edu','555-0220',2.11,51,'ACTIVE','BS-KIN','FB-01',NULL,'1998-01-09',3,NOW(),NOW()),
 ('RIVERTON','RVT-S-88121','Sofia','Kalinina','skalinina@riverton.example.edu','555-0221',3.90,95,'ACTIVE','BS-CS',NULL,NULL,'1995-05-23',3,NOW(),NOW()),
 ('RIVERTON','RVT-S-88122','Ethan','Boateng','eboateng@riverton.example.edu','555-0222',1.72,19,'PROBATION','BA-COM','BB-07',NULL,'1999-09-30',4,NOW(),NOW()),
 ('SUMMIT','SUM-S-701','Rosa','Mendez','rmendez@summit.example.edu','555-0330',3.15,33,'ACTIVE','AA-GEN',NULL,'MAIN','1999-02-17',5,NOW(),NOW()),
 ('SUMMIT','SUM-S-702','Kyle','Tarrant','ktarrant@summit.example.edu','555-0331',1.60,12,'PROBATION','AS-NUR',NULL,'WEST','2000-08-05',5,NOW(),NOW()),
 ('SUMMIT','SUM-S-703','Ingrid','Halvorsen','ihalvorsen@summit.example.edu','555-0332',2.44,27,'ACTIVE','AA-BUS',NULL,'ONLINE','1998-12-11',6,NOW(),NOW());

INSERT INTO course (customer_code, course_code, title, credits, term_code, department) VALUES
 ('NORTHLAKE','HIST-210','Modern European History',3,'FA2014','History'),
 ('NORTHLAKE','ME-140','Statics',4,'FA2014','Mechanical Engineering'),
 ('RIVERTON','KIN-101','Introduction to Kinesiology',3,'FA2014','Kinesiology'),
 ('RIVERTON','CS-330','Database Systems',4,'FA2014','Computer Science'),
 ('SUMMIT','NUR-110','Foundations of Nursing',4,'FA2014','Nursing'),
 ('SUMMIT','BUS-101','Principles of Business',3,'FA2014','Business');

INSERT INTO enrollment (customer_code, student_id, course_id, term_code, grade, credits, status, enrolled_at) VALUES
 ('NORTHLAKE',1,1,'FA2014','B+',3,'E',NOW()),
 ('NORTHLAKE',2,2,'FA2014','D',4,'E',NOW()),
 ('NORTHLAKE',3,1,'FA2014',NULL,3,'W',NOW()),
 ('RIVERTON',4,3,'FA2014','C-',3,'E',NOW()),
 ('RIVERTON',5,4,'FA2014','A',4,'E',NOW()),
 ('RIVERTON',6,3,'FA2014',NULL,3,'W',NOW()),
 ('SUMMIT',7,6,'FA2014','B',3,'E',NOW()),
 ('SUMMIT',8,5,'FA2014','F',4,'E',NOW()),
 ('SUMMIT',9,6,'FA2014','C+',3,'E',NOW());

INSERT INTO appointment (customer_code, student_id, advisor_id, starts_at, duration_minutes, status,
                         reason_code, location, notes, walk_in, created_at) VALUES
 ('NORTHLAKE',1,1,'2014-10-06 09:00:00',30,'COMPLETED','MAJOR_CHANGE','Wilson Hall 204','Discussed minor options.',0,NOW()),
 ('NORTHLAKE',2,2,'2014-10-06 10:30:00',30,'NO_SHOW','ACADEMIC_STANDING','Wilson Hall 204',NULL,0,NOW()),
 ('RIVERTON',4,3,'2014-10-07 13:00:00',45,'SCHEDULED','EARLY_ALERT','Student Success Center',NULL,0,NOW()),
 ('RIVERTON',6,4,'2014-10-08 15:00:00',45,'SCHEDULED','ATHLETICS_REVIEW','Athletics Advising',NULL,0,NOW()),
 ('SUMMIT',7,5,'2014-10-06 11:20:00',20,'COMPLETED','REGISTRATION','Counseling Center','Registered for spring.',1,NOW()),
 ('SUMMIT',8,5,'2014-10-09 14:00:00',20,'SCHEDULED','EARLY_ALERT','Counseling Center',NULL,1,NOW());

INSERT INTO early_alert_case (customer_code, student_id, advisor_id, reason, severity, status, raised_by,
                              opened_at, closed_at, outcome_notes) VALUES
 ('NORTHLAKE',2,2,'Midterm grade below D+','HIGH','OPEN','system','2014-10-03 02:10:00',NULL,NULL),
 ('NORTHLAKE',3,1,'Attendance below 70%','MEDIUM','IN_PROGRESS','dwhitfield','2014-09-29 02:10:00',NULL,NULL),
 ('RIVERTON',4,3,'GPA below 2.25 threshold','HIGH','ESCALATED','system','2014-09-20 02:10:00',NULL,NULL),
 ('RIVERTON',6,4,'GPA below 2.25 threshold','HIGH','OPEN','system','2014-10-01 02:10:00',NULL,NULL),
 ('SUMMIT',8,5,'Failing NUR-110','HIGH','OPEN','system','2014-10-02 02:10:00',NULL,NULL),
 -- Closing this one posts a late-registration fee to the student's finance
 -- account in the same transaction. See EarlyAlertService.closeCase().
 ('NORTHLAKE',1,1,'Late registration, no term schedule on file','MEDIUM','OPEN','dwhitfield','2014-10-05 02:10:00',NULL,NULL);

-- ---------------------------------------------------------------------------
-- Student Finance / Billing seed. One account per student, hung off the SAME
-- student ids the advising rows above use. The balances are the ledger totals:
-- sum(account_charge.amount) - sum(account_payment.amount).
-- ---------------------------------------------------------------------------

INSERT INTO student_account (customer_code, student_id, term_code, balance, status,
                             last_charge_at, last_payment_at, recomputed_at, created_at, updated_at) VALUES
 ('NORTHLAKE',1,'FA2014',   0.00,'OPEN',    '2014-08-25 06:00:00','2014-09-04 11:12:00',NOW(),NOW(),NOW()),
 ('NORTHLAKE',2,'FA2014',1240.00,'PAST_DUE','2014-10-01 02:10:00','2014-09-08 09:40:00',NOW(),NOW(),NOW()),
 ('NORTHLAKE',3,'FA2014', 310.00,'PAST_DUE','2014-08-25 06:00:00','2014-09-19 14:05:00',NOW(),NOW(),NOW()),
 ('RIVERTON', 4,'FA2014',1672.00,'PAST_DUE','2014-09-30 02:10:00',NULL,                 NOW(),NOW(),NOW()),
 ('RIVERTON', 5,'FA2014',   0.00,'OPEN',    '2014-08-20 06:00:00','2014-08-29 10:00:00',NOW(),NOW(),NOW()),
 ('RIVERTON', 6,'FA2014',2090.00,'PAST_DUE','2014-09-30 02:10:00',NULL,                 NOW(),NOW(),NOW()),
 ('SUMMIT',   7,'FA2014', 142.00,'PAST_DUE','2014-08-28 06:00:00','2014-09-11 13:30:00',NOW(),NOW(),NOW()),
 ('SUMMIT',   8,'FA2014', 568.00,'PAST_DUE','2014-10-02 02:10:00','2014-09-15 08:55:00',NOW(),NOW(),NOW()),
 ('SUMMIT',   9,'FA2014',   0.00,'OPEN',    '2014-08-28 06:00:00','2014-09-02 16:20:00',NOW(),NOW(),NOW());

INSERT INTO account_charge (customer_code, account_id, student_id, charge_type, amount, term_code,
                            description, source_ref, posted_at) VALUES
 ('NORTHLAKE',1,1,'TUITION', 930.00,'FA2014','Tuition, 3 credits at 310.00','SIS-FA2014','2014-08-25 06:00:00'),
 ('NORTHLAKE',1,1,'FEE',      65.00,'FA2014','Student activity fee','SIS-FA2014','2014-08-25 06:00:00'),
 ('NORTHLAKE',2,2,'TUITION',1240.00,'FA2014','Tuition, 4 credits at 310.00','SIS-FA2014','2014-08-25 06:00:00'),
 ('NORTHLAKE',2,2,'FEE',      65.00,'FA2014','Student activity fee','SIS-FA2014','2014-08-25 06:00:00'),
 ('NORTHLAKE',2,2,'LATE_FEE', 25.00,'FA2014','Late payment fee, nightly reconcile','NIGHTLY','2014-10-01 02:10:00'),
 ('NORTHLAKE',3,3,'TUITION', 930.00,'FA2014','Tuition, 3 credits at 310.00','SIS-FA2014','2014-08-25 06:00:00'),
 ('NORTHLAKE',3,3,'FEE',      65.00,'FA2014','Student activity fee','SIS-FA2014','2014-08-25 06:00:00'),
 ('RIVERTON', 4,4,'TUITION',1254.00,'FA2014','Tuition, 3 credits at 418.00','SIS-FA2014','2014-08-20 06:00:00'),
 ('RIVERTON', 4,4,'FEE',     118.00,'FA2014','Health and recreation fee','SIS-FA2014','2014-08-20 06:00:00'),
 ('RIVERTON', 4,4,'LATE_FEE',300.00,'FA2014','Late payment fee, 1.5 percent of balance','NIGHTLY','2014-09-30 02:10:00'),
 ('RIVERTON', 5,5,'TUITION',1672.00,'FA2014','Tuition, 4 credits at 418.00','SIS-FA2014','2014-08-20 06:00:00'),
 ('RIVERTON', 5,5,'FEE',     118.00,'FA2014','Health and recreation fee','SIS-FA2014','2014-08-20 06:00:00'),
 ('RIVERTON', 6,6,'TUITION',1254.00,'FA2014','Tuition, 3 credits at 418.00','SIS-FA2014','2014-08-20 06:00:00'),
 ('RIVERTON', 6,6,'FEE',     118.00,'FA2014','Health and recreation fee','SIS-FA2014','2014-08-20 06:00:00'),
 ('RIVERTON', 6,6,'LATE_FEE',718.00,'FA2014','Late payment fee, 1.5 percent of balance','NIGHTLY','2014-09-30 02:10:00'),
 ('SUMMIT',   7,7,'TUITION', 426.00,'FA2014','Tuition, 3 credits at 142.00','SIS-FA2014','2014-08-28 06:00:00'),
 ('SUMMIT',   7,7,'FEE',      40.00,'FA2014','Technology fee','SIS-FA2014','2014-08-28 06:00:00'),
 ('SUMMIT',   8,8,'TUITION', 568.00,'FA2014','Tuition, 4 credits at 142.00','SIS-FA2014','2014-08-28 06:00:00'),
 ('SUMMIT',   8,8,'FEE',      40.00,'FA2014','Technology fee','SIS-FA2014','2014-08-28 06:00:00'),
 ('SUMMIT',   8,8,'LATE_FEE', 10.00,'FA2014','Late payment fee, capped at Summit','NIGHTLY','2014-10-02 02:10:00'),
 ('SUMMIT',   9,9,'TUITION', 426.00,'FA2014','Tuition, 3 credits at 142.00','SIS-FA2014','2014-08-28 06:00:00'),
 ('SUMMIT',   9,9,'FEE',      40.00,'FA2014','Technology fee','SIS-FA2014','2014-08-28 06:00:00');

INSERT INTO account_payment (customer_code, account_id, amount, payment_method, reference_no,
                             received_by, posted_at) VALUES
 ('NORTHLAKE',1, 995.00,'CHECK','NL-CHK-10041','bursar','2014-09-04 11:12:00'),
 ('NORTHLAKE',2,  90.00,'CASH', 'NL-CSH-10088','bursar','2014-09-08 09:40:00'),
 ('NORTHLAKE',3, 685.00,'AID',  'NL-AID-22107','system','2014-09-19 14:05:00'),
 ('RIVERTON', 5,1790.00,'AID',  'RVT-AID-88120','system','2014-08-29 10:00:00'),
 ('SUMMIT',   7, 324.00,'CARD', 'SUM-CRD-4417','counter','2014-09-11 13:30:00'),
 ('SUMMIT',   8,  50.00,'CASH', 'SUM-CSH-4420','counter','2014-09-15 08:55:00'),
 ('SUMMIT',   9, 466.00,'CHECK','SUM-CHK-4431','counter','2014-09-02 16:20:00');

-- Holds. Note student 4 (Devon Marsh) is a Riverton athlete: the hold row
-- exists, but billing.athleteHoldExempt=true means it does NOT block his
-- advising appointments. Student 3's hold was released by hand in September.
INSERT INTO financial_hold (customer_code, account_id, student_id, reason_code, threshold_amount,
                            balance_at_placement, placed_by, notes, placed_at, released_at) VALUES
 ('NORTHLAKE',2,2,'BALANCE_OVER_THRESHOLD', 500.00,1240.00,'system',NULL,'2014-10-01 02:11:00',NULL),
 ('NORTHLAKE',3,3,'BALANCE_OVER_THRESHOLD', 500.00, 995.00,'system','Aid disbursed, released','2014-09-01 02:11:00','2014-09-19 14:06:00'),
 ('RIVERTON', 4,4,'BALANCE_OVER_THRESHOLD',1000.00,1672.00,'system',NULL,'2014-09-30 02:11:00',NULL),
 ('RIVERTON', 6,6,'BALANCE_OVER_THRESHOLD',1000.00,2090.00,'system',NULL,'2014-09-30 02:11:00',NULL),
 ('SUMMIT',   8,8,'BALANCE_OVER_THRESHOLD', 250.00, 568.00,'system',NULL,'2014-10-02 02:11:00',NULL);
