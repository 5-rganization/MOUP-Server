package com.moup.server.mapping;

import com.moup.domain.routine.domain.Routine;
import com.moup.domain.routine.domain.RoutineTask;
import com.moup.domain.salary.domain.Salary;
import com.moup.domain.user.domain.User;
import com.moup.domain.user.domain.Worker;
import com.moup.domain.work.domain.Work;
import com.moup.domain.work.domain.WorkRoutineMapping;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.global.security.token.SocialToken;
import com.moup.global.security.token.UserToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/// Phase 0-1 회귀 테스트 — MyBatis가 **위치 기반** 생성자 매핑으로 떨어지지 않도록 막는다.
///
/// MyBatis는 무인자 생성자가 없으면 유일한 생성자를 골라 **결과 컬럼을 순서대로** 밀어넣는다.
/// 지금까지는 테이블 컬럼 순서와 필드 선언 순서가 우연히 일치해 동작했을 뿐이다.
/// 컬럼을 하나 추가하거나 순서를 바꾸는 순간 **예외가 아니라 조용히 잘못된 값**이 들어간다.
///
/// 무인자 생성자가 있으면 MyBatis는 setter/필드 기반 **이름 매핑**을 쓴다.
/// 이 테스트는 그 전제(= 무인자 생성자 존재)를 고정한다.
class EntityConstructorMappingTest {

    @ParameterizedTest(name = "{0}에 무인자 생성자가 있어야 한다")
    @ValueSource(classes = {
            User.class, Worker.class, Workplace.class, Salary.class, Work.class,
            Routine.class, RoutineTask.class, WorkRoutineMapping.class,
            UserToken.class, SocialToken.class
    })
    @DisplayName("MyBatis 결과 매핑 대상 엔티티는 전부 무인자 생성자를 가져야 한다")
    void everyMappedEntityHasNoArgsConstructor(Class<?> entity) {
        assertDoesNotThrow(
                () -> entity.getDeclaredConstructor(),
                entity.getSimpleName() + "에 무인자 생성자가 없다. "
                        + "MyBatis가 위치 기반 생성자 매핑으로 떨어져 컬럼 추가 시 "
                        + "조용히 잘못된 값이 매핑된다. @NoArgsConstructor + @AllArgsConstructor를 붙일 것."
        );
    }
}
