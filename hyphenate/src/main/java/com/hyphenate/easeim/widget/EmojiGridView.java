package com.hyphenate.easeim.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.adapter.EmojiGridAdapter;
import com.hyphenate.easeim.domain.EaseDefaultEmojiconDatas;
import com.hyphenate.easeim.interfaces.EmojiViewlistener;

public class EmojiGridView extends LinearLayout {

    private Context context;

    private GridView gridView;
    private EmojiViewlistener emojiViewlistener;

    public EmojiGridView(Context context) {
        this(context, null);
    }

    public EmojiGridView(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public EmojiGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.emoji_grid_view, this);
        initView();
    }

    private void initView() {
        gridView = findViewById(R.id.emoji_grid);
        gridView.setNumColumns(8);
        EmojiGridAdapter adapter = new EmojiGridAdapter(context, 1, EaseDefaultEmojiconDatas.getData());
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                emojiViewlistener.onEmojiItemClick(adapter.getItem(i));
            }
        });
    }

    public void setEmojiViewlistener(EmojiViewlistener emojiViewlistener){
        this.emojiViewlistener = emojiViewlistener;
    }

}
