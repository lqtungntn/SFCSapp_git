package com.example.sfcsapp.Callback;

import com.example.sfcsapp.Model.CategoryModel;

import java.util.List;

public interface ICatergoryCallBackListener {
    void onCategoryLoadSuccess(List<CategoryModel> categoryModelsList);
    void onCategoryLoadFailed(String message);
}
