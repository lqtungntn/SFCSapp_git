package com.example.sfcsapp.Callback;

import com.example.sfcsapp.Model.CommentModel;

import java.util.List;

public interface ICommentCallBackListener {
    void onCommentLoadSuccess(List<CommentModel> commentModel);
    void onCommentLoadFailed(String message);
}
