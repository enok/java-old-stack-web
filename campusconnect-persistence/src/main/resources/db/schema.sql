-- CampusConnect schema. MySQL 5.7.
-- Applied by hand on each customer box. There is no migration tool.
-- XXX CC-1280: RIVERTON's production schema has two extra indexes that are not
-- in this file. Nobody knows who added them.

CREATE DATABASE IF NOT EXISTS campusconnect
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE campusconnect;

DROP TABLE IF EXISTS enrollment;
DROP TABLE IF EXISTS early_alert_case;
DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS advisor;

CREATE TABLE advisor (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  customer_code   VARCHAR(20)  NOT NULL,
  staff_id        VARCHAR(40),
  first_name      VARCHAR(80),
  last_name       VARCHAR(80),
  email           VARCHAR(160),
  department      VARCHAR(80),
  active          TINYINT(1)   DEFAULT 1,
  max_caseload    INT          DEFAULT 300,
  PRIMARY KEY (id),
  KEY ix_advisor_customer (customer_code),
  KEY ix_advisor_staff (customer_code, staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE student (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  customer_code     VARCHAR(20)  NOT NULL,
  sis_id            VARCHAR(40)  NOT NULL,
  first_name        VARCHAR(80),
  last_name         VARCHAR(80),
  email             VARCHAR(160),
  phone             VARCHAR(40),
  gpa               DOUBLE,
  credits_completed INT,
  enrollment_status VARCHAR(20),
  program_code      VARCHAR(40),
  -- RIVERTON only
  athletics_code    VARCHAR(20),
  -- SUMMIT only
  home_campus       VARCHAR(40),
  birth_date        DATE,
  advisor_id        BIGINT,
  created_at        DATETIME,
  updated_at        DATETIME,
  PRIMARY KEY (id),
  -- TODO CC-1281: this should be UNIQUE (customer_code, sis_id). It is not,
  -- and the nightly import has created duplicates at SUMMIT twice.
  KEY ix_student_sis (customer_code, sis_id),
  KEY ix_student_advisor (advisor_id),
  CONSTRAINT fk_student_advisor FOREIGN KEY (advisor_id) REFERENCES advisor (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE course (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  customer_code VARCHAR(20)  NOT NULL,
  course_code   VARCHAR(40),
  title         VARCHAR(200),
  credits       INT,
  term_code     VARCHAR(20),
  department    VARCHAR(80),
  PRIMARY KEY (id),
  KEY ix_course_customer_term (customer_code, term_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE appointment (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  customer_code    VARCHAR(20)  NOT NULL,
  student_id       BIGINT,
  advisor_id       BIGINT,
  starts_at        DATETIME,
  duration_minutes INT,
  status           VARCHAR(20),
  reason_code      VARCHAR(40),
  location         VARCHAR(120),
  notes            VARCHAR(4000),
  walk_in          TINYINT(1)   DEFAULT 0,
  created_at       DATETIME,
  PRIMARY KEY (id),
  KEY ix_appt_advisor_day (customer_code, advisor_id, starts_at),
  KEY ix_appt_student (student_id),
  CONSTRAINT fk_appt_student FOREIGN KEY (student_id) REFERENCES student (id),
  CONSTRAINT fk_appt_advisor FOREIGN KEY (advisor_id) REFERENCES advisor (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE early_alert_case (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  customer_code VARCHAR(20)  NOT NULL,
  student_id    BIGINT,
  advisor_id    BIGINT,
  reason        VARCHAR(200),
  severity      VARCHAR(20),
  status        VARCHAR(20),
  raised_by     VARCHAR(120),
  opened_at     DATETIME,
  closed_at     DATETIME,
  outcome_notes VARCHAR(4000),
  PRIMARY KEY (id),
  KEY ix_case_status (customer_code, status),
  CONSTRAINT fk_case_student FOREIGN KEY (student_id) REFERENCES student (id),
  CONSTRAINT fk_case_advisor FOREIGN KEY (advisor_id) REFERENCES advisor (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE enrollment (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  customer_code VARCHAR(20)  NOT NULL,
  student_id    BIGINT,
  course_id     BIGINT,
  term_code     VARCHAR(20),
  grade         VARCHAR(4),
  credits       INT,
  status        VARCHAR(4),
  enrolled_at   DATETIME,
  PRIMARY KEY (id),
  KEY ix_enr_student (student_id),
  KEY ix_enr_course (course_id)
  -- No foreign keys here on purpose: JdbcEnrollmentDao writes rows outside any
  -- transaction and the constraints were dropped in 2016 to stop the nightly
  -- job failing. See docs/LEGACY-SMELLS.md #7.
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
