package com.medibook.app.routing;

/**
 * Tính khoảng cách "đường chim bay" giữa 2 toạ độ bằng công thức Haversine.
 * Không cần mạng, không cần API. Dùng để lọc nhanh các cơ sở y tế ở gần.
 */
public class KhoangCach {

    private static final double BAN_KINH_TRAI_DAT_MET = 6371000;

    public static double haversineMet(double viDo1, double kinhDo1, double viDo2, double kinhDo2) {
        double dViDo = Math.toRadians(viDo2 - viDo1);
        double dKinhDo = Math.toRadians(kinhDo2 - kinhDo1);

        double a = Math.sin(dViDo / 2) * Math.sin(dViDo / 2)
                + Math.cos(Math.toRadians(viDo1)) * Math.cos(Math.toRadians(viDo2))
                * Math.sin(dKinhDo / 2) * Math.sin(dKinhDo / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BAN_KINH_TRAI_DAT_MET * c;
    }
}
