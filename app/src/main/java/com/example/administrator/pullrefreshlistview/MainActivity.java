package com.example.administrator.pullrefreshlistview;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private List<String> list;
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final RefreshableView listView=(RefreshableView)findViewById(R.id.list);
        list=new ArrayList<>();
        for (int i=0;i<15;i++){
            list.add("Hello!");
        }
        adapter=new BaseAdapter() {
            @Override
            public int getCount() {
                return list.size();
            }

            @Override
            public Object getItem(int i) {
                return list.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view==null){
                    view=LayoutInflater.from(MainActivity.this).inflate(R.layout.item, viewGroup, false);
                }
                ((TextView)view.findViewById(R.id.text)).setText(list.get(i));
                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setPullDownRefreshListener(new RefreshableView.pullDownRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 5; i++) {
                            list.add("haha!");
                        }
                        adapter.notifyDataSetChanged();
                        listView.finishRefresh();
                    }
                }, 3000);
            }
        });
        listView.setPullUpRefreshListener(new RefreshableView.pullUpRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 5; i++) {
                            list.add("haha!");
                        }
                        adapter.notifyDataSetChanged();
                        listView.finishRefresh();
                    }
                }, 3000);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
