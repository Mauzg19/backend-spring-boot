package com.Restaurant.Exception;

import java.io.Serial;

public class RestaurantException extends Exception {


	@Serial
	private static final long serialVersionUID = 1L;

	public RestaurantException(String message) {
		super(message);
		
	}
	

}
