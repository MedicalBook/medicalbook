package com.medibook.app.routing;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Một bệnh viện / phòng khám có toạ độ.
 * TẠM THỜI dùng dữ liệu mẫu (toạ độ gần đúng). Sau này lấy từ bảng benh_vien trên Supabase
 * (cần thêm 2 cột vi_do, kinh_do).
 */
public class CoSoYTe {

    public final String ten;
    public final double viDo;
    public final double kinhDo;

    public CoSoYTe(String ten, double viDo, double kinhDo) {
        this.ten = ten;
        this.viDo = viDo;
        this.kinhDo = kinhDo;
    }

    public GeoPoint viTri() {
        return new GeoPoint(viDo, kinhDo);
    }

    // Dữ liệu mẫu quanh khu vực Quận 10 / Quận 5 / Tân Bình, TP.HCM (toạ độ gần đúng)
    public static List<CoSoYTe> danhSachMau() {
        List<CoSoYTe> ds = new ArrayList<>();
        ds.add(new CoSoYTe("Bệnh viện Chợ Rẫy", 10.7577, 106.6595));
        ds.add(new CoSoYTe("Bệnh viện Nhân dân 115", 10.7755, 106.6671));
        ds.add(new CoSoYTe("Bệnh viện Trưng Vương", 10.7719, 106.6582));
        ds.add(new CoSoYTe("Bệnh viện Đại học Y Dược", 10.7553, 106.6641));
        ds.add(new CoSoYTe("Bệnh viện Nhi Đồng 1", 10.7684, 106.6702));
        ds.add(new CoSoYTe("Bệnh viện Thống Nhất", 10.7919, 106.6531));
        return ds;
    }

    // Lấy N cơ sở gần nhất theo đường chim bay (Haversine) trong danh sách mẫu
    public static List<CoSoYTe> ganNhat(GeoPoint viTri, int soLuong) {
        return ganNhat(danhSachMau(), viTri, soLuong);
    }

    // Lấy N cơ sở gần nhất theo đường chim bay (Haversine) trong 1 danh sách bất kỳ
    public static List<CoSoYTe> ganNhat(List<CoSoYTe> danhSach, GeoPoint viTri, int soLuong) {
        List<CoSoYTe> ds = new ArrayList<>(danhSach);
        ds.sort((a, b) -> Double.compare(
                KhoangCach.haversineMet(viTri.getLatitude(), viTri.getLongitude(), a.viDo, a.kinhDo),
                KhoangCach.haversineMet(viTri.getLatitude(), viTri.getLongitude(), b.viDo, b.kinhDo)));
        return new ArrayList<>(ds.subList(0, Math.min(soLuong, ds.size())));
    }
}
