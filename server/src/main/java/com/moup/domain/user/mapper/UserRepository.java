package com.moup.domain.user.mapper;

import com.moup.domain.auth.domain.Login;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.dto.UserCreateRequest;
import com.moup.global.common.type.Role;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserRepository {
    @Insert("INSERT INTO users (provider, provider_id, username) VALUES (#{provider}, #{providerId}, #{username})")
    @Options(useGeneratedKeys = true, keyProperty = "userId", keyColumn = "id")
    Long create(UserCreateRequest userCreateRequest);

    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);

    /// 여러 사용자 ID에 해당하는 모든 사용자를 조회하는 메서드
    ///
    /// @param idList 조회할 근무지 ID 리스트
    /// @return 조회된 Workplace 객체 리스트
    @Select("""
            <script>
                SELECT * FROM users
                WHERE id IN
                <foreach item="id" collection="idList" open="(" separator="," close=")">
                      #{id}
                </foreach>
            </script>
            """)
    List<User> findAllByIdListIn(@Param("idList") List<Long> idList);

    @Select("SELECT * FROM users WHERE provider = #{provider} AND provider_id = #{providerId}")
    Optional<User> findByProviderAndId(Login provider, String providerId);

    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(String username);

    @Update("UPDATE users SET profile_img = #{profileImg} WHERE id = #{id}")
    void updateProfileImg(Long id, String profileImg);

    @Update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP(), is_deleted = 1 WHERE id = #{id}")
    void softDeleteUserById(Long id);

    @Update("UPDATE users SET deleted_at = null, is_deleted = 0 WHERE id = #{id}")
    void undeleteUserById(Long id);

    /// 탈퇴 확정 — 행을 지우지 않고 **개인정보만 제거**한다 (확정 정책 7).
    ///
    /// 하드 삭제하면 `workplaces`·`workers`·`works`·`salaries`가 CASCADE/SET NULL로
    /// 함께 무너져, 남아 있는 알바생들의 근무·급여 이력까지 사라진다. 그 데이터는
    /// 사장님만의 것이 아니다.
    ///
    /// `provider_id`를 난수로 치환하는 이유: 그대로 두면 같은 소셜 계정으로 재가입할 때
    /// `UNIQUE (provider, provider_id)`가 충돌한다. 치환하면 재가입 시 새 계정이 생기고
    /// 과거 이력과 연결되지 않는다 — 삭제 요청의 정상적 결과다.
    ///
    /// `nickname`을 NULL이 아니라 리터럴로 두는 이유: 근무자 목록 등에서 nickname을
    /// 그대로 노출하는 지점이 여럿이라, NULL로 두면 빈칸이 뜬다.
    ///
    /// **유지**: `id`(FK 무결성), `provider`, `role`, `created_at`, `deleted_at`
    @Update("""
            UPDATE users
            SET username      = NULL,
                nickname      = '탈퇴한 사용자',
                profile_img   = NULL,
                provider_id   = CONCAT('withdrawn_', UUID()),
                is_deleted    = 1,
                anonymized_at = CURRENT_TIMESTAMP()
            WHERE id = #{id}
            """)
    void anonymizeUserById(Long id);

    /// 아직 가명처리되지 않은 탈퇴 신청 건만 집는다.
    /// `anonymized_at IS NULL`이 없으면 이미 처리된 사용자를 매일 다시 집어
    /// 소셜 연동 해제를 무한 재시도한다.
    @Select("SELECT * FROM users WHERE is_deleted = 1 AND anonymized_at IS NULL")
    List<User> findAllHardDeleteUsers();

    /// 유예기간이 지났고 아직 포기 기준에는 도달하지 않은 삭제 대상을 조회한다.
    /// 소셜 연동 해제를 계속 재시도할 대상이다.
    @Select("SELECT * FROM users WHERE is_deleted = 1 AND anonymized_at IS NULL AND deleted_at < #{graceDeadline} AND deleted_at >= #{giveUpDeadline}")
    List<User> findAllOldHardDeleteUsers(LocalDateTime graceDeadline, LocalDateTime giveUpDeadline);

    /// 포기 기준을 넘긴 삭제 대상을 조회한다.
    /// 소셜 연동 해제를 더 시도하지 않고 기록만 남긴 뒤 삭제할 대상이다.
    @Select("SELECT * FROM users WHERE is_deleted = 1 AND anonymized_at IS NULL AND deleted_at < #{giveUpDeadline}")
    List<User> findAllRevokeGiveUpUsers(LocalDateTime giveUpDeadline);

    /// 탈퇴(신청 포함) 여부. 근무지 쓰기 게이트에서 소유자 상태를 볼 때 쓴다.
    @Select("SELECT EXISTS(SELECT 1 FROM users WHERE id = #{id} AND is_deleted = 1)")
    boolean isWithdrawn(Long id);

    /// 주어진 id 중 **탈퇴한(탈퇴 신청 포함) 사용자**의 id만 돌려준다.
    ///
    /// 근무지 목록에서 사장님 탈퇴 여부를 판정할 때 쓴다. 근무지마다 사장님을 조회하면
    /// N+1이 되므로 한 번에 가져온다. 호출부에서 빈 리스트를 사전 차단할 것.
    @Select("""
            <script>
            SELECT id FROM users
            WHERE is_deleted = 1
              AND id IN <foreach item="id" collection="ids" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Long> findWithdrawnIdsIn(@Param("ids") List<Long> ids);

    @Update("UPDATE users SET nickname = #{nickname}, role = #{role} WHERE id = #{id}")
    void updateById(Long id, String nickname, Role role);

    @Update("UPDATE users SET nickname = #{nickname} WHERE id = #{id}")
    void updateNicknameById(Long id, String nickname);

    @Select("SELECT * FROM users ORDER BY id LIMIT #{batchSize} OFFSET #{offset}")
    List<User> findUsersWithPaging(@Param("offset") int page, @Param("batchSize") int batchSize);
}
