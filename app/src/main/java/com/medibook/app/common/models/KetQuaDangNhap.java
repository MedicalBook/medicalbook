package com.medibook.app.common.models;

import com.google.gson.annotations.SerializedName;

public class KetQuaDangNhap {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("user")
    private NguoiDung user;

    public String getAccessToken() {
        return accessToken;
    }

    public NguoiDung getUser() {
        return user;
    }

    // Class lồng bên trong, đại diện cho object "user" trong phản hồi
    public static class NguoiDung {
        @SerializedName("id")
        private String id;

        @SerializedName("email")
        private String email;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }
    }
}