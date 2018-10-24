package io.echoplex.web.auth;

import org.pac4j.core.config.Config;
import org.pac4j.springframework.security.web.CallbackFilter;
import org.pac4j.springframework.security.web.LogoutFilter;
import org.pac4j.springframework.security.web.Pac4jEntryPoint;
import org.pac4j.springframework.security.web.SecurityFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@EnableWebSecurity
public class SecurityConfig {

	@Configuration
	@Order(15)
	public static class CallbackConfigurationAdapter extends WebSecurityConfigurerAdapter {
		@Autowired
		private Config config;

		protected void configure(HttpSecurity http) throws Exception {

			CallbackFilter callbackFilter = new CallbackFilter(config);
			callbackFilter.setMultiProfile(true);

			http.antMatcher("/**").addFilterBefore(callbackFilter, BasicAuthenticationFilter.class).csrf().disable();

		}
	} 

	@Configuration
	@Order(1)
	public static class FacebookWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		@Autowired
		private Config config;

		protected void configure(final HttpSecurity http) throws Exception {

			final SecurityFilter filter = new SecurityFilter(config, "FacebookClient");
			filter.setMatchers("excludedPath");

			http.antMatcher("/facebook/**").addFilterBefore(filter, BasicAuthenticationFilter.class).sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
			http.csrf().disable();
		}
	}

	@Configuration
	@Order(2)
	public static class TwitterWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		@Autowired
		private Config config;

		protected void configure(final HttpSecurity http) throws Exception {

			final SecurityFilter filter = new SecurityFilter(config, "TwitterClient,FacebookClient");
			filter.setMatchers("excludedPath");

			http.antMatcher("/twitter/**").addFilterBefore(filter, BasicAuthenticationFilter.class).sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
			http.csrf().disable();
		}
	}

	@Configuration
	@Order(3)
	public static class FormWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		@Autowired
		private Config config;

		protected void configure(final HttpSecurity http) throws Exception {

			final SecurityFilter filter = new SecurityFilter(config, "FormClient");
			filter.setMatchers("excludedPath");

			http.antMatcher("/echo/**").authorizeRequests().anyRequest().authenticated().and().exceptionHandling().authenticationEntryPoint(new Pac4jEntryPoint(config, "FormClient")).and()
					.addFilterBefore(filter, BasicAuthenticationFilter.class).sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
			http.csrf().disable();
		}
	}

	@Configuration
	@Order(4)
	public static class GoogleWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		@Autowired
		private Config config;

		protected void configure(final HttpSecurity http) throws Exception {

			final SecurityFilter filter = new SecurityFilter(config, "GoogleOidcClient");
			filter.setMatchers("excludedPath");

			http.antMatcher("/google/**").addFilterBefore(filter, BasicAuthenticationFilter.class).sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
			http.csrf().disable();
		}
	}

	@Configuration
	@Order(13)
	public static class Pac4jLogoutWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		@Autowired
		private Config config;

		protected void configure(final HttpSecurity http) throws Exception {

			final LogoutFilter filter = new LogoutFilter(config, "/");
			filter.setDestroySession(true);

			http.antMatcher("/logout").addFilterBefore(filter, BasicAuthenticationFilter.class).sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER);
		}
	}

	@Configuration
	@Order(14)
	public static class Pac4jCentralLogoutWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		@Autowired
		private Config config;

		protected void configure(final HttpSecurity http) throws Exception {

			final LogoutFilter filter = new LogoutFilter(config, "http://localhost:8080/?defaulturlafterlogoutafteridp");
			filter.setLocalLogout(false);
			filter.setCentralLogout(true);
			filter.setLogoutUrlPattern("http://localhost:8080/.*");

			http.antMatcher("/centralLogout").addFilterBefore(filter, BasicAuthenticationFilter.class).sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER);
		}
	}
}