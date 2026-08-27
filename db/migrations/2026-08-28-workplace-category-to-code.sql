-- 근무지 카테고리를 표시용 한글에서 영문 코드로 전환한다.
--
-- 배경 —
-- `workplaces.category_name`에 화면 표시용 한글("편의점")이 그대로 저장돼 있었다.
-- 구 iOS 클라이언트가 UI 텍스트를 그대로 주고받도록 짜여 있었던 것이 원인이다.
-- 표시명을 DB에 두면 다국어 대응이 막히고, 클라이언트가 문구를 한 글자만 바꿔도
-- 기존 데이터와 매칭되지 않는다. 실제로 iOS 재구현 중 기존 근무지의 카테고리를
-- 읽지 못해 수정 화면 저장이 막혔다.
--
-- 실행 —
--   docker compose exec -T mysql mysql --default-character-set=utf8mb4 \
--     -u root -p"$DATABASE_PASSWORD" moup < db/migrations/2026-08-28-workplace-category-to-code.sql
--
-- ⚠️ 되돌리기 어렵다. 실행 전 백업할 것:
--   docker compose exec -T mysql mysqldump --single-transaction -u root -p"$DATABASE_PASSWORD" moup | gzip > pre-category-migration.sql.gz
--
-- 이 스크립트는 **매핑표에 없는 값이 하나라도 있으면 아무것도 바꾸지 않고 멈춘다.**
-- 임의로 OTHERS로 뭉개는 것은 되돌릴 수 없는 정보 손실이라 하지 않는다.

DELIMITER //

DROP PROCEDURE IF EXISTS migrate_workplace_category //

CREATE PROCEDURE migrate_workplace_category()
BEGIN
    DECLARE unmapped TEXT;

    -- 1. 사전 점검 — 모르는 값이 있으면 여기서 중단한다.
    SELECT GROUP_CONCAT(DISTINCT category_name ORDER BY category_name SEPARATOR ', ')
      INTO unmapped
      FROM workplaces
     WHERE category_name NOT IN (
        '음식점', '카페', '편의점', '영화관', '기타',
        'RESTAURANT', 'CAFE', 'CVS', 'MOVIE_THEATER', 'OTHERS');

    IF unmapped IS NOT NULL THEN
        -- SIGNAL의 MESSAGE_TEXT에는 함수 호출을 직접 쓸 수 없어 변수를 거친다.
        SET @msg = CONCAT('매핑되지 않은 카테고리가 있습니다. 확인 후 매핑표를 보완하세요: ', unmapped);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @msg;
    END IF;

    -- 2. 컬럼 폭 확대 — 가장 긴 코드 MOVIE_THEATER가 13자라 VARCHAR(10)에 들어가지 않는다.
    --    이 단계를 UPDATE보다 먼저 해야 한다.
    ALTER TABLE workplaces MODIFY COLUMN category_name VARCHAR(20) NOT NULL;

    -- 3. 변환. 이미 코드인 행은 그대로 두므로 재실행해도 안전하다.
    UPDATE workplaces
       SET category_name = CASE category_name
           WHEN '음식점' THEN 'RESTAURANT'
           WHEN '카페'   THEN 'CAFE'
           WHEN '편의점' THEN 'CVS'
           WHEN '영화관' THEN 'MOVIE_THEATER'
           WHEN '기타'   THEN 'OTHERS'
           ELSE category_name
       END;
END //

DELIMITER ;

CALL migrate_workplace_category();
DROP PROCEDURE migrate_workplace_category;

-- 4. 검증 — 아래 결과가 **0행**이어야 한다. 행이 나오면 전환되지 않은 값이 남은 것이다.
SELECT category_name, COUNT(*) AS cnt
  FROM workplaces
 WHERE category_name NOT IN ('RESTAURANT', 'CAFE', 'CVS', 'MOVIE_THEATER', 'OTHERS')
 GROUP BY category_name;
