package io.agora.edu.common.bean.board;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

public class BoardInfo implements Parcelable {
    private String boardAppId;
    private String boardId;
    private String boardToken;

    public BoardInfo() {
    }

    public BoardInfo(String boardAppId, String boardId, String boardToken) {
        this.boardAppId = boardAppId;
        this.boardId = boardId;
        this.boardToken = boardToken;
    }

    public String getBoardAppId() {
        return boardAppId;
    }

    public void setBoardAppId(String boardAppId) {
        this.boardAppId = boardAppId;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public String getBoardToken() {
        return boardToken;
    }

    public void setBoardToken(String boardToken) {
        this.boardToken = boardToken;
    }

    protected BoardInfo(Parcel in) {
        boardAppId = in.readString();
        boardId = in.readString();
        boardToken = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(boardAppId);
        dest.writeString(boardId);
        dest.writeString(boardToken);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BoardInfo> CREATOR = new Creator<BoardInfo>() {
        @Override
        public BoardInfo createFromParcel(Parcel in) {
            return new BoardInfo(in);
        }

        @Override
        public BoardInfo[] newArray(int size) {
            return new BoardInfo[size];
        }
    };
}
