package space.dawdawich.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFilter
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import space.dawdawich.configuration.model.WalletAuthenticationRequest
import space.dawdawich.configuration.provider.WalletAuthenticationProvider
import space.dawdawich.utils.baseDecode


@Configuration
@EnableWebSecurity
class SecurityConfiguration(private val environment: Environment) {

    @Bean
    fun authManager(http: HttpSecurity, jwtAuthProvider: WalletAuthenticationProvider): AuthenticationManager {
        val authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder::class.java)
        authenticationManagerBuilder.authenticationProvider(jwtAuthProvider)
        return authenticationManagerBuilder.build()
    }

    @Bean
    fun filterChain(
        http: HttpSecurity,
        authenticationManager: AuthenticationManager
    ): SecurityFilterChain {
        val authenticationFilter = AuthenticationFilter(authenticationManager) { request ->
            val address = request.getHeader("Account-Address")?.baseDecode()?.lowercase() ?: ""
            val signature = request.getHeader("Account-Address-Signature")?.baseDecode()?.lowercase() ?: ""

            if (address.isNotBlank() && signature.isNotBlank()) {
                WalletAuthenticationRequest(address, signature)
            } else {
                null
            }
        }

        authenticationFilter.successHandler = AuthenticationSuccessHandler { _, _, _ -> } // After success auth, AuthenticationSuccessHandler tries to redirect user to main page, with this approach we disabled redirect
        if (environment.activeProfiles[0] == "prod") {
            http
                .requiresChannel {
                    it.anyRequest().requiresSecure()
                }
        }
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
                    requestMatchers(HttpMethod.GET, "/account/salt").permitAll()
                    requestMatchers("/account/**").authenticated()
                    requestMatchers("/analyzer").authenticated()
                    requestMatchers("/analyzer/top20").permitAll()
                    requestMatchers("/analyzer/**").authenticated()
                    requestMatchers("/folder").authenticated()
                    requestMatchers("/folder/**").authenticated()
                    requestMatchers("/manager/**").authenticated()
                    requestMatchers("/symbol/names").permitAll()
                    requestMatchers("/symbol").hasAuthority("ADMIN")
                    requestMatchers("/ws/*").permitAll()
                    requestMatchers(HttpMethod.GET, "/health-check").hasAuthority("ADMIN")
                }
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

}
