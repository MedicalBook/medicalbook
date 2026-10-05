package com.medibook.app.routing;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gọi OSRM (Open Source Routing Machine) để tìm đường đi NGẮN NHẤT theo đường phố thật.
 * - Miễn phí, không cần API key.
 * - Server demo router.project-osrm.org tính theo xe (driving), xe máy gần tương đương.
 * - Server demo chỉ dùng cho học tập / thử nghiệm, không dùng cho sản phẩm thật.
 * - BẮT BUỘC gọi ở luồng nền (không gọi trên main thread).
 */
public class TimDuongOsrm {

    private static final String DIA_CHI_OSRM = "https://router.project-osrm.org/route/v1/driving/";

    public static class KetQua {
        public final List<GeoPoint> cacDiem;   // các điểm tạo thành tuyến đường
        public final double quangDuongMet;     // tổng quãng đường (mét)
        public final double thoiGianGiay;      // thời gian ước tính (giây)

        public KetQua(List<GeoPoint> cacDiem, double quangDuongMet, double thoiGianGiay) {
            this.cacDiem = cacDiem;
            this.quangDuongMet = quangDuongMet;
            this.thoiGianGiay = thoiGianGiay;
        }
    }

    public static KetQua timDuong(GeoPoint tu, GeoPoint den) throws Exception {
        // OSRM nhận toạ độ theo thứ tự KINH ĐỘ, VĨ ĐỘ (ngược với thói quen)
        String url = String.format(Locale.US,
                "%s%f,%f;%f,%f?overview=full&geometries=geojson",
                DIA_CHI_OSRM,
                tu.getLongitude(), tu.getLatitude(),
                den.getLongitude(), den.getLatitude());

        HttpURLConnection ketNoi = (HttpURLConnection) URI.create(url).toURL().openConnection();
        ketNoi.setConnectTimeout(10000);
        ketNoi.setReadTimeout(10000);
        ketNoi.setRequestProperty("User-Agent", "MediBook-Android-DoAnSinhVien");

        try {
            int ma = ketNoi.getResponseCode();
            if (ma != 200) {
                throw new Exception("OSRM trả về mã lỗi " + ma);
            }

            StringBuilder noiDung = new StringBuilder();
            try (BufferedReader doc = new BufferedReader(
                    new InputStreamReader(ketNoi.getInputStream(), StandardCharsets.UTF_8))) {
                String dong;
                while ((dong = doc.readLine()) != null) {
                    noiDung.append(dong);
                }
            }

            JSONObject json = new JSONObject(noiDung.toString());
            if (!"Ok".equals(json.optString("code"))) {
                throw new Exception("Không tìm được đường: " + json.optString("message"));
            }

            // OSRM trả về tuyến ngắn nhất ở vị trí đầu tiên
            JSONObject tuyen = json.getJSONArray("routes").getJSONObject(0);
            double quangDuong = tuyen.getDouble("distance");
            double thoiGian = tuyen.getDouble("duration");

            JSONArray toaDo = tuyen.getJSONObject("geometry").getJSONArray("coordinates");
            List<GeoPoint> cacDiem = new ArrayList<>();
            for (int i = 0; i < toaDo.length(); i++) {
                JSONArray diem = toaDo.getJSONArray(i);
                cacDiem.add(new GeoPoint(diem.getDouble(1), diem.getDouble(0))); // [kinhDo, viDo]
            }

            return new KetQua(cacDiem, quangDuong, thoiGian);
        } finally {
            ketNoi.disconnect();
        }
    }
}
