package com.example.coolweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    /**
     * 1.创建视图
     * 2.定义方法，读取数据
     * 3。显示数据
     * 4.
     */
    private TextView titleText;
    private Button back_btn;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> datalist = new ArrayList<>();//存放当前列表需要显示的数据
    private Province currentProvince;//当前的省
    private City currentCity;
    private List<Province> provinceList;//从数据库中读取的省数据
    private List<City> cityList;//从数据库中读取的市数据
    private List<County> countyList;
    private int currentLevel;//当前访问数据类型，取值对应LEVEL_PROVINCE，LEVEL_CITY，LEVEL_COUNTY

    private ProgressDialog progressDialog;//进度对话框

    /**
     * 在onCreateView()方法里，首先获取控件实例，然后初始化一个适配器，用于传递数据给listview并显示
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //动态加载layout文件
        View view = inflater.inflate(R.layout.choose_area, container, false);
        //获取控件实例
        titleText = view.findViewById(R.id.title_text);
        back_btn = view.findViewById(R.id.back_btn);
        listView = view.findViewById(R.id.list_view);
        //listview无法直接加载数据，需要定义适配器进行数据传递
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, datalist);

        listView.setAdapter(adapter);

        return view;
    }
    //数据库没有数据，去服务器请求数据，存放到数据库中，再读取显示


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        requireActivity().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event.getTargetState() == Lifecycle.State.CREATED) {
                    queryPronvince();
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            if (currentLevel == LEVEL_PROVINCE) {
                                //当前显示省级数据，点击后显示对应市级数据
                                currentProvince = provinceList.get(i);
                                queryCity();
                            } else if (currentLevel == LEVEL_CITY) {
                                //当前显示市级数据，点击后显示对应县级数据
                                currentCity = cityList.get(i);
                                queryCity();
                            }
                        }
                    });

                    //点击返回键，返回上级目录
                    back_btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (currentLevel == LEVEL_COUNTY) {
                                queryCity();
                            } else if (currentLevel == LEVEL_CITY) {
                                queryPronvince();
                            }
                        }
                    });

                    requireActivity().getLifecycle().removeObserver(this);
                }
            }
        });

    }


    public void queryFromService(String address, String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                //响应失败时，需要执行操作
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //关闭进度对话框
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }


            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //响应成功，需要执行的操作
                String responseText = response.body().string();
                //判断当前服务器响应的数据类型，并调用相应的数据处理方法进行数据解析和存储
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);//获取省数据，并存入数据表
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, currentProvince.getProvinceId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, currentCity.getCityId());
                }

                //判断处理结果
                if (result) {
                    //对UI进行操作，需要切换到主线程去调用界面方法
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //关闭进度对话框
                            closeProgressDialog();
                            //已经完成了数据处理和存储，则加载数据并显示。
                            if ("province".equals(type)) {
                                queryPronvince();//读取数据库，显示数据
                            } else if ("city".equals(type)) {
                                queryCity();
                            } else if ("county".equals(type)) {
                                queryCounty();
                            }
                        }
                    });
                }
            }
        });
    }

    //数据库中有数据，直接读取并显示
    public void queryPronvince() {
        titleText.setText("中国");
        back_btn.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);
        //数据表非空
        if (provinceList.size() > 0) {
            datalist.clear();
            for (Province province : provinceList) {
                datalist.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);//初始化，默认选择列表中的的一个省

            //更新当前访问状态
            currentLevel = LEVEL_PROVINCE;

            //数据表非空，则获取数据并显示
        } else {
            //数据表非空，则从服务器中获取数据
            String address = "http://guolin.tech/api/china";
            queryFromService(address, "province");
        }
    }

    public void queryCity() {
        titleText.setText(currentProvince.getProvinceName());
        back_btn.setVisibility(View.VISIBLE);//显示返回
        cityList = LitePal.where("provinceId = ?", String.valueOf(currentProvince.getProvinceId())).find(City.class);//返回currentProvince下面所有的市
        //cityList = LitePal.findAll(City.class);//返回所有市级数据
        //数据表非空，则获取数据并显示
        if (cityList.size() > 0) {
            datalist.clear();
            for (City city : cityList) {
                datalist.add(city.getCityName());
            }
            //数据显示
            adapter.notifyDataSetChanged();
            listView.setSelection(0);//默认选中第一个市

            currentLevel = LEVEL_CITY;
        } else {
            //数据表为空，则访问服务器存储数据
            int provinceId = currentProvince.getProvinceId();
            String address = "http://guolin.tech/api/china/" + provinceId;
            queryFromService(address, "city");

        }

    }

    public void queryCounty() {
        titleText.setText(currentCity.getCityName());
        back_btn.setVisibility(View.VISIBLE);
        //获取数据表信息
        countyList = LitePal.where("cityId = ?", String.valueOf(currentCity.getCityId())).find(County.class);//返回currentProvince下面所有的市
        //countyList = LitePal.findAll(County.class);
        if (countyList.size() > 0) {
            datalist.clear();
            for (County county : countyList) {
                datalist.add(county.getCountyName());
            }
            //数据显示
            adapter.notifyDataSetChanged();
            listView.setSelection(0);//默认选中第一个市

            //更新当前访问状态
            currentLevel = LEVEL_COUNTY;

        } else {
            //数据表为空，则访问服务器存储数据
            int provinceId = currentProvince.getProvinceId();
            int cityId = currentCity.getCityId();
            String address = "http://guolin.tech/api/china/" + provinceId + "/" + cityId;
            queryFromService(address, "county");

        }
    }

    //加载数据时，显示进度对话框
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载中。。。");
            progressDialog.setCanceledOnTouchOutside(false);//用户点击对话框以外，对话框不显示
        }
        progressDialog.show();

    }

    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
