package com.example.sfcsapp.Adapter;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.asksira.loopingviewpager.LoopingPagerAdapter;
import com.asksira.loopingviewpager.LoopingViewPager;
import com.bumptech.glide.Glide;
import com.example.sfcsapp.EventBus.BestDealItemClick;
import com.example.sfcsapp.Model.BestDealModel;
import com.example.sfcsapp.R;

import org.greenrobot.eventbus.EventBus;

import java.net.CookieHandler;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyBestDealAdapter extends LoopingPagerAdapter<BestDealModel> {

    @BindView(R.id.img_best_deal)
    ImageView img_best_deal;
    @BindView(R.id.txt_best_deal)
    TextView txt_best_deal;

    Unbinder unbinder;

    public MyBestDealAdapter(Context context, List<BestDealModel> itemList, boolean isInfinite) {
        super(context, itemList, isInfinite);
    }

    @Override
    protected void bindView(View convertView, int ListPosition, int viewType) {
        unbinder = ButterKnife.bind(this, convertView);
        Glide.with(convertView).load(getItemList().get(ListPosition).getImage()).into(img_best_deal);
        txt_best_deal.setText(getItemList().get(ListPosition).getName());
        convertView.setOnClickListener(view -> EventBus.getDefault().postSticky(new BestDealItemClick(getItemList().get(ListPosition))));
    }

    @Override
    protected View inflateView(int viewType, ViewGroup container, int listPosition) {
        return LayoutInflater.from(getContext()).inflate(R.layout.layout_best_deal_item,container,false);
    }
}
