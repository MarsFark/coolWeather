package com.example.coolweather.util;

import android.text.TextUtils;

import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class Utility {

    //处理服务器返回的省级数据，存储到数据库中
    public static boolean handleProvinceResponse(String response){
        //判断响应值是否为空，为空则进行处理并返回true，
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allProvinces = new JSONArray(response);
                //遍历所有省级数据，并存储到数据库中
                for (int i = 0;i < allProvinces.length();i++){
                    JSONObject provinceObjet = allProvinces.getJSONObject(i);
                    Province province = new Province();

                    province.setProvinceId(provinceObjet.getInt("id"));
                    province.setProvinceName(provinceObjet.getString("name"));

                    //执行save()方法，将数据存储到数据表里面
                    province.save();
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    //处理服务器返回的
    public static boolean handleCityResponse(String response,int provinceId){
        //判断服务器返回的
        if (!TextUtils.isEmpty(response)){
            try{
                JSONArray allCities = new JSONArray(response);
                for (int i = 0;i < allCities.length();i++){
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();

                    city.setCityId(cityObject.getInt("id"));
                    city.setCityName(cityObject.getString("name"));
                    city.setProvinceId(provinceId);

                    city.save();
                }
            }catch (JSONException e){
                e.printStackTrace();
                //遍历所有省级数据，并存储到数据库中
            }
            return true;
        }
        return false;
    }
    public static boolean handleCountyResponse(String response,int cityId){
        if (!TextUtils.isEmpty(response)){
            try{
                JSONArray allCounties = new JSONArray(response);
                //遍历所有物品，并存储到数据库中
                for (int i = 0;i < allCounties.length();i++){
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();

                    county.setCountyName(countyObject.getString("name"));
                    county.setCityId(countyObject.getInt("id"));
                    county.setCityId(cityId);


                    county.save();
                }
            }catch (JSONException e){
                e.printStackTrace();
                //遍历所有省级数据，并存储到数据库中
            }
            return true;
        }
        return false;
    }

}
