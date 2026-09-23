-- CampusConnect schema. MySQL 5.7.
-- Applied by hand on each customer box. There is no migration tool.
-- XXX CC-1280: RIVERTON's production schema has two extra indexes that are not
-- in this file. Nobody knows who added them.

CREATE DATABASE IF NOT EXISTS campusconnect
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE campusconnect;

-- Student Finance tables first: they hold the foreign keys into student.
DROP TABLE IF EXISTS financial_hold;
DROP TABLE IF EXISTS account_payment;
DROP TABLE IF EXISTS account_charge;
DROP TABLE IF EXISTS student_account;
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

-- ---------------------------------------------------------------------------
-- Student Finance / Billing. Added 2016 "for one term". Same database, same
-- schema, same customer_code column, and hung off the SAME student row by
-- foreign key - there is no billing-side person record.
-- ---------------------------------------------------------------------------

CREATE TABLE student_account (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  customer_code   VARCHAR(20)   NOT NULL,
  student_id      BIGINT,
  term_code       VARCHAR(20),
  balance         DECIMAL(12,2) DEFAULT 0.00,
  status          VARCHAR(20),
  last_charge_at  DATETIME,
  last_payment_at DATETIME,
  recomputed_at   DATETIME,
  created_at      DATETIME,
  updated_at      DATETIME,
  PRIMARY KEY (id),
  -- TODO CC-1412: this should be UNIQUE (customer_code, student_id). It is not,
  -- and the nightly reconcile has opened a second account at SUMMIT twice.
  KEY ix_acct_customer (customer_code),
  KEY ix_acct_student (student_id),
  CONSTRAINT fk_acct_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE account_charge (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  customer_code VARCHAR(20)   NOT NULL,
  account_id    BIGINT,
  -- Denormalised copy of student_account.student_id, added to speed up the
  -- nightly exposure report. Nothing keeps the two columns in step.
  student_id    BIGINT,
  charge_type   VARCHAR(20),
  amount        DECIMAL(12,2),
  term_code     VARCHAR(20),
  description   VARCHAR(200),
  source_ref    VARCHAR(60),
  posted_at     DATETIME,
  PRIMARY KEY (id),
  KEY ix_charge_account (account_id),
  KEY ix_charge_customer_term (customer_code, term_code),
  CONSTRAINT fk_charge_account FOREIGN KEY (account_id) REFERENCES student_account (id),
  CONSTRAINT fk_charge_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE account_payment (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  customer_code  VARCHAR(20)   NOT NULL,
  account_id     BIGINT,
  amount         DECIMAL(12,2),
  payment_method VARCHAR(20),
  reference_no   VARCHAR(60),
  received_by    VARCHAR(120),
  posted_at      DATETIME,
  PRIMARY KEY (id),
  KEY ix_pay_account (account_id),
  CONSTRAINT fk_pay_account FOREIGN KEY (account_id) REFERENCES student_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE financial_hold (
  id                   BIGINT        NOT NULL AUTO_INCREMENT,
  customer_code        VARCHAR(20)   NOT NULL,
  account_id           BIGINT,
  student_id           BIGINT,
  reason_code          VARCHAR(40),
  threshold_amount     DECIMAL(12,2),
  balance_at_placement DECIMAL(12,2),
  placed_by            VARCHAR(120),
  notes                VARCHAR(4000),
  placed_at            DATETIME,
  -- "Active" is released_at IS NULL, spelled out in HQL, in raw SQL and once
  -- in a JSP. A status column would have been one spelling.
  released_at          DATETIME,
  PRIMARY KEY (id),
  KEY ix_hold_student (student_id),
  KEY ix_hold_customer_active (customer_code, released_at),
  CONSTRAINT fk_hold_account FOREIGN KEY (account_id) REFERENCES student_account (id),
  CONSTRAINT fk_hold_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
