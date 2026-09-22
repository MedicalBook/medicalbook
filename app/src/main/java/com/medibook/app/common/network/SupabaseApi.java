package com.medibook.app.common.network;

import com.medibook.app.common.models.HoSo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

import com.medibook.app.common.models.KetQuaDangNhap;
import java.util.Map;
public interface SupabaseApi {

    // Lấy danh sách hồ sơ (có thể lọc theo điều kiện qua Query)
    // Ví dụ: GET /rest/v1/ho_so?id=eq.<id_can_tim>
    @GET("rest/v1/ho_so")
    Call<List<HoSo>> layHoSo(@Query("id") String idFilter);

    // Tạo mới 1 hồ sơ (dùng khi đăng ký tài khoản)
    @Headers("Prefer: return=representation")
    @POST("rest/v1/ho_so")
    Call<List<HoSo>> taoHoSo(@Body HoSo hoSo);

    // Đăng nhập bằng email + mật khẩu
    @POST("auth/v1/token?grant_type=password")
    Call<KetQuaDangNhap> dangNhap(@Body Map<String, String> thongTin);

    // Đăng ký tài khoản mới (tạo user trong hệ thống Auth)
    @POST("auth/v1/signup")
    Call<KetQuaDangNhap> dangKy(@Body Map<String, String> thongTin);
}