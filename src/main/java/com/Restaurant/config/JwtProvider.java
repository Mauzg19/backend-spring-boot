package com.Restaurant.config;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtProvider {
	
	private final SecretKey key;

	public JwtProvider(@Value("${app.jwt.secret}") String secret) {
		key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	Claims parseToken(String jwt) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
	}
	
	public String generateToken(Authentication auth) {
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
	    String roles = populateAuthorities(authorities);

		String jwt=Jwts.builder()
				.setIssuedAt(new Date())
				.setExpiration(new Date(new Date().getTime()+86400000))
				.claim("email",auth.getName())
				.claim("authorities", roles)
				.signWith(key)
				.compact();
		return jwt;
		
	}
	
	public String getEmailFromJwtToken(String jwt) {
		if (jwt == null) return null;
		jwt = jwt.trim();
		// aceptar tanto 'Bearer <token>' como el token puro
		if (jwt.toLowerCase().startsWith("bearer ")) {
			jwt = jwt.substring(7);
		}
		Claims claims = parseToken(jwt);
		String email = String.valueOf(claims.get("email"));
		return email;
	}
	
	public String populateAuthorities(Collection<? extends GrantedAuthority> collection) {
		Set<String> auths=new HashSet<>();
		
		for(GrantedAuthority authority:collection) {
			auths.add(authority.getAuthority());
		}
		return String.join(",",auths);
	}

}
