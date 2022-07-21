package com.todoay.api.domain.auth.service;

import com.todoay.api.domain.auth.dto.AuthSaveDto;
import com.todoay.api.domain.auth.dto.LoginRequestDto;
import com.todoay.api.domain.auth.dto.LoginResponseDto;
import com.todoay.api.domain.auth.entity.Auth;
import com.todoay.api.domain.auth.repository.AuthRepository;
import com.todoay.api.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final AuthRepository authRepository;
        private final JwtTokenProvider jwtTokenProvider;

        // spring security 필수 메소드
        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
            return authRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException((email)));
        }

        /**
         * 회원정보 저장
         * @return 저장되는 회원의 PK
         **/

        public Long save(AuthSaveDto authSaveDto) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            authSaveDto.setPassword(encoder.encode(authSaveDto.getPassword()));

            return authRepository.save(authSaveDto.toAuthEntity()).getId();
        }

        @Override
        public LoginResponseDto login(LoginRequestDto loginRequestDto) {
            Auth auth = (Auth)loadUserByUsername(loginRequestDto.getEmail());
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            if (!encoder.matches(loginRequestDto.getPassword(), auth.getPassword())) {
                throw new IllegalArgumentException();  // 나중에 custom exception 추가
            }

            String accessToken = jwtTokenProvider.createAccessToken(loginRequestDto.getEmail());
            String refreshToken = jwtTokenProvider.createRefreshToken(loginRequestDto.getEmail());
            return new LoginResponseDto(accessToken, refreshToken);
        }
}
