package cn.cbb.dc.andriod;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.cbb.dc.andriod.db.City;
import cn.cbb.dc.andriod.db.County;
import cn.cbb.dc.andriod.db.Province;
import cn.cbb.dc.andriod.util.HttpUtil;
import cn.cbb.dc.andriod.util.Utility;
import dc.cbb.cn.democoolapp.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by hanjun on 2017/10/1.
 */
public class ChooseAreaFragment extends Fragment {

    public static  final  int LEVEL_PROVINCE =0;
    public  static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;
    public static final String GUOLIN_ADDR="http://guolin.tech/api/china";
    private ProgressDialog progressDialog;
    private TextView tilteText;
    private Button backButton;
    private ListView listView;

    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<String>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (currentLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCounties();
                }
                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (currentLevel==LEVEL_COUNTY){
                            queryCities();
                        }else if(currentLevel==LEVEL_CITY){
                            queryProvinces();
                        }
                    }
                });
            }
        });
        queryProvinces();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area_layout,container,false);
        tilteText=view.findViewById(R.id.title_text);
        backButton=view.findViewById(R.id.back_button);
        listView=view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        Log.i("s","=========================queryprovinces----");
        return view;
    }


    private void queryCounties() {
        tilteText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList=DataSupport.where("cityid=?",String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size()>0){
            dataList.clear();
            for (County county:countyList){
                adapter.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address=GUOLIN_ADDR+"/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }
    }

    private void queryProvinces() {
       Log.i("INFO","query province。。。");
        tilteText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address=GUOLIN_ADDR;
            queryFromServer(address,"province");
        }
    }

    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if ("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if ("county".equals(type)){
                    result= Utility.handlerCountyResponse(responseText,selectedCity.getId());
                }else if ("city".equals(type)){
                    result= Utility.handlerCityResponse(responseText,selectedProvince.getId());
                }

                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvinces();
                            }else if("county".equals(type)){
                                queryCounties();
                            }else if("city".equals(type)){
                                queryCities();
                            }
                        }
                    });
                }
            }
        });
    }

    private void closeProgressDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if (progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }

    private void queryCities() {
        tilteText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            String address=GUOLIN_ADDR+"/"+provinceCode;
            queryFromServer(address,"city");
        }
    }
}
