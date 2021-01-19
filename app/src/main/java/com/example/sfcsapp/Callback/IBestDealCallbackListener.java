package com.example.sfcsapp.Callback;

import com.example.sfcsapp.Model.BestDealModel;
import com.example.sfcsapp.Model.PopularCategoryModel;

import java.util.List;

public interface IBestDealCallbackListener {
    void onBestDealSuccess(List<BestDealModel> bestDealModels);
    void onBestDealLoadFailed(String message);
}
