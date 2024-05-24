package clofi.runningplanet.security.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JWTUtil {

	private SecretKey secretKey;

	public JWTUtil(@Value("${custom.jwt.secretKey}") String secret) {

		byte[] keyBytes = Decoders.BASE64.decode(secret);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	public String getUsername(String token) {

		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.get("username", String.class);
	}

	public String getRole(String token) {

		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.get("role", String.class);
	}

	public Boolean isExpired(String token) {

		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getExpiration()
			.before(new Date());
	}

	public JwtToken createJwt(String username, Long expiredMs) {

		String accessToken = Jwts.builder()
			.claim("username", username)
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + expiredMs))
			.signWith(secretKey)
			.compact();

		long refreshExpiredMs = expiredMs * 24;
		String refreshToken = Jwts.builder()
			.expiration(new Date(System.currentTimeMillis() + refreshExpiredMs))
			.signWith(secretKey)
			.compact();

		return JwtToken.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}
}
