package app.hyuabot.backend.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val tokenProvider: JWTTokenProvider,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .httpBasic { it.disable() } // 기본 로그인 인증창 비활성화
            .csrf { it.disable() } // CSRF 보호 비활성화 (API 서버에서는 일반적으로 필요 없음)
            .cors {
                val configuration = CorsConfiguration()
                // 허용할 출처
                configuration.addAllowedOrigin("http://localhost:3000")
                configuration.addAllowedOrigin("http://localhost:8080")
                configuration.addAllowedOrigin("https://admin.hyuabot.app")
                configuration.addAllowedOrigin("https://backend.hyuabot.app")
                configuration.addAllowedOrigin("https://dashboard.hyuabot.app")
                configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                configuration.allowCredentials = true // 자격 증명 허용
                configuration.allowedHeaders = listOf("*") // 모든 헤더 허용
                val source = UrlBasedCorsConfigurationSource()
                source.registerCorsConfiguration("/**", configuration)
                it.configurationSource(source)
            }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) } // 세션 관리 정책을 Stateless로 설정
            .authorizeHttpRequests { requests ->
                requests // 인증 API, Swagger UI, GraphQL 클라이언트 API는 인증 없이 접근 가능
                    .requestMatchers(
                        "/api/v1/user",
                        "/api/v1/user/token",
                        "/api/v1/live-activity/**",
                        "/api/v1/analytics/watch/events",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/graphql/**",
                        "/graphiql/**",
                        "/error",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/prometheus",
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.addFilterBefore(
                JWTAuthenticationFilter(tokenProvider, redisTemplate),
                UsernamePasswordAuthenticationFilter::class.java,
            ).build()

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()
}
