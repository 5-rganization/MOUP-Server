DROP SCHEMA IF EXISTS moup;
CREATE SCHEMA moup;
USE moup;

CREATE TABLE `users` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `provider` ENUM('LOGIN_GOOGLE', 'LOGIN_APPLE') NOT NULL,
    `provider_id` VARCHAR(100) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `nickname` VARCHAR(100) NOT NULL,
    `role` ENUM('ROLE_WORKER', 'ROLE_OWNER', 'ROLE_ADMIN') DEFAULT 'ROLE_WORKER' NOT NULL,
    `profile_img` VARCHAR(255),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    `deleted_at` TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0,
    UNIQUE KEY `unique_provider` (`provider`, `provider_id`)
);

-- 토큰 DB --
CREATE TABLE `social_tokens` (
    `id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `provider` VARCHAR(20) NOT NULL,
    `access_token` TEXT NULL,
    `refresh_token` TEXT NULL,
    `updated_at` TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES users(`id`) ON DELETE CASCADE
);

CREATE TABLE `user_tokens` (
    `id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `refresh_token` TEXT NOT NULL,
    `expiry_date` DATETIME NULL,
    `created_at` DATETIME NOT NULL
);

-- 루틴 DB --
CREATE TABLE `routines` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `routine_name` VARCHAR(100) NOT NULL,
    `alarm_time` TIME NULL,
	FOREIGN KEY(`user_id`) REFERENCES users(`id`) ON DELETE CASCADE
);

CREATE TABLE `routine_tasks` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `routine_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` VARCHAR(100) NOT NULL,
    `order_index` INT NOT NULL,
    FOREIGN KEY(`routine_id`) REFERENCES routines(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`user_id`) REFERENCES users(`id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_routine_order` (`routine_id`, `order_index`)
);
--

-- 캘린더 DB --
CREATE TABLE `calendars` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `calendar_name` VARCHAR(100) NOT NULL,
    `is_shared` TINYINT(1) DEFAULT 0 NOT NULL
);

CREATE TABLE `user_calendar_mappings` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `calendar_id` BIGINT NOT NULL,
    FOREIGN KEY(`user_id`) REFERENCES users(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`calendar_id`) REFERENCES calendars(`id`) ON DELETE CASCADE
);
--

-- 근무지 DB --
CREATE TABLE `workplace_category` (
	`id` INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `category_name` VARCHAR(100) NOT NULL
);

CREATE TABLE `workplaces` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `owner_id` BIGINT NULL,
    `workplace_name` VARCHAR(100) NOT NULL,
    `category_id` INT NULL,
    `salary_type` ENUM('SALARY_MONTHLY', 'SALARY_WEEKLY', 'SALARY_DAILY') NOT NULL,
    `salary_calculation` ENUM('SALARY_CALCULATION_HOURLY', 'SALARY_CALCULATION_FIXED') NOT NULL,
    `hourly_rate` INT NULL,
    `salary_date` INT CHECK (salary_date >= 1 AND salary_date <= 31) NULL,
    `salary_day` ENUM('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN') NULL,
    `has_national_pension` TINYINT(1) NOT NULL,
    `has_health_insurance` TINYINT(1) NOT NULL,
    `has_employment_insurance` TINYINT(1) NOT NULL,
    `has_industrial_accident` TINYINT(1) NOT NULL,
    `has_income_tax` TINYINT(1) NOT NULL,
    `has_night_allowance` TINYINT(1) NOT NULL,
    `label_color` VARCHAR(20) DEFAULT 'orange' NOT NULL,
    FOREIGN KEY(`owner_id`) REFERENCES users(`id`) ON DELETE SET NULL,
    FOREIGN KEY(`category_id`) REFERENCES workplace_category(`id`) ON DELETE SET NULL
);
--

CREATE TABLE `workers` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `workplace_id` BIGINT NOT NULL,
    FOREIGN KEY(`user_id`) REFERENCES users(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`workplace_id`) REFERENCES workplaces(`id`) ON DELETE CASCADE
);

CREATE TABLE `works` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `workplace_id` BIGINT NOT NULL,
    `work_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `rest_time` TIME NULL,
    `memo` VARCHAR(50) NULL,
    `daily_income` INT NULL,
    FOREIGN KEY(`workplace_id`) REFERENCES workplaces(`id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_workplace_date` (`workplace_id`, `work_date`)
);

CREATE TABLE `calendar_work_mapping` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `calendar_id` BIGINT NOT NULL,
    `work_id` BIGINT NOT NULL,
    `workplace_id` BIGINT NOT NULL,
    FOREIGN KEY(`calendar_id`) REFERENCES calendars(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`work_id`) REFERENCES works(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`workplace_id`) REFERENCES workplaces(`id`) ON DELETE CASCADE
);
