DROP SCHEMA IF EXISTS moup;
CREATE SCHEMA moup;
USE moup;

CREATE TABLE `users`
(
    `id`          BIGINT AUTO_INCREMENT                                              NOT NULL PRIMARY KEY,
    `provider`    ENUM ('LOGIN_GOOGLE', 'LOGIN_APPLE', 'LOGIN_NAVER', 'LOGIN_KAKAO') NOT NULL,
    `provider_id` VARCHAR(100)                                                       NOT NULL,
    `username`    VARCHAR(20),
    `nickname`    VARCHAR(20),
    `role`        ENUM ('ROLE_WORKER', 'ROLE_OWNER', 'ROLE_ADMIN')                   DEFAULT 'ROLE_WORKER',
    `profile_img` VARCHAR(255),
    `created_at`  TIMESTAMP                                                          DEFAULT CURRENT_TIMESTAMP(),
    `deleted_at`  TIMESTAMP,
    `is_deleted`  TINYINT(1)                                                         DEFAULT 0,
    `fcm_token`   TEXT,
    -- TODO: 사용자 당 생성할 수 있는 근무지 20개로 제한
    UNIQUE KEY `unique_provider` (`provider`, `provider_id`)
);

-- 토큰 DB --
CREATE TABLE `social_tokens`
(
    `id`            BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id`       BIGINT                NOT NULL,
    `refresh_token` TEXT                  NULL,
    `updated_at`    TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES users (`id`) ON DELETE CASCADE
);

CREATE TABLE `user_tokens`
(
    `id`            BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id`       BIGINT                NOT NULL,
    `refresh_token` TEXT                  NOT NULL,
    `expiry_date`   DATETIME              NULL,
    `created_at`    DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

-- 루틴 DB --
CREATE TABLE `routines`
(
    `id`           BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id`      BIGINT                NOT NULL,
    `routine_name` VARCHAR(20)           NOT NULL,
    `alarm_time`   TIME                  NULL,
    FOREIGN KEY (`user_id`) REFERENCES users (`id`) ON DELETE CASCADE
);

CREATE TABLE `routine_tasks`
(
    `id`          BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `routine_id`  BIGINT                NOT NULL,
    `content`     VARCHAR(30)           NOT NULL,
    `order_index` INT                   NOT NULL,
    FOREIGN KEY (`routine_id`) REFERENCES routines (`id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_routine_order` (`routine_id`, `order_index`)
);

CREATE TABLE `normal_alarms`
(
    `id`          BIGINT AUTO_INCREMENT                                                                             NOT NULL PRIMARY KEY,
    `sender_id`   BIGINT                                                                                            NOT NULL,
    `receiver_id` BIGINT                                                                                            NOT NULL,
    `title`       TEXT                                                                                              NOT NULL,
    `content`     TEXT                                                                                              NULL,
    `sent_at`     DATETIME                                                                                          NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `read_at`     DATETIME                                                                                          NULL
);

CREATE TABLE `admin_alarms`
(
    `id`         BIGINT AUTO_INCREMENT       NOT NULL PRIMARY KEY,
    `title`      TEXT                        NOT NULL,
    `content`    TEXT                        NULL,
    `sent_at`    DATETIME                    NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE `admin_alarm_user_mappings` (
    `id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `alarm_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `read_at` DATETIME NULL,
    `deleted_at` DATETIME NULL,
    FOREIGN KEY (`alarm_id`) REFERENCES admin_alarms(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES users(`id`) ON DELETE CASCADE
);
--

-- 근무지 DB --
CREATE TABLE `workplaces`
(
    `id`             BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `owner_id`       BIGINT                NULL,
    `workplace_name` VARCHAR(50)           NOT NULL,
    `category_name`  VARCHAR(10)           NOT NULL,
    `is_shared`      TINYINT(1) DEFAULT 0  NOT NULL,
    `address`        VARCHAR(100)          NULL,
    `latitude`       DECIMAL(9, 6)         NULL,
    `longitude`      DECIMAL(9, 6)         NULL,
    FOREIGN KEY (`owner_id`) REFERENCES users (`id`) ON DELETE CASCADE
);

CREATE TABLE `workers`
(
    `id`                       BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id`                  BIGINT                NULL,
    `workplace_id`             BIGINT                NOT NULL,
    `worker_based_label_color` VARCHAR(10)           NULL,
    `owner_based_label_color`  VARCHAR(10)           NULL,
    `is_accepted`              TINYINT(1)            NULL,
    FOREIGN KEY (`user_id`) REFERENCES users (`id`) ON DELETE SET NULL,
    FOREIGN KEY (`workplace_id`) REFERENCES workplaces (`id`) ON DELETE CASCADE
);

CREATE TABLE `works`
(
    `id`                      BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `worker_id`               BIGINT                NOT NULL,
    `work_date`               DATE                  NOT NULL,
    `start_time`              DATETIME              NOT NULL,
    `actual_start_time`       DATETIME              NULL,
    `end_time`                DATETIME              NOT NULL,
    `actual_end_time`         DATETIME              NULL,
    `rest_time_minutes`       INT                   DEFAULT 0,
    `memo`                    VARCHAR(200)          NULL,
    `hourly_rate`             INT                   NULL,
    `base_pay`                INT                   DEFAULT 0,  -- 기본급 (휴게시간 제외)
    `night_allowance`         INT                   DEFAULT 0,  -- 야간수당
    `holiday_allowance`       INT                   DEFAULT 0,  -- 주휴수당 (해당 주에 발생한 수당을 N등분하여 일별로 저장)
    `gross_income`            INT                   DEFAULT 0,  -- 세전 일급 (위 4가지의 합)
    `estimated_net_income`    INT                   DEFAULT 0,  -- 추정 세후 일급 (캘린더 표시용)
    `repeat_days`             VARCHAR(100)          NULL,
    `repeat_end_date`         DATETIME              NULL,
    FOREIGN KEY (`worker_id`) REFERENCES workers (`id`) ON DELETE CASCADE
);

CREATE TABLE `monthly_salaries`
(
    `id`                      BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `worker_id`               BIGINT                NOT NULL,
    `salary_month`            VARCHAR(10)           NOT NULL,  -- "yyyy-MM" 형식
    `gross_income`            INT                   NOT NULL,  -- 세전 총소득
    `national_pension`        INT                   NOT NULL,  -- 국민연금
    `health_insurance`        INT                   NOT NULL,  -- 건강보험
    `employment_insurance`    INT                   NOT NULL,  -- 고용보험
    `income_tax`              INT                   NOT NULL,  -- 소득세
    `local_income_tax`        INT                   NOT NULL,  -- 지방소득세
    `net_income`              INT                   NOT NULL,  -- 세후 실지급액
    FOREIGN KEY (`worker_id`) REFERENCES workers (`id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_worker_month` (`worker_id`, `salary_month`)
);

CREATE TABLE `work_routine_mappings`
(
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `work_id`    BIGINT NOT NULL,
    `routine_id` BIGINT NOT NULL,
    FOREIGN KEY (`work_id`) REFERENCES works (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`routine_id`) REFERENCES routines (`id`) ON DELETE CASCADE
);

CREATE TABLE `salaries`
(
    `id`                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    `worker_id`                BIGINT                                                                              NOT NULL,
    `salary_type`              ENUM ('SALARY_MONTHLY', 'SALARY_WEEKLY', 'SALARY_DAILY')                            NOT NULL,
    `salary_calculation`       ENUM ('SALARY_CALCULATION_HOURLY', 'SALARY_CALCULATION_FIXED')                      NOT NULL,
    `hourly_rate`              INT                                                                                 NULL,
    `fixed_rate`               INT                                                                                 NULL,
    `salary_date`              INT CHECK (salary_date >= 1 AND salary_date <= 31)                                  NULL,
    `salary_day`               ENUM ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NULL,
    `has_national_pension`     TINYINT(1)                                                                          NOT NULL,
    `has_health_insurance`     TINYINT(1)                                                                          NOT NULL,
    `has_employment_insurance` TINYINT(1)                                                                          NOT NULL,
    `has_industrial_accident`  TINYINT(1)                                                                          NOT NULL,
    `has_income_tax`           TINYINT(1)                                                                          NOT NULL,
    `has_night_allowance`      TINYINT(1)                                                                          NOT NULL,
    FOREIGN KEY (`worker_id`) REFERENCES workers (`id`) ON DELETE CASCADE
);
--
