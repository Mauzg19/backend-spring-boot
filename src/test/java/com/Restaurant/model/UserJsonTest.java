package com.Restaurant.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class UserJsonTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void acceptsPasswordOnSignupButNeverSerializesIt() throws Exception {
		String password = "demo-password";
		String request = """
				{
				  "fullName": "Demo User",
				  "email": "demo@example.com",
				  "password": "%s",
				  "role": "ROLE_CUSTOMER"
				}
				""".formatted(password);

		User user = objectMapper.readValue(request, User.class);
		String response = objectMapper.writeValueAsString(user);

		assertEquals(password, user.getPassword());
		assertFalse(response.contains("password"));
		assertFalse(response.contains(password));
	}
}
