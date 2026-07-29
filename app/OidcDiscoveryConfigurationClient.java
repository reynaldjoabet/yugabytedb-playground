
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches an OpenID Connect provider's discovery document.
 */
public class OidcDiscoveryConfigurationClient {

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	private static final String ISSUER_FIELD = "issuer";

	private static final String JWKS_URI_FIELD = "jwks_uri";

	private final HttpClient httpClient;

	private final ObjectMapper mapper = new ObjectMapper();

	public OidcDiscoveryConfigurationClient() {
		this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
	}

	public OidcDiscoveryConfigurationClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * @param discoveryUrl the provider's <code>/.well-known/openid-configuration</code> URL
	 * @throws IOException if the document cannot be fetched, parsed, or lacks the fields token
	 *                     validation needs
	 */
	public OidcDiscoveryConfiguration fetchDiscoveryConfiguration(String discoveryUrl) throws IOException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(discoveryUrl)).timeout(REQUEST_TIMEOUT)
				.header("Accept", "application/json").GET().build();

		HttpResponse<String> response;
		try {
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while fetching OIDC discovery document from " + discoveryUrl, e);
		}
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException(String.format("Got status %d fetching OIDC discovery document from %s",
					response.statusCode(), discoveryUrl));
		}

		JsonNode document = mapper.readTree(response.body());
		return new OidcDiscoveryConfiguration(requireField(document, ISSUER_FIELD, discoveryUrl),
				requireField(document, JWKS_URI_FIELD, discoveryUrl));
	}

	private static String requireField(JsonNode document, String field, String discoveryUrl) throws IOException {
		JsonNode value = document.get(field);
		if (value == null || !value.isTextual() || value.asText().isEmpty()) {
			throw new IOException(String.format("OIDC discovery document from %s has no '%s'", discoveryUrl, field));
		}
		return value.asText();
	}
}
