package com.example.sfcsapp.EventBus;

import com.example.sfcsapp.Database.CartItem;

public class UpdateItemInCart {
    private CartItem cartItem;

    public UpdateItemInCart(){}

    public UpdateItemInCart(CartItem cartItem) {
        this.cartItem = cartItem;
    }

    public CartItem getCartItem() {
        return cartItem;
    }

    public void setCartItem(CartItem cartItem) {
        this.cartItem = cartItem;
    }
}
