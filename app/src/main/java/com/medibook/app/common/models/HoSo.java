package com.medibook.app.common.models;

import com.google.gson.annotations.SerializedName;

public class HoSo {

    @SerializedName("id")
    private String id;

    @SerializedName("ho_ten")
    private String hoTen;

    @SerializedName("vai_tro")
    private String vaiTro;

    @SerializedName("so_dien_thoai")
    private String soDienThoai;

    @SerializedName("anh_dai_dien")
    private String anhDaiDien;

    @SerializedName("dia_chi_mac_dinh")
    private String diaChiMacDinh;

    @SerializedName("ngay_sinh")
    private String ngaySinh;

    @SerializedName("gioi_tinh")
    private String gioiTinh;

    // Constructor rỗng - bắt buộc phải có để Gson hoạt động
    public HoSo() {
    }

    // Getters và Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    public String getDiaChiMacDinh() {
        return diaChiMacDinh;
    }

    public void setDiaChiMacDinh(String diaChiMacDinh) {
        this.diaChiMacDinh = diaChiMacDinh;
    }

    public String getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(String ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(String gioiTinh) {
        this.gioiTinh = gioiTinh;
    }
}