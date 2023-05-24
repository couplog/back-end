package com.dateplan.dateplan.global.auth;

import static com.dateplan.dateplan.global.constant.Auth.HEADER_AUTHORIZATION;
import static com.dateplan.dateplan.global.constant.Auth.REFRESH_TOKEN_EXPIRATION;
import static com.dateplan.dateplan.global.constant.Auth.SUBJECT_REFRESH_TOKEN;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.global.exception.auth.TokenExpiredException;
import com.dateplan.dateplan.global.exception.auth.TokenInvalidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class JwtProvider {

	@Value("${jwt.secret}")
	private String secret;
	private final MemberRepository memberRepository;
	private final StringRedisTemplate redisTemplate;

	public Member findMemberByToken(String token) {
		return memberRepository.findById(getIdByToken(token))
			.orElseThrow(MemberNotFoundException::new);
	}

	private Long getIdByToken(String token) {
		try {
			return Long.parseLong(String.valueOf(Jwts.parser()
				.setSigningKey(generateKey())
				.parseClaimsJws(token)
				.getBody()
				.get("id")));
		} catch (ExpiredJwtException e) {
			throw new TokenExpiredException();
		} catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
			throw new TokenInvalidException();
		}
	}

	public String generateToken(Long id, Long expiration, String subject) {
		Date issueDate = new Date();
		Date expireDate = new Date();
		expireDate.setTime(issueDate.getTime() + expiration);
		return Jwts.builder()
			.setHeaderParam("typ", "JWT")
			.setClaims(generateClaims(id))
			.setIssuedAt(issueDate)
			.setSubject(subject)
			.setExpiration(expireDate)
			.signWith(SignatureAlgorithm.HS256, generateKey())
			.compact();
	}

	public String generateAccessTokenByRefreshToken(String refreshToken) {
		Member member = findMemberByToken(refreshToken);

		if (!checkRefreshTokenEquals(member, refreshToken)) {
			throw new TokenInvalidException();
		}

		return generateToken(
			member.getId(),
			REFRESH_TOKEN_EXPIRATION.getExpiration(),
			SUBJECT_REFRESH_TOKEN.getContent()
		);
	}

	private boolean checkRefreshTokenEquals(Member member, String refreshToken) {
		ListOperations<String, String> opsForList = redisTemplate.opsForList();

		String key = String.valueOf(member.getId());
		String value = opsForList.rightPop(key);

		if (value == null || !value.equals(refreshToken)) {
			return false;
		}

		opsForList.rightPush(key, value);
		return true;
	}

	private Claims generateClaims(Long id) {
		Claims claims = Jwts.claims();
		claims.put("id", id);
		return claims;
	}

	public boolean isValid(String token) {
		try {
			Jwts.parser()
				.setSigningKey(generateKey())
				.parseClaimsJws(token);
			return true;
		} catch (ExpiredJwtException e) {
			throw new TokenExpiredException();
		} catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
			throw new TokenInvalidException();
		}
	}

	public Optional<String> resolveToken(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(HEADER_AUTHORIZATION.getContent()));
	}

	private byte[] generateKey() {
		return secret.getBytes(StandardCharsets.UTF_8);
	}
}
