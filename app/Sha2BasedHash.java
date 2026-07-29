
import java.io.InvalidClassException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.Sha2Crypt;

/**
 * SHA-2-based hash, in the crypt(3) <code>$5$</code> (sha256crypt) and <code>$6$</code>
 * (sha512crypt) formats, including an optional <code>rounds=</code> parameter.
 * <p>
 * Parsing accepts either variant; hashes created here use sha512crypt. Prefer
 * {@link Argon2idHash} for new hashes - sha-crypt is only memory-hard by iteration count.
 */
public final class Sha2BasedHash extends Hash {

	private static final Charset HASH_CHARSET = StandardCharsets.US_ASCII;
	private static final Charset MESSAGE_CHARSET = StandardCharsets.UTF_8;

	private static final String SHA256_PREFIX = "$5$";
	private static final String SHA512_PREFIX = "$6$";

	/** <code>$5$</code> or <code>$6$</code>, an optional rounds parameter, the salt and checksum. */
	private static final Pattern HASHSTRING_PATTERN = Pattern.compile("^\\$(?<type>[56])\\$((?<params>rounds=[0-9]+)"
			+ "\\$)?(?<salt>[./A-Za-z0-9]{1,16})\\$(?<hash>[./A-Za-z0-9]{43,86})$");

	public static Sha2BasedHash fromHashString(String hashString) throws InvalidClassException {
		if (hashString == null) {
			return null;
		}
		final Matcher matcher = HASHSTRING_PATTERN.matcher(hashString);
		if (!matcher.matches()) {
			throw new InvalidClassException("Cannot parse sha2crypt hash string");
		}
		Sha2BasedHash sha2Hash = new Sha2BasedHash();
		sha2Hash.prefix = "5".equals(matcher.group("type")) ? SHA256_PREFIX : SHA512_PREFIX;
		sha2Hash.params = matcher.group("params");
		sha2Hash.salt = matcher.group("salt");
		sha2Hash.hash = hashString;
		return sha2Hash;
	}

	private String prefix = SHA512_PREFIX;

	/** The <code>rounds=N</code> segment, when the hash string carries one. */
	private String params;

	private String salt;

	private String hash;

	public Sha2BasedHash() {

	}

	public Sha2BasedHash(String input) {
		this.digest(input);
	}

	@Override
	public void digest(String[] inputs) {
		if (inputs == null || inputs.length == 0) {
			this.hash = null;
			return;
		}
		this.hash = crypt(concat(inputs), this.salt == null ? null : saltSpec());
		final Matcher matcher = HASHSTRING_PATTERN.matcher(this.hash);
		if (matcher.matches()) {
			this.salt = matcher.group("salt");
		}
	}

	@Override
	public String toHashString() {
		return this.hash;
	}

	@Override
	public boolean check(String input) {
		if (input == null) {
			return this.hash == null;
		}
		if (this.hash == null) {
			return false;
		}
		// sha2crypt accepts the stored hash in place of the salt and takes the salt back out of it.
		String candidate = crypt(input.getBytes(MESSAGE_CHARSET), this.hash);
		return MessageDigest.isEqual(candidate.getBytes(HASH_CHARSET), this.hash.getBytes(HASH_CHARSET));
	}

	public String getSalt() {
		return this.salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	private String crypt(byte[] input, String saltSpec) {
		if (SHA256_PREFIX.equals(this.prefix)) {
			return Sha2Crypt.sha256Crypt(input, saltSpec);
		}
		return Sha2Crypt.sha512Crypt(input, saltSpec);
	}

	private String saltSpec() {
		if (this.params == null) {
			return this.prefix + this.salt;
		}
		return this.prefix + this.params + "$" + this.salt;
	}

	private static byte[] concat(String[] inputs) {
		StringBuilder sb = new StringBuilder();
		for (String input : inputs) {
			if (input != null) {
				sb.append(input);
			}
		}
		return sb.toString().getBytes(MESSAGE_CHARSET);
	}
}
