package com.Restaurant.config;

import java.io.IOException;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtTokenValidator extends OncePerRequestFilter {

	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String jwt = request.getHeader(JwtConstant.JWT_HEADER);
		
		if(jwt != null) {
			// Validar el formato del token
			if (!jwt.startsWith("Bearer ")) {
				throw new BadCredentialsException("El formato del token es inválido. Debe comenzar con 'Bearer '");
			}
			
			try {
				// Remover el prefijo "Bearer "
				jwt = jwt.substring(7);
				
				if (jwt.trim().isEmpty()) {
					throw new BadCredentialsException("Token vacío");
				}
				
				SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
				
				Claims claims = Jwts.parserBuilder()
					.setSigningKey(key)
					.build()
					.parseClaimsJws(jwt)
					.getBody();
				
				String email = String.valueOf(claims.get("email"));
				if (email == null || email.trim().isEmpty()) {
					throw new BadCredentialsException("El token no contiene el email del usuario");
				}
				
				String authorities = String.valueOf(claims.get("authorities"));
				if (authorities == null || authorities.trim().isEmpty()) {
					throw new BadCredentialsException("El token no contiene los roles del usuario");
				}
				
				List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
				Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, auths);
				
				SecurityContextHolder.getContext().setAuthentication(authentication);
				
			} catch (io.jsonwebtoken.ExpiredJwtException e) {
				throw new BadCredentialsException("El token ha expirado");
			} catch (io.jsonwebtoken.security.SecurityException | io.jsonwebtoken.MalformedJwtException e) {
				throw new BadCredentialsException("Token malformado o firma inválida");
			} catch (Exception e) {
				throw new BadCredentialsException("Error al validar el token: " + e.getMessage());
			}
		}
		
		filterChain.doFilter(request, response);
		
	}


}
