package com.Restaurant.service;

import com.Restaurant.Exception.CartException;
import com.Restaurant.Exception.CartItemException;
import com.Restaurant.Exception.FoodException;
import com.Restaurant.Exception.UserException;
import com.Restaurant.model.Cart;
import com.Restaurant.model.CartItem;
import com.Restaurant.request.AddCartItemRequest;

public interface CartSerive {

	public CartItem addItemToCart(AddCartItemRequest req, String jwt) throws UserException, FoodException, CartException, CartItemException;

	public CartItem updateCartItemQuantity(Long cartItemId,int quantity) throws CartItemException;

	public Cart removeItemFromCart(Long cartItemId, String jwt) throws UserException, CartException, CartItemException;

	public Long calculateCartTotals(Cart cart) throws UserException;
	
	public Cart findCartById(Long id) throws CartException;
	
	public Cart findCartByUserId(Long userId) throws CartException, UserException;
	
	public Cart clearCart(Long userId) throws CartException, UserException;
	

	

}
