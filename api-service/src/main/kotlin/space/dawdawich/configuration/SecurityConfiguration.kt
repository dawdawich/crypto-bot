package space.dawdawich.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFilter
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import space.dawdawich.configuration.provider.JwtAuthenticationProvider
import space.dawdawich.configuration.provider.model.JWT
import space.dawdawich.configuration.provider.model.JwtAuthenticationToken
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


@Configuration
@EnableWebSecurity
open class SecurityConfiguration(
    @Value("\${app.signatureKey:dummy}") private val signatureKey: String
) {
    companion object {
        const val SIGNATURE_ALGORITHM = "HmacSHA256"
    }

    @Bean("mac")
    @Lazy(false)
    open fun getEncryptor(): Mac {
        val messageAlgCode = Mac.getInstance(SIGNATURE_ALGORITHM)
        messageAlgCode.init(SecretKeySpec(signatureKey.toByteArray(), SIGNATURE_ALGORITHM))
        return messageAlgCode
    }

    @Bean
    open fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    open fun authManager(http: HttpSecurity, jwtAuthProvider: JwtAuthenticationProvider): AuthenticationManager {
        val authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder::class.java)
        authenticationManagerBuilder.authenticationProvider(jwtAuthProvider)
        return authenticationManagerBuilder.build()
    }

    @Bean
    open fun filterChain(
        http: HttpSecurity,
        authenticationProvider: JwtAuthenticationProvider,
        authenticationManager: AuthenticationManager
    ): SecurityFilterChain {
        val authenticationFilter = AuthenticationFilter(authenticationManager) {
            val header = it.getHeader(HttpHeaders.AUTHORIZATION)

            if (header != null && header.startsWith("Bearer ")) {
                JwtAuthenticationToken(JWT.parseJwt(header.substring(7)), null)
            } else {
                null
            }
        }

        authenticationFilter.successHandler = AuthenticationSuccessHandler { _, _, _ -> } // After success auth, AuthenticationSuccessHandler tries to redirect user to main page, with this approach we disabled redirect
        return http
            .x509 { customizer -> customizer.disable() }
            .cors { customizer ->
                customizer.configurationSource {
                    CorsConfiguration().apply {
                        allowedOrigins = listOf("*")
                        allowedHeaders = listOf("*")
                        allowedMethods = mutableListOf("*")
                    }
                }
            }
            .csrf { customizer -> customizer.disable() }
            .authorizeHttpRequests {
                it.apply {
                    requestMatchers(HttpMethod.POST, "/account").permitAll()
                    requestMatchers(HttpMethod.GET, "/account/token").permitAll()
                    requestMatchers(HttpMethod.GET, "/account").authenticated()
                    requestMatchers(HttpMethod.GET, "/account/api-token").authenticated()
                    requestMatchers("/analyzer").authenticated()
                    requestMatchers("/analyzer/bulk").hasAuthority("ADMIN")
                    requestMatchers("/trade-manager").authenticated()
                    requestMatchers("/symbol/all").permitAll()
                    requestMatchers("/symbol").hasAuthority("ADMIN")
                    requestMatchers(HttpMethod.GET, "/health-check").hasAuthority("ADMIN")
                    anyRequest().permitAll()
                }
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}
