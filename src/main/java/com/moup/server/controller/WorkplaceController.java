package com.moup.server.controller;

import com.moup.server.model.dto.ErrorResponse;
import com.moup.server.model.dto.RegisterRequest;
import com.moup.server.model.dto.RegisterResponse;
import com.moup.server.model.entity.Workplace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author neoskyclad
 * <p>
 * 근무지 정보 관리를 위한 Controller
 */
@Tag(name = "Workplace-Controller", description = "근무지 정보 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/workplace")
public class WorkplaceController {
//    @PostMapping()
//    @Operation(summary = "근무지 생성", description = "현재 로그인된 유저가 근무지 정보를 입력 하여 생성")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "근무지 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ))),
//            @ApiResponse(responseCode = "409", description = "중복된 근무지", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
//            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
//    @RequestBody(description = "근무지 생성을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = )))
//    public ResponseEntity<?> createWorkplace(@RequestBody Workplace workplace) {
//
//    }
}
