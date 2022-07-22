package com.todoay.api.global.config;

import com.todoay.api.domain.auth.service.AuthService;
import com.todoay.api.global.jwt.JwtAuthenticationFilter;
import com.todoay.api.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.hibernate.criterion.Restrictions.and;

@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;


    @Override
    public void configure(WebSecurity web){
        web.ignoring().antMatchers("/css/**", "/js/**", "/img/**", "/swagger-ui/**", "/v3/api-docs/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .headers().frameOptions().disable()

                .and()
                    .authorizeRequests()
                        .antMatchers("/auth/**", "/signup", "/user/**", "/docs", "/profile/**", "/h2-console/**").permitAll()  // 누구나 접근 가능 // profile/my는 permitAll하면 안됨.
                        .antMatchers("/").hasRole("USER")  // USER, ADMIN만 접근 가능
                        .antMatchers("/admin").hasRole("ADMIN")  // ADMIN만
                        .anyRequest().authenticated()  // 나머지 요청들은 권한의 종류에 상관없이 권한이 있어야 접근

                .and()
                    .formLogin()
                        .loginPage("/auth")  // 로그인 페이지 링크
                        .defaultSuccessUrl("/")  // 로그인 성공 후 리다이렉트 주소
                .and()
                    .logout()
                        .logoutSuccessUrl("/")  // 로그아웃 성공시 리다이렉트 주소
                        .invalidateHttpSession(true)

                .and()
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(authService).passwordEncoder(new BCryptPasswordEncoder());
    }
}
