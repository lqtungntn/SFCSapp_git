package com.example.sfcsapp.Remote;

import com.example.sfcsapp.Model.BraintreeToken;
import com.example.sfcsapp.Model.BraintreeTransaction;

import io.reactivex.Observable;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface ICloudFunctions {
    @GET("token")
    Observable<BraintreeToken> getToken();

    @POST("checkout")
    @FormUrlEncoded
    Observable<BraintreeTransaction> submitPayment(@Field("amount") double amount,
                                                   @Field("payment_method_nonce") String nonce);
}
