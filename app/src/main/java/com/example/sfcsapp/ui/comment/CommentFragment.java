package com.example.sfcsapp.ui.comment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sfcsapp.Adapter.MyCommentAdapter;
import com.example.sfcsapp.Callback.ICommentCallBackListener;
import com.example.sfcsapp.Common.Common;
import com.example.sfcsapp.Model.CommentModel;
import com.example.sfcsapp.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class CommentFragment extends BottomSheetDialogFragment implements ICommentCallBackListener {

    private CommentViewModel commentViewModel;
    private Unbinder unbinder;
    @BindView(R.id.recycler_comment)
    RecyclerView recycler_comment;

    AlertDialog dialog;
    ICommentCallBackListener listener;

    public CommentFragment() {
        listener = this;
    }

    private static CommentFragment instance;

    public static CommentFragment getInstance(){
        if (instance == null){
            instance = new CommentFragment();
        }
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_comment_fragment, container, false);
        unbinder = ButterKnife.bind(this, itemView);
        initViews();
        loadCommentFromFirebase();
        commentViewModel.getMutableLiveDataFoodList().observe(this, commentModels -> {
            MyCommentAdapter adapter = new MyCommentAdapter(getContext(), commentModels);
            recycler_comment.setAdapter(adapter);
        });
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void loadCommentFromFirebase() {
        dialog.show();
        List<CommentModel> commentModels = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REF).child(Common.selectedFood.getId()).orderByChild("commentTimeStamp")
                .limitToLast(100).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot commentSnapShot:dataSnapshot.getChildren()){
                    CommentModel commentModel = commentSnapShot.getValue(CommentModel.class);
                    commentModels.add(commentModel);
                }
                listener.onCommentLoadSuccess(commentModels);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onCommentLoadFailed(databaseError.getMessage());
            }
        });
    }

    private void initViews() {
        commentViewModel = new ViewModelProvider(this).get(CommentViewModel.class);
        dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        recycler_comment.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),RecyclerView.VERTICAL, true);
        recycler_comment.setLayoutManager(layoutManager);
        recycler_comment.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
    }

    @Override
    public void onCommentLoadSuccess(List<CommentModel> commentModel) {
        dialog.dismiss();
        commentViewModel.setCommentList(commentModel);
    }

    @Override
    public void onCommentLoadFailed(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }
}
