package com.medibook.app.tracking;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DiemToaDoDao {

    @Insert
    void themDiem(DiemToaDo diem);

    @Query("SELECT * FROM diem_toa_do WHERE maHanhTrinh = :maHanhTrinh ORDER BY thoiGian ASC")
    List<DiemToaDo> layTatCaDiemTheoHanhTrinh(String maHanhTrinh);

    @Query("DELETE FROM diem_toa_do WHERE maHanhTrinh = :maHanhTrinh")
    void xoaHanhTrinh(String maHanhTrinh);

    @Query("SELECT maHanhTrinh FROM diem_toa_do ORDER BY thoiGian DESC LIMIT 1")
    String layMaHanhTrinhGanNhat();
}