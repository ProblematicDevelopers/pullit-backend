package com.pullit.user.controller;

import com.pullit.common.annotation.AuthUser;
import com.pullit.common.annotation.LoggingTrace;
import com.pullit.common.annotation.RateLimited;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.common.exception.ErrorCode;
import com.pullit.user.dto.request.PasswordChangeRequest;
import com.pullit.user.dto.request.UserUpdateRequest;
import com.pullit.user.dto.response.UserResponse;
import com.pullit.user.entity.User;
import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name="User", description = "사용자 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보 조회")
    @LoggingTrace
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@AuthUser CustomUserDetails userDetails){
        User user = userService.getUserById(userDetails.getUserId());
        UserResponse response = UserResponse.from(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 정보를 수정합니다")
    @LoggingTrace
    @RateLimited(limit = 1, duration = 1, timeUnit= TimeUnit.MINUTES)
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
            @AuthUser CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse response = userService.updateUser(userDetails.getUserId(), request);

        return ResponseEntity.ok(ApiResponse.success(response, "사용자 정보가 수정되었습니다"));
    }

    @PutMapping("/me/password")
    @Operation(summary = "비밀번호 변경", description = "현재 로그인한 사용자의 비밀번호를 변경합니다")
    @LoggingTrace
    @RateLimited(limit = 5, duration = 5, timeUnit = TimeUnit.MINUTES)  // 5분에 5회
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthUser CustomUserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {

        if (!request.isPasswordMatching()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.PASSWORD_MISMATCH));
        }

        userService.changePassword(userDetails.getUserId(), request);

        return ResponseEntity.ok(ApiResponse.successWithoutData("비밀번호가 변경되었습니다"));
    }

    @GetMapping("/check/username/{username}")
    @Operation(summary = "사용자명 중복 체크", description = "사용자명이 이미 사용중인지 확인합니다")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@PathVariable String username) {

        boolean exists = userService.existsByUsername(username);

        return ResponseEntity.ok(ApiResponse.success(!exists,
                exists ? "이미 사용중인 사용자명입니다" : "사용 가능한 사용자명입니다"));
    }

    @GetMapping("/check/email/{email}")
    @Operation(summary = "이메일 중복 체크", description = "이메일이 이미 사용중인지 확인합니다")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@PathVariable String email) {

        boolean exists = userService.existsByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(!exists,
                exists ? "이미 사용중인 이메일입니다" : "사용 가능한 이메일입니다"));
    }

}
