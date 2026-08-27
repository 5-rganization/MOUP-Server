-- 스코프 1 I6 — user_tokens / social_tokens 에 UNIQUE (user_id) 추가
--
-- 왜: 저장 경로가 read-then-write(findByUserId → 없으면 save)라 동시 로그인 시
--     두 스레드가 모두 "행 없음"을 보고 INSERT 한다. 행이 2개가 되면
--     Optional<UserToken> 매핑이 TooManyResultsException 으로 터져
--     **해당 유저의 로그인·재발급이 영구 500** 이 된다. 스스로 복구되지 않는다.
--
-- 같이 나가는 코드 변경: 두 save() 를 INSERT ... ON DUPLICATE KEY UPDATE 로 바꿨다.
--     UNIQUE 만 추가하면 경합 시 DuplicateKeyException 으로 바뀔 뿐이라 함께 나가야 한다.
--
-- 실행: docker compose -f docker-compose.prod.yml exec -T mysql \
--         mysql -uroot -p"$DATABASE_PASSWORD" moup < 이 파일

-- 1) 중복 행 정리 — user_id 당 가장 최근(id 최대) 행만 남긴다.
--    UNIQUE 를 걸기 전에 반드시 먼저 실행해야 한다. 남은 중복이 있으면 ALTER 가 실패한다.
--    먼저 확인:
--    SELECT user_id, COUNT(*) c FROM user_tokens   GROUP BY user_id HAVING c > 1;
--    SELECT user_id, COUNT(*) c FROM social_tokens GROUP BY user_id HAVING c > 1;

DELETE t FROM `user_tokens` t
    JOIN (SELECT `user_id`, MAX(`id`) AS keep_id FROM `user_tokens` GROUP BY `user_id`) k
      ON t.`user_id` = k.`user_id` AND t.`id` <> k.keep_id;

DELETE t FROM `social_tokens` t
    JOIN (SELECT `user_id`, MAX(`id`) AS keep_id FROM `social_tokens` GROUP BY `user_id`) k
      ON t.`user_id` = k.`user_id` AND t.`id` <> k.keep_id;

-- 2) 제약 추가
ALTER TABLE `user_tokens`   ADD UNIQUE KEY `uk_user_tokens_user`   (`user_id`);
ALTER TABLE `social_tokens` ADD UNIQUE KEY `uk_social_tokens_user` (`user_id`);
