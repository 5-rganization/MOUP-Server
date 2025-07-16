DROP SCHEMA IF EXISTS moup;
CREATE SCHEMA moup;
USE moup;

CREATE TABLE `users` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `provider` ENUM('LOGIN_GOOGLE', 'LOGIN_APPLE') NOT NULL,
    `provider_id` VARCHAR(100) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `role` ENUM('ROLE_WORKER', 'ROLE_OWNER') DEFAULT 'ROLE_WORKER',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    UNIQUE KEY `unique_provider` (`provider`, `provider_id`)
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

CREATE TABLE `colors` (
	`id` INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `color_name` VARCHAR(10) NOT NULL,
    `r` INT NOT NULL,
    `g` INT NOT NULL,
    `b` INT NOT NULL
);

-- 근무지 DB --
CREATE TABLE `workspace_category` (
	`id` INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `category_name` VARCHAR(100) NOT NULL
);

CREATE TABLE `workspaces` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `owner_id` BIGINT NULL,
    `workspace_name` VARCHAR(100) NOT NULL,
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
    `label_color_id` INT DEFAULT 0 NOT NULL,
    FOREIGN KEY(`owner_id`) REFERENCES users(`id`) ON DELETE SET NULL,
    FOREIGN KEY(`category_id`) REFERENCES workspace_category(`id`) ON DELETE SET NULL,
    FOREIGN KEY(`label_color_id`) REFERENCES colors(`id`) ON DELETE CASCADE
);
--

CREATE TABLE `workers` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `workspace_id` BIGINT NOT NULL,
    FOREIGN KEY(`user_id`) REFERENCES users(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`workspace_id`) REFERENCES workspaces(`id`) ON DELETE CASCADE
);

CREATE TABLE `works` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `workspace_id` BIGINT NOT NULL,
    `work_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `rest_time` TIME NULL,
    `memo` VARCHAR(50) NULL,
    `daily_income` INT NULL,
    FOREIGN KEY(`workspace_id`) REFERENCES workspaces(`id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_workspace_date` (`workspace_id`, `work_date`)
);

CREATE TABLE `calendar_work_mapping` (
	`id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `calendar_id` BIGINT NOT NULL,
    `work_id` BIGINT NOT NULL,
    `workspace_id` BIGINT NOT NULL,
    FOREIGN KEY(`calendar_id`) REFERENCES calendars(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`work_id`) REFERENCES works(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`workspace_id`) REFERENCES workspaces(`id`) ON DELETE CASCADE
);