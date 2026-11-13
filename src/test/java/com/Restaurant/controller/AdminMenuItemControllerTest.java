package com.Restaurant.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.Restaurant.model.Food;
import com.Restaurant.domain.USER_ROLE;
import com.Restaurant.model.User;
import com.Restaurant.service.FoodService;
import com.Restaurant.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminMenuItemControllerTest {
	@Mock
	private FoodService menuItemService;

	@Mock
	private ProductImageStorage productImageStorage;
	@Mock
	private UserService userService;

	@InjectMocks
	private AdminMenuItemController controller;

	@Test
	void uploadProductImagePersistsImageUrlOnFood() throws Exception {
		Long productId = 42L;
		String imageUrl = "/uploads/product-images/product.jpg";
		MockMultipartFile image = new MockMultipartFile(
				"image", "product.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
		when(menuItemService.findFoodById(productId)).thenReturn(new Food());
		when(productImageStorage.save(image)).thenReturn(imageUrl);
		User administrator = new User();
		administrator.setRole(USER_ROLE.ROLE_ADMIN);
		when(userService.findUserProfileByJwt("test-jwt")).thenReturn(administrator);

		ResponseEntity<Map<String, String>> response = controller.uploadProductImage(productId, image, "test-jwt");

		assertEquals(Map.of("url", imageUrl), response.getBody());
		verify(menuItemService).findFoodById(productId);
		verify(menuItemService).updateFood(
				eq(productId), isNull(), isNull(), isNull(), eq(List.of(imageUrl)), isNull());
	}
}
