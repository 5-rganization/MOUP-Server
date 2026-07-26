DROP TABLE IF EXISTS admin_alarm_user_mappings;
DROP TABLE IF EXISTS admin_alarms;
DROP TABLE IF EXISTS normal_alarms;
DROP TABLE IF EXISTS users;

CREATE TABLE users
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider    ENUM ('LOGIN_GOOGLE', 'LOGIN_APPLE', 'LOGIN_NAVER', 'LOGIN_KAKAO') NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    username    VARCHAR(20),
    nickname    VARCHAR(20),
    role        ENUM ('ROLE_WORKER', 'ROLE_OWNER', 'ROLE_ADMIN') DEFAULT 'ROLE_WORKER',
    profile_img VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP,
    is_deleted  BOOLEAN DEFAULT FALSE,
    fcm_token   TEXT,
    CONSTRAINT unique_provider UNIQUE (provider, provider_id)
);

CREATE TABLE normal_alarms
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id   BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    title       TEXT NOT NULL,
    content     TEXT,
    sent_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at     DATETIME,
    created_at  DATETIME,
    updated_at  DATETIME,
    CONSTRAINT fk_normal_alarm_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT fk_normal_alarm_receiver FOREIGN KEY (receiver_id) REFERENCES users (id)
);

CREATE TABLE admin_alarms
(
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    title   TEXT NOT NULL,
    content TEXT,
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE admin_alarm_user_mappings
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    alarm_id   BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    read_at    DATETIME,
    deleted_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_admin_alarm_mapping_alarm
        FOREIGN KEY (alarm_id) REFERENCES admin_alarms (id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_alarm_mapping_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
