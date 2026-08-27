-- 스코프 7 C3 — workplaces.owner_id 의 ON DELETE CASCADE 를 SET NULL 로 변경
--
-- 왜: 사장님이 탈퇴(하드 삭제)하면 users → workplaces → workers → works/salaries/
--     work_routine_mappings 가 연쇄 삭제되어, 아무 잘못 없는 알바생 전원의 근무·급여
--     이력이 사라진다. 확정 정책 3(사장님 탈퇴 시 데이터는 남기고 접근만 차단)과 정면 충돌.
--     바로 옆 workers.user_id 는 이미 SET NULL 이다.
--
-- 선행 조건: PermissionVerifyUtil 의 Objects.equals 수정(C2)이 배포되어 있어야
--           owner_id 가 NULL 인 근무지 조회 시 500 이 아니라 403 이 나온다.
--
-- 실행: docker compose -f docker-compose.prod.yml exec -T mysql \
--         mysql -uroot -p"$DATABASE_PASSWORD" moup < 이 파일
--
-- 되돌리기: 아래 ADD CONSTRAINT 를 ON DELETE CASCADE 로 다시 실행.

-- 1) 기존 FK 이름 확인 (스키마에 이름이 없어 MySQL 이 자동 생성했다. 보통 workplaces_ibfk_1)
--    SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
--     WHERE TABLE_SCHEMA = 'moup' AND TABLE_NAME = 'workplaces'
--       AND COLUMN_NAME = 'owner_id' AND REFERENCED_TABLE_NAME = 'users';

ALTER TABLE `workplaces` DROP FOREIGN KEY `workplaces_ibfk_1`;

ALTER TABLE `workplaces`
    ADD CONSTRAINT `fk_workplaces_owner`
        FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- 2) 검증 — DELETE_RULE 이 SET NULL 이어야 한다
--    SELECT CONSTRAINT_NAME, DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS
--     WHERE CONSTRAINT_SCHEMA = 'moup' AND TABLE_NAME = 'workplaces';
