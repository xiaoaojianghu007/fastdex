package com.example.fertilizercrm.basic;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import com.example.fertilizercrm.R;
import com.example.fertilizercrm.sdlv.SlideAndDragListView;
import org.json.JSONObject;
import java.lang.reflect.Type;
import butterknife.ButterKnife;
import com.example.fertilizercrm.common.utils.ReflectionUtil;
import com.example.fertilizercrm.common.view.SmartJSONArrayAdapter;

/**
 * listview activity
 */
public abstract class JSONArrayListActivity<VIEWHOLDER> extends BaseActivity {
    protected SlideAndDragListView slv;
    protected ListView lv;
    protected TextView tv_empty;

    protected SmartJSONArrayAdapter adapter;
    protected Type viewHolderType;
    protected abstract int getCellLayoutResId();

    protected int getLayoutResId() {
        return R.layout.activity_listview;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewHolderType = ReflectionUtil.getGenericFirstType(getClass());
        setContentView(getLayoutResId());
        lv = (ListView) findViewById(R.id.lv);
        if (lv != null && (lv instanceof SlideAndDragListView)) {
            slv = (SlideAndDragListView) lv;
        }
        onListViewInit();
        tv_empty = (TextView) findViewById(R.id.tv_empty);

        ButterKnife.bind(this);
        adapter = new SmartJSONArrayAdapter(this,getCellLayoutResId(),(Class<?>)viewHolderType);
        adapter.setViewBinder(new SmartJSONArrayAdapter.ViewBinder() {
            @Override
            public void bindData(int position, Object holder, JSONObject obj) {
                JSONArrayListActivity.this.bindData(position, (VIEWHOLDER) holder, obj);
            }
        });
        lv.setAdapter(adapter);
        if (tv_empty != null) {
            lv.setEmptyView(tv_empty);
        }
    }

    protected void onListViewInit() {

    }

    protected abstract void bindData(int position, VIEWHOLDER holder,JSONObject obj);

    /**
     * 设置没有数据时显示的提示信息
     * @param emptyText
     */
    public void setEmptyText(String emptyText) {
        if (tv_empty != null) {
            tv_empty.setText(emptyText);
        }
    }
}
