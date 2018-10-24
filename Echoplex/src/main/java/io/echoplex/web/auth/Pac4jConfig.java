package io.echoplex.web.auth;

import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.matching.PathMatcher;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.JWSAlgorithm;

@Configuration
public class Pac4jConfig {

	@Autowired
	Authenticator<UsernamePasswordCredentials> authenticator;
	
	@Bean
	public Config config() {
		final OidcConfiguration oidcConfiguration = new OidcConfiguration();
		oidcConfiguration.setClientId("12686579480-rugnk47glkr2du6aqbf9n8neiv15e92g.apps.googleusercontent.com");
		oidcConfiguration.setSecret("r4M5St7vK4HM94tPV_7k1MIq");
		oidcConfiguration.setPreferredJwsAlgorithm(JWSAlgorithm.PS384);
		oidcConfiguration.addCustomParam("prompt", "consent");
		final GoogleOidcClient oidcClient = new GoogleOidcClient(oidcConfiguration);
		oidcClient.setAuthorizationGenerator((ctx, profile) -> {
			profile.addRole("ROLE_ADMIN");
			return profile;
		});

//		final SAML2ClientConfiguration cfg = new SAML2ClientConfiguration("resource:samlKeystore.jks", "pac4j-demo-passwd", "pac4j-demo-passwd", "resource:metadata-okta.xml");
//		cfg.setMaximumAuthenticationLifetime(3600);
//		cfg.setServiceProviderEntityId("http://localhost:8080/callback?client_name=SAML2Client");
//		cfg.setServiceProviderMetadataPath("sp-metadata.xml");
//		final SAML2Client saml2Client = new SAML2Client(cfg);

		final FacebookClient facebookClient = new FacebookClient("311452675930199", "14ee87a5246158b1258db9b49f7b6d35");
		final TwitterClient twitterClient = new TwitterClient("9rDcWraAOSmWMQtpwbCvvWb9e", "YJFYWsuna3L53GnorXx6AoVbCia8MkH0470LDjbWzIPUflSX5R");
	

	//	final SCryptUsernamePasswordAuthenticator simpleTestUsernamePasswordAuthenticator = new SCryptUsernamePasswordAuthenticator();
		final FormClient formClient = new FormClient("http://echoplex.gc4.io/login", authenticator);
//		final IndirectBasicAuthClient indirectBasicAuthClient = new IndirectBasicAuthClient(simpleTestUsernamePasswordAuthenticator);
//
//		final CasConfiguration casConfiguration = new CasConfiguration("https://casserverpac4j.herokuapp.com/login");
//		final CasClient casClient = new CasClient(casConfiguration);
//
//		ParameterClient parameterClient = new ParameterClient("token", new JwtAuthenticator(new SecretSignatureConfiguration(salt), new SecretEncryptionConfiguration(salt)));
//		parameterClient.setSupportGetRequest(true);
//		parameterClient.setSupportPostRequest(false);
//
//		final DirectBasicAuthClient directBasicAuthClient = new DirectBasicAuthClient(simpleTestUsernamePasswordAuthenticator);
//
//		final AnonymousClient anonymousClient = new AnonymousClient();

		final Clients clients = new Clients("http://echoplex.gc4.io/callback", formClient, twitterClient, facebookClient, oidcClient);
		

		final Config config = new Config(clients);
		config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
		//config.addAuthorizer("custom", new CustomAuthorizer());
		//config.addMatcher("excludedPath", new PathMatcher().excludePath("/"));
		PathMatcher pm = new PathMatcher();
		pm.excludePath("/error"); 
		//pm.excludePath("/login");
		pm.excludePath("/");
		pm.excludeBranch("/VAADIN");
		pm.excludeBranch("/vaadinServlet");
		//pm.excludeBranch("/callback");
		
		
		config.addMatcher("excludedPath", pm);
		return config;
	}
}