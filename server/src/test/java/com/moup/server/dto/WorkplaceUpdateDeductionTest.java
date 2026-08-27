package com.moup.server.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import com.moup.domain.user.dto.OwnerWorkplaceUpdateRequest;
import com.moup.domain.user.dto.WorkerWorkplaceUpdateRequest;
import com.moup.domain.workplace.dto.BaseWorkplaceUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// `@JsonTypeInfo(DEDUCTION)`이 어떤 필드 조합에서 어느 서브타입을 고르는지 고정한다.
/// 클라이언트가 보내는 필드 집합이 서버 DTO와 어긋나면 조용히 오동작하는 게 아니라
/// 여기서 먼저 깨진다.
public class WorkplaceUpdateDeductionTest {

  /// Spring Boot가 실제로 주입하는 것과 동일한 설정(= `FAIL_ON_UNKNOWN_PROPERTIES` 비활성).
  /// raw `new ObjectMapper()`로 검증하면 운영과 다른 결론이 나온다.
  private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

  private static final String COMMON = """
      "workplaceName":"세븐일레븐","categoryName":"CVS",
      "address":"기본 주소","latitude":0.0,"longitude":0.0
      """;

  @Test
  @DisplayName("ownerBasedLabelColor가 있으면 사장님 DTO로 판별된다")
  void 사장님_판별() throws Exception {
    BaseWorkplaceUpdateRequest r = mapper.readValue(
        "{" + COMMON + ",\"ownerBasedLabelColor\":\"BLUE\"}", BaseWorkplaceUpdateRequest.class);
    assertInstanceOf(OwnerWorkplaceUpdateRequest.class, r);
  }

  @Test
  @DisplayName("workerBasedLabelColor가 있으면 알바생 DTO로 판별된다")
  void 알바생_판별() throws Exception {
    BaseWorkplaceUpdateRequest r = mapper.readValue(
        "{" + COMMON + ",\"workerBasedLabelColor\":\"RED\"}", BaseWorkplaceUpdateRequest.class);
    assertInstanceOf(WorkerWorkplaceUpdateRequest.class, r);
  }

  /// **판별 키가 둘 다 존재하면 Jackson은 실패하지 않고 JSON 키 순서로 고른다.**
  /// 먼저 나온 키가 이긴다. 클라이언트가 `null`을 명시 출력하는 인코더로 바뀌면
  /// 요청이 엉뚱한 서브타입으로 역직렬화될 수 있다는 뜻이다.
  ///
  /// 다만 `WorkplaceService.updateWorkplace`가 `user.getRole()`과 판별된 타입을
  /// 교차 검증하므로, 오판별은 조용한 오동작이 아니라 403으로 드러난다.
  /// 그 이중 방어가 왜 필요한지를 이 테스트가 설명한다.
  @Test
  @DisplayName("판별 키가 둘 다 있으면 JSON 키 순서로 서브타입이 정해진다")
  void 양쪽_키_존재시_키_순서로_결정() throws Exception {
    assertInstanceOf(OwnerWorkplaceUpdateRequest.class, mapper.readValue(
        "{" + COMMON + ",\"ownerBasedLabelColor\":null,\"workerBasedLabelColor\":\"RED\"}",
        BaseWorkplaceUpdateRequest.class));

    assertInstanceOf(WorkerWorkplaceUpdateRequest.class, mapper.readValue(
        "{" + COMMON + ",\"workerBasedLabelColor\":\"RED\",\"ownerBasedLabelColor\":null}",
        BaseWorkplaceUpdateRequest.class));
  }

  /// 어느 서브타입에도 없는 필드가 섞여 있어도 판별에는 영향이 없어야 한다.
  /// 서버에서 알바생 DTO의 근무지 필드를 제거해도 되는지를 결정하는 근거.
  @Test
  @DisplayName("알 수 없는 필드가 섞여도 판별은 유지된다")
  void 미지의_필드_무시() throws Exception {
    BaseWorkplaceUpdateRequest r = mapper.readValue(
        "{" + COMMON + ",\"workerBasedLabelColor\":\"RED\",\"totallyUnknown\":1}",
        BaseWorkplaceUpdateRequest.class);
    assertInstanceOf(WorkerWorkplaceUpdateRequest.class, r);
  }
}
