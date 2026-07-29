
/**
 * The subset of an OpenID Connect provider's discovery document
 * (<code>/.well-known/openid-configuration</code>) needed to validate ID tokens.
 */
public final class OidcDiscoveryConfiguration {

	/** The `iss` claim every token from this provider must carry. */
	public final String issuer;

	/** Where the provider publishes the JWK set its tokens are signed with. */
	public final String jwksUri;

	public OidcDiscoveryConfiguration(String issuer, String jwksUri) {
		this.issuer = issuer;
		this.jwksUri = jwksUri;
	}

	@Override
	public String toString() {
		return String.format("OidcDiscoveryConfiguration(issuer=%s, jwksUri=%s)", issuer, jwksUri);
	}
}
