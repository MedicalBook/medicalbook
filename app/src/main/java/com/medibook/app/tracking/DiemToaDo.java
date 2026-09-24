package com.medibook.app.tracking;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "diem_toa_do")
public class DiemToaDo {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public double viDo;      // latitude
    public double kinhDo;    // longitude
    public long thoiGian;    // thời điểm ghi nhận (timestamp)
    public String maHanhTrinh;  // để phân biệt các lần ghi hành trình khác nhau

    public DiemToaDo(double viDo, double kinhDo, long thoiGian, String maHanhTrinh) {
        this.viDo = viDo;
        this.kinhDo = kinhDo;
        this.thoiGian = thoiGian;
        this.maHanhTrinh = maHanhTrinh;
    }
}