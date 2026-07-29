
import java.io.InvalidClassException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;

/**
 * MD5-based hash, in the crypt(3) <code>$1$</code> (md5crypt) format, plus a raw mode for bare
 * unsalted MD5 digests.
 * <p>
 * MD5 is not a suitable password hash any more. This class exists so that hashes created before
 * {@link Argon2idHash} can still be verified; new hashes should use Argon2id.
 */
public final class Md5BasedHash extends Hash {

	private static final Charset HASH_CHARSET = StandardCharsets.US_ASCII;
	private static final Charset MESSAGE_CHARSET = StandardCharsets.UTF_8;

	private static final String MD5CRYPT_PREFIX = "$1$";

	/** <code>$1$&lt;salt&gt;$&lt;checksum&gt;</code>, as produced by crypt(3). */
	private static final Pattern HASHSTRING_PATTERN = Pattern
			.compile("^\\$1\\$(?<salt>[./A-Za-z0-9]{1,8})\\$(?<hash>[./A-Za-z0-9]{22})$");

	/** A bare, unsalted MD5 digest in hex, as used by legacy stores. */
	private static final Pattern RAW_HASHSTRING_PATTERN = Pattern.compile("^(?<hash>[a-fA-F0-9]{32})$");

	public static Md5BasedHash fromHashString(String hashString) throws InvalidClassException {
		if (hashString == null) {
			return null;
		}
		final Matcher matcher = HASHSTRING_PATTERN.matcher(hashString);
		if (!matcher.matches()) {
			throw new InvalidClassException("Cannot parse md5crypt hash string");
		}
		Md5BasedHash md5Hash = new Md5BasedHash(false);
		md5Hash.salt = matcher.group("salt");
		md5Hash.hash = hashString;
		return md5Hash;
	}

	public static Md5BasedHash fromRawHashString(String hashString) throws InvalidClassException {
		if (hashString == null) {
			return null;
		}
		if (!RAW_HASHSTRING_PATTERN.matcher(hashString).matches()) {
			throw new InvalidClassException("Cannot parse raw MD5 hash string");
		}
		Md5BasedHash md5Hash = new Md5BasedHash(true);
		md5Hash.hash = hashString.toLowerCase();
		return md5Hash;
	}

	/** Unsalted hex digest instead of the salted crypt(3) format. */
	private final boolean raw;

	private String salt;

	private String hash;

	public Md5BasedHash() {
		this(false);
	}

	private Md5BasedHash(boolean raw) {
		this.raw = raw;
	}

	public Md5BasedHash(String input) {
		this(false);
		this.digest(input);
	}

	@Override
	public void digest(String[] inputs) {
		if (inputs == null || inputs.length == 0) {
			this.hash = null;
			return;
		}
		byte[] inputBytes = concat(inputs);
		if (this.raw) {
			this.hash = DigestUtils.md5Hex(inputBytes);
			return;
		}
		this.hash = Md5Crypt.md5Crypt(inputBytes, this.salt == null ? null : MD5CRYPT_PREFIX + this.salt);
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
		byte[] inputBytes = input.getBytes(MESSAGE_CHARSET);
		// Both forms accept the stored hash in place of the salt and take the salt back out of it.
		String candidate = this.raw ? DigestUtils.md5Hex(inputBytes) : Md5Crypt.md5Crypt(inputBytes, this.hash);
		return MessageDigest.isEqual(candidate.getBytes(HASH_CHARSET), this.hash.getBytes(HASH_CHARSET));
	}

	public String getSalt() {
		return this.salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
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
