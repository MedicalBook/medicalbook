package com.medibook.app.routing;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tìm các bệnh viện THẬT quanh vị trí người dùng bằng dữ liệu OpenStreetMap (Overpass API).
 * - Miễn phí, không cần API key, hoạt động ở bất kỳ đâu (Q10, Thủ Đức, tỉnh khác...).
 * - Chỉ lấy các điểm được đánh dấu amenity=hospital trên OpenStreetMap.
 * - BẮT BUỘC gọi ở luồng nền.
 * - Lưu ý: bệnh viện tìm được ở đây chỉ dùng để CHỈ ĐƯỜNG, không dùng để đặt lịch
 *   (đặt lịch phải dùng bảng benh_vien trên Supabase).
 */
public class TimBenhVienOverpass {

    // 3 server Overpass công cộng: server này lỗi/quá tải thì thử server tiếp theo
    private static final String[] CAC_SERVER = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    };

    public static List<CoSoYTe> timQuanh(GeoPoint viTri, int banKinhMet) throws Exception {
        double lat = viTri.getLatitude();
        double lon = viTri.getLongitude();

        // Tìm bệnh viện dạng điểm (node), toà nhà (way) và khu phức hợp (relation) trong bán kính
        String truyVan = String.format(Locale.US,
                "[out:json][timeout:20];("
                        + "node[\"amenity\"=\"hospital\"](around:%d,%f,%f);"
                        + "way[\"amenity\"=\"hospital\"](around:%d,%f,%f);"
                        + "relation[\"amenity\"=\"hospital\"](around:%d,%f,%f);"
                        + ");out center tags;",
                banKinhMet, lat, lon, banKinhMet, lat, lon, banKinhMet, lat, lon);

        Exception loiCuoi = null;
        for (String server : CAC_SERVER) {
            try {
                return docKetQua(guiTruyVan(server, truyVan));
            } catch (Exception e) {
                loiCuoi = e; // thử server tiếp theo
            }
        }
        throw loiCuoi;
    }

    private static String guiTruyVan(String server, String truyVan) throws Exception {
        HttpURLConnection ketNoi = (HttpURLConnection) URI.create(server).toURL().openConnection();
        ketNoi.setRequestMethod("POST");
        ketNoi.setDoOutput(true);
        ketNoi.setConnectTimeout(10000);
        ketNoi.setReadTimeout(25000);
        ketNoi.setRequestProperty("User-Agent", "MediBook-Android-DoAnSinhVien");
        ketNoi.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try {
            byte[] noiDungGui = ("data=" + URLEncoder.encode(truyVan, "UTF-8"))
                    .getBytes(StandardCharsets.UTF_8);
            try (OutputStream ghi = ketNoi.getOutputStream()) {
                ghi.write(noiDungGui);
            }

            int ma = ketNoi.getResponseCode();
            if (ma != 200) {
                throw new Exception("Overpass trả về mã lỗi " + ma);
            }

            StringBuilder noiDung = new StringBuilder();
            try (BufferedReader doc = new BufferedReader(
                    new InputStreamReader(ketNoi.getInputStream(), StandardCharsets.UTF_8))) {
                String dong;
                while ((dong = doc.readLine()) != null) {
                    noiDung.append(dong);
                }
            }
            return noiDung.toString();
        } finally {
            ketNoi.disconnect();
        }
    }

    private static List<CoSoYTe> docKetQua(String json) throws Exception {
        JSONArray cacPhanTu = new JSONObject(json).getJSONArray("elements");
        List<CoSoYTe> ketQua = new ArrayList<>();
        Set<String> daCo = new HashSet<>(); // tránh 1 bệnh viện bị trùng (vừa là điểm vừa là toà nhà)

        for (int i = 0; i < cacPhanTu.length(); i++) {
            JSONObject phanTu = cacPhanTu.getJSONObject(i);
            JSONObject the = phanTu.optJSONObject("tags");
            if (the == null) continue;

            String ten = the.optString("name", "");
            if (ten.isEmpty()) ten = the.optString("name:vi", "");
            if (ten.isEmpty()) continue;           // bỏ qua điểm không có tên
            if (!daCo.add(ten)) continue;          // bỏ qua tên trùng

            // node có lat/lon trực tiếp, way/relation có toạ độ tâm trong "center"
            double lat, lon;
            if (phanTu.has("lat")) {
                lat = phanTu.getDouble("lat");
                lon = phanTu.getDouble("lon");
            } else if (phanTu.has("center")) {
                JSONObject tam = phanTu.getJSONObject("center");
                lat = tam.getDouble("lat");
                lon = tam.getDouble("lon");
            } else {
                continue;
            }

            ketQua.add(new CoSoYTe(ten, lat, lon));
        }
        return ketQua;
    }
}
