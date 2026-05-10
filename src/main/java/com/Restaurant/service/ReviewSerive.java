package com.Restaurant.service;

import java.util.List;

import com.Restaurant.Exception.ReviewException;
import com.Restaurant.model.Review;
import com.Restaurant.model.User;
import com.Restaurant.request.ReviewRequest;

public interface ReviewSerive {
	
    public Review submitReview(ReviewRequest review,User user);
    public void deleteReview(Long reviewId) throws ReviewException;
    public double calculateAverageRating(List<Review> reviews);
}
