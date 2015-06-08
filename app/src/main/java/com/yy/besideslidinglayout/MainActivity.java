package com.yy.besideslidinglayout;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    private Button leftBtn;
    private Button rightBtn;
    private TextView leftText;
    private EditText rightEdit;
    private ListView contentList;
    private BesideSlidingLayout besideSlidingLayout;
    private ArrayAdapter<String> contentListAdapter;
    private ArrayList<String> contentItems = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        besideSlidingLayout = (BesideSlidingLayout) findViewById(R.id.bidir_slide_layout);
        contentList = (ListView) findViewById(R.id.contentList);
        contentListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                contentItems);
        contentList.setAdapter(contentListAdapter);
        besideSlidingLayout.setScrollEvent(contentList);
    }

    private void addItem() {
        String rightText = rightEdit.getText().toString();
        if (!rightText.equals("")) {
        contentItems.add(rightText);
        contentListAdapter.notifyDataSetChanged();
        rightEdit.setText("");
        besideSlidingLayout.scrollToContentMenuFromRightMenu();
        }
    }

    private void changeLeftText() {
        leftText.setText("you click on the left button");
    }

    private void initView() {
        String[] initString = {"a", "v", "b", "sd", "a", "v", "b", "sd","a", "v", "b", "sd"};
        rightBtn = (Button) findViewById(R.id.right_btn);
        leftBtn = (Button) findViewById(R.id.left_btn);
        rightEdit = (EditText) findViewById(R.id.right_edit);
        leftText = (TextView) findViewById(R.id.left_text);

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });
        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLeftText();
            }
        });

        for(int i = 0; i < initString.length; i++) {
         contentItems.add(initString[i]);
        }
    }
}
