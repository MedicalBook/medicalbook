package com.medibook.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.medibook.app.routing.CoSoYTe;
import com.medibook.app.routing.KhoangCach;
import com.medibook.app.routing.TimBenhVienOverpass;
import com.medibook.app.routing.TimDuongOsrm;
import com.medibook.app.tracking.AppDatabase;
import com.medibook.app.tracking.DiemToaDo;
import com.medibook.app.tracking.DiemToaDoDao;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TheoDoiHanhTrinhActivity extends AppCompatActivity {

    private static final int MA_YEU_CAU_QUYEN_VI_TRI = 100;
    private static final String TAG = "MediBook"; // lọc Logcat bằng: tag:MediBook
    private static final int SO_UNG_VIEN_GAN_NHAT = 3; // số bệnh viện gần nhất (chim bay) đem đi so đường thật

    private MapView mapView;
    private MyLocationNewOverlay lopViTriHienTai;
    private Button btnBatDau, btnDung, btnTimDuong, btnXoa;
    private TextView tvTrangThai;

    // Ghi hành trình (lưu vết)
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Polyline duongDi;                 // đường THỰC TẾ đã đi (xanh)
    private DiemToaDoDao dao;
    private final ExecutorService luongNen = Executors.newSingleThreadExecutor(); // luồng nền cho Room
    private String maHanhTrinhHienTai;
    private boolean dangGhi = false;
    private int soDiem = 0;

    // Theo dõi mạng
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback theoDoiMang;
    private boolean coMang = true;

    // MỚI: tìm đường ngắn nhất
    private Polyline duongGoiY;               // đường NGẮN NHẤT gợi ý (cam)
    private Marker markerDich;                // điểm đến
    private final ExecutorService luongMang = Executors.newSingleThreadExecutor(); // luồng nền gọi OSRM
    private boolean dangTimDuong = false;

    // MỚI: bệnh viện thật quanh vị trí người dùng (OpenStreetMap)
    private static final int BAN_KINH_TIM_MET = 5000;       // tìm trong 5 km trước
    private static final int BAN_KINH_MO_RONG_MET = 15000;  // không có thì mở rộng 15 km
    private static final int SO_MARKER_TOI_DA = 20;         // chỉ hiện 20 bệnh viện gần nhất cho đỡ rối
    private final List<Marker> cacMarkerBenhVien = new ArrayList<>();
    private volatile boolean dangDungDuLieuMau = false;
    // SỬA: ghi nhớ danh sách bệnh viện đã tải thành công để dùng lại
    private static final int BAN_KINH_DUNG_LAI_MET = 2000;
    private volatile List<CoSoYTe> boNhoBenhVien = null;
    private volatile GeoPoint viTriBoNho = null;

    // MỚI: ghi nhớ hành trình đang ghi dở (để mở lại app thì vẽ tiếp, còn đã Dừng thì bản đồ sạch)
    private static final String KHOA_MA_DANG_GHI = "ma_hanh_trinh_dang_ghi";
    private SharedPreferences boNho;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_theo_doi_hanh_trinh);

        mapView = findViewById(R.id.mapView);
        btnBatDau = findViewById(R.id.btnBatDau);
        btnDung = findViewById(R.id.btnDung);
        btnTimDuong = findViewById(R.id.btnTimDuong);
        btnXoa = findViewById(R.id.btnXoa);
        boNho = getSharedPreferences("hanh_trinh", MODE_PRIVATE);
        tvTrangThai = findViewById(R.id.tvTrangThai);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);

        dao = AppDatabase.getInstance(this).diemToaDoDao();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // MỚI: đường gợi ý (cam) thêm TRƯỚC để nằm dưới đường thực tế (xanh)
        duongGoiY = new Polyline(mapView);
        duongGoiY.getOutlinePaint().setColor(Color.parseColor("#F97316"));
        duongGoiY.getOutlinePaint().setStrokeWidth(14f);
        mapView.getOverlays().add(duongGoiY);

        duongDi = new Polyline(mapView);
        duongDi.getOutlinePaint().setColor(Color.parseColor("#1D4ED8")); // màu chủ đạo của app
        duongDi.getOutlinePaint().setStrokeWidth(10f);
        mapView.getOverlays().add(duongDi);

        // MỚI: hiện các bệnh viện + cho phép nhấn giữ để chọn điểm đến
        dangKySuKienBanDo();

        locationListener = location -> xuLyViTriMoi(location);

        btnBatDau.setOnClickListener(v -> batDauGhi());
        btnDung.setOnClickListener(v -> dungGhi());
        btnDung.setEnabled(false);
        btnTimDuong.setOnClickListener(v -> timBenhVienGanNhat());
        btnXoa.setOnClickListener(v -> xoaBanDo()); // MỚI

        kiemTraVaXinQuyenViTri();
        dangKyTheoDoiMang();
        moLaiHanhTrinhGanNhat();

        // Mở sẵn Room để Database Inspector xem được ngay khi vào màn hình
        luongNen.execute(() -> {
            dao.layMaHanhTrinhGanNhat();
            Log.d(TAG, "[ROOM] Đã mở database medibook_local.db");
        });
        Log.d(TAG, "[APP] Mở màn hình Theo dõi hành trình");
    }

    // ================== MỚI: TÌM ĐƯỜNG NGẮN NHẤT ==================

    // MỚI: lấy bệnh viện quanh vị trí (chạy ở LUỒNG NỀN)
    // Có mạng: lấy từ OpenStreetMap. Lỗi/không có mạng: dùng danh sách mẫu viết sẵn.
    private List<CoSoYTe> layBenhVienXungQuanh(GeoPoint viTri) {
        // 1. Vừa tải ở gần đây (< 2 km) thì dùng lại, không gọi Overpass nữa
        //    → nhanh hơn và tránh bị server Overpass chặn vì gọi quá nhiều lần
        if (boNhoBenhVien != null && viTriBoNho != null
                && KhoangCach.haversineMet(viTri.getLatitude(), viTri.getLongitude(),
                viTriBoNho.getLatitude(), viTriBoNho.getLongitude()) < BAN_KINH_DUNG_LAI_MET) {
            dangDungDuLieuMau = false;
            Log.d(TAG, "[OVERPASS] Dùng lại " + boNhoBenhVien.size() + " bệnh viện đã tải trước đó");
            return CoSoYTe.ganNhat(boNhoBenhVien, viTri, SO_MARKER_TOI_DA);
        }

        // 2. Gọi Overpass để lấy bệnh viện thật quanh vị trí
        try {
            List<CoSoYTe> ds = TimBenhVienOverpass.timQuanh(viTri, BAN_KINH_TIM_MET);
            if (ds.isEmpty()) {
                ds = TimBenhVienOverpass.timQuanh(viTri, BAN_KINH_MO_RONG_MET);
            }
            if (!ds.isEmpty()) {
                boNhoBenhVien = ds;       // ghi nhớ để lần sau dùng lại
                viTriBoNho = viTri;
                dangDungDuLieuMau = false;
                Log.d(TAG, "[OVERPASS] Tìm thấy " + ds.size() + " bệnh viện quanh ("
                        + viTri.getLatitude() + ", " + viTri.getLongitude() + ")");
                return CoSoYTe.ganNhat(ds, viTri, SO_MARKER_TOI_DA);
            }
            Log.w(TAG, "[OVERPASS] Không có bệnh viện nào trong 15 km");
        } catch (Exception e) {
            Log.e(TAG, "[OVERPASS] Lỗi: " + e.getMessage());
        }

        // 3. Overpass lỗi nhưng đã từng tải thành công → dùng lại kết quả cũ
        if (boNhoBenhVien != null) {
            dangDungDuLieuMau = false;
            Log.w(TAG, "[OVERPASS] Dùng lại danh sách bệnh viện đã tải lần trước");
            return CoSoYTe.ganNhat(boNhoBenhVien, viTri, SO_MARKER_TOI_DA);
        }

        // 4. Chưa từng tải được lần nào → mới dùng dữ liệu mẫu
        Log.w(TAG, "[OVERPASS] Chuyển sang dùng dữ liệu bệnh viện mẫu");
        dangDungDuLieuMau = true;
        return CoSoYTe.ganNhat(viTri, SO_MARKER_TOI_DA);
    }

    // MỚI: tải bệnh viện quanh vị trí rồi vẽ marker (dùng khi mới mở app)
    private void taiVaHienThiBenhVien(GeoPoint viTri) {
        luongMang.execute(() -> {
            List<CoSoYTe> ds = layBenhVienXungQuanh(viTri);
            runOnUiThread(() -> hienThiMarkerBenhVien(ds));
        });
    }

    // SỬA: vẽ marker theo danh sách bệnh viện tìm được (xoá marker cũ trước)
    private void hienThiMarkerBenhVien(List<CoSoYTe> danhSach) {
        for (Marker cu : cacMarkerBenhVien) {
            cu.closeInfoWindow();
            mapView.getOverlays().remove(cu);
        }
        cacMarkerBenhVien.clear();

        for (CoSoYTe coSo : danhSach) {
            Marker marker = new Marker(mapView);
            marker.setPosition(coSo.viTri());
            marker.setTitle(coSo.ten);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setOnMarkerClickListener((m, map) -> {
                m.showInfoWindow();
                timDuongDen(coSo.viTri(), coSo.ten);
                return true;
            });
            mapView.getOverlays().add(marker);
            cacMarkerBenhVien.add(marker);
        }
        mapView.invalidate();
    }

    // Nhấn giữ bất kỳ điểm nào trên bản đồ để chọn làm điểm đến
    private void dangKySuKienBanDo() {
        MapEventsOverlay lopSuKien = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                timDuongDen(p, "điểm đã chọn");
                return true;
            }
        });
        mapView.getOverlays().add(0, lopSuKien); // đặt dưới cùng để không che marker
    }

    // Lấy vị trí hiện tại (từ lớp vị trí của bản đồ, hoặc vị trí GPS gần nhất)
    @SuppressLint("MissingPermission")
    private GeoPoint layViTriHienTai() {
        if (lopViTriHienTai != null && lopViTriHienTai.getMyLocation() != null) {
            return lopViTriHienTai.getMyLocation();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location viTri = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (viTri != null) {
                return new GeoPoint(viTri.getLatitude(), viTri.getLongitude());
            }
        }
        return null;
    }

    // Nút "Tìm đường đến bệnh viện gần nhất":
    // 1. Lọc 3 bệnh viện gần nhất theo đường chim bay (Haversine, không cần mạng)
    // 2. Hỏi OSRM đường đi thật đến từng bệnh viện
    // 3. Chọn bệnh viện có QUÃNG ĐƯỜNG THẬT ngắn nhất
    private void timBenhVienGanNhat() {
        GeoPoint viTri = layViTriHienTai();
        if (viTri == null) {
            Toast.makeText(this, "Chưa có vị trí GPS, vui lòng đợi một chút", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dangTimDuong) return;
        dangTimDuong = true;
        tvTrangThai.setText("Đang tìm các bệnh viện quanh bạn...");

        luongMang.execute(() -> {
            // SỬA: lấy bệnh viện THẬT quanh vị trí (OpenStreetMap), rồi lọc 3 cái gần nhất
            List<CoSoYTe> danhSach = layBenhVienXungQuanh(viTri);
            List<CoSoYTe> ungVien = CoSoYTe.ganNhat(danhSach, viTri, SO_UNG_VIEN_GAN_NHAT);
            TimDuongOsrm.KetQua tuyenTotNhat = null;
            CoSoYTe coSoTotNhat = null;

            for (CoSoYTe coSo : ungVien) {
                try {
                    TimDuongOsrm.KetQua kq = TimDuongOsrm.timDuong(viTri, coSo.viTri());
                    Log.d(TAG, String.format(Locale.US, "[HAVERSINE] %s: chim bay %.2f km",
                            coSo.ten, com.medibook.app.routing.KhoangCach.haversineMet(
                                    viTri.getLatitude(), viTri.getLongitude(), coSo.viDo, coSo.kinhDo) / 1000));
                    Log.d(TAG, String.format(Locale.US, "[OSRM] %s: đường thật %.2f km, %d phút",
                            coSo.ten, kq.quangDuongMet / 1000, Math.round(kq.thoiGianGiay / 60)));
                    if (tuyenTotNhat == null || kq.quangDuongMet < tuyenTotNhat.quangDuongMet) {
                        tuyenTotNhat = kq;
                        coSoTotNhat = coSo;
                    }
                } catch (Exception e) {
                    // bỏ qua bệnh viện này, thử bệnh viện tiếp theo
                    Log.e(TAG, "[OSRM] Lỗi tìm đường đến " + coSo.ten + ": " + e.getMessage());
                }
            }
            if (coSoTotNhat != null) {
                Log.d(TAG, "[KẾT QUẢ] Bệnh viện có đường đi ngắn nhất: " + coSoTotNhat.ten);
            }

            final TimDuongOsrm.KetQua ketQua = tuyenTotNhat;
            final CoSoYTe den = coSoTotNhat;
            runOnUiThread(() -> {
                dangTimDuong = false;
                hienThiMarkerBenhVien(danhSach); // MỚI: cập nhật marker theo vị trí hiện tại
                if (ketQua == null) {
                    baoLoiTimDuong();
                } else {
                    veDuongGoiY(ketQua, den.ten, den.viTri());
                    tvTrangThai.append(dangDungDuLieuMau
                            ? "\n(Dữ liệu mẫu: không tải được OpenStreetMap)"
                            : "\n(Nguồn: OpenStreetMap, " + danhSach.size() + " bệnh viện quanh bạn)");
                }
            });
        });
    }

    // Tìm đường ngắn nhất đến 1 điểm cụ thể (bệnh viện được chạm, hoặc điểm nhấn giữ)
    private void timDuongDen(GeoPoint dich, String tenDich) {
        GeoPoint viTri = layViTriHienTai();
        if (viTri == null) {
            Toast.makeText(this, "Chưa có vị trí GPS, vui lòng đợi một chút", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dangTimDuong) return;
        dangTimDuong = true;
        tvTrangThai.setText("Đang tìm đường đến " + tenDich + "...");

        luongMang.execute(() -> {
            TimDuongOsrm.KetQua kq = null;
            try {
                kq = TimDuongOsrm.timDuong(viTri, dich);
                Log.d(TAG, String.format(Locale.US, "[OSRM] Đến %s: %.2f km, %d phút",
                        tenDich, kq.quangDuongMet / 1000, Math.round(kq.thoiGianGiay / 60)));
            } catch (Exception e) {
                // xử lý ở dưới
                Log.e(TAG, "[OSRM] Lỗi tìm đường đến " + tenDich + ": " + e.getMessage());
            }
            final TimDuongOsrm.KetQua ketQua = kq;
            runOnUiThread(() -> {
                dangTimDuong = false;
                if (ketQua == null) {
                    baoLoiTimDuong();
                } else {
                    veDuongGoiY(ketQua, tenDich, dich);
                }
            });
        });
    }

    private void veDuongGoiY(TimDuongOsrm.KetQua kq, String tenDich, GeoPoint dich) {
        duongGoiY.setPoints(kq.cacDiem);

        if (markerDich == null) {
            markerDich = new Marker(mapView);
            markerDich.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(markerDich);
        }
        markerDich.setPosition(dich);
        markerDich.setTitle(tenDich);

        if (!dangGhi && !kq.cacDiem.isEmpty()) {
            mapView.zoomToBoundingBox(duongGoiY.getBounds(), true, 100);
        }
        mapView.invalidate();

        double km = kq.quangDuongMet / 1000.0;
        long phut = Math.round(kq.thoiGianGiay / 60.0);
        tvTrangThai.setText(String.format(Locale.getDefault(),
                "Đường ngắn nhất đến %s: %.1f km, khoảng %d phút.\nBấm Bắt đầu để ghi lại đường bạn thực sự đi.",
                tenDich, km, phut));
    }

    private void baoLoiTimDuong() {
        tvTrangThai.setText(coMang
                ? "Không tìm được đường, vui lòng thử lại."
                : "[Offline] Tìm đường cần có mạng. Việc lưu vết vẫn chạy bình thường.");
    }

    // ================== GHI HÀNH TRÌNH ==================

    @SuppressLint("MissingPermission")
    private void batDauGhi() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            kiemTraVaXinQuyenViTri();
            return;
        }

        maHanhTrinhHienTai = "HT_" + System.currentTimeMillis();
        soDiem = 0;
        duongDi.setPoints(new ArrayList<>());
        mapView.invalidate();

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 3000, 5, locationListener);

        boNho.edit().putString(KHOA_MA_DANG_GHI, maHanhTrinhHienTai).apply(); // MỚI: đánh dấu đang ghi dở

        dangGhi = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnBatDau.setEnabled(false);
        btnDung.setEnabled(true);
        tvTrangThai.setText((coMang ? "" : "[Offline] ") + "Đang ghi... chờ tín hiệu GPS");
        Log.d(TAG, "[GHI] Bắt đầu hành trình " + maHanhTrinhHienTai);
    }

    private void xuLyViTriMoi(Location location) {
        GeoPoint diem = new GeoPoint(location.getLatitude(), location.getLongitude());

        duongDi.addPoint(diem);
        mapView.getController().animateTo(diem);
        mapView.invalidate();

        DiemToaDo diemMoi = new DiemToaDo(
                location.getLatitude(), location.getLongitude(),
                System.currentTimeMillis(), maHanhTrinhHienTai);
        final int thuTu = soDiem + 1;
        final boolean offline = !coMang;
        Log.d(TAG, String.format(Locale.US, "[GPS] Điểm #%d: %.6f, %.6f (sai số %.0f m)",
                thuTu, location.getLatitude(), location.getLongitude(), location.getAccuracy()));
        luongNen.execute(() -> {
            dao.themDiem(diemMoi);
            Log.d(TAG, "[ROOM] INSERT điểm #" + thuTu + " vào bảng diem_toa_do"
                    + (offline ? " (đang OFFLINE)" : ""));
        });

        soDiem++;
        tvTrangThai.setText((coMang ? "" : "[Offline] ") + "Đang ghi... đã lưu " + soDiem + " điểm");
    }

    private void dungGhi() {
        locationManager.removeUpdates(locationListener);
        boNho.edit().remove(KHOA_MA_DANG_GHI).apply(); // MỚI: đã dừng đàng hoàng
        dangGhi = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnBatDau.setEnabled(true);
        btnDung.setEnabled(false);

        Log.d(TAG, "[GHI] Dừng hành trình " + maHanhTrinhHienTai + ", tổng " + soDiem + " điểm");
        veLaiTuRoom(maHanhTrinhHienTai, "Đã dừng.");
    }

    private void veLaiTuRoom(String maHanhTrinh, String thongBao) {
        luongNen.execute(() -> {
            List<DiemToaDo> danhSach = dao.layTatCaDiemTheoHanhTrinh(maHanhTrinh);
            Log.d(TAG, "[ROOM] SELECT " + danhSach.size() + " điểm của " + maHanhTrinh + " để vẽ lại");

            List<GeoPoint> cacDiem = new ArrayList<>();
            for (DiemToaDo d : danhSach) {
                cacDiem.add(new GeoPoint(d.viDo, d.kinhDo));
            }

            runOnUiThread(() -> {
                duongDi.setPoints(cacDiem);
                soDiem = cacDiem.size();
                if (!cacDiem.isEmpty() && !dangGhi) {
                    mapView.zoomToBoundingBox(duongDi.getBounds(), true, 80);
                }
                mapView.invalidate();
                tvTrangThai.setText(thongBao + " (" + cacDiem.size() + " điểm)");
            });
        });
    }

    // ================== THEO DÕI MẠNG & MỞ LẠI APP ==================

    private void dangKyTheoDoiMang() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        theoDoiMang = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    boolean vuaMatMang = !coMang;
                    coMang = true;
                    Log.d(TAG, "[MẠNG] Có mạng" + (vuaMatMang ? " trở lại → tải lại bản đồ, vẽ lại hành trình" : ""));
                    mapView.getTileProvider().clearTileCache();
                    mapView.invalidate();
                    if (vuaMatMang && maHanhTrinhHienTai != null) {
                        veLaiTuRoom(maHanhTrinhHienTai, "Đã có mạng lại, đã vẽ lại hành trình.");
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    coMang = false;
                    Log.w(TAG, "[MẠNG] Mất mạng → vẫn tiếp tục ghi GPS vào Room");
                    tvTrangThai.setText("[Offline] Mất mạng, vẫn đang lưu vị trí vào máy");
                });
            }
        };
        connectivityManager.registerDefaultNetworkCallback(theoDoiMang);
    }

    // SỬA: chỉ vẽ lại khi lần trước app bị tắt GIỮA LÚC ĐANG GHI (chưa bấm Dừng).
    // Nếu lần trước đã bấm Dừng thì mở app lên bản đồ sạch, không còn vết cũ.
    private void moLaiHanhTrinhGanNhat() {
        String ma = boNho.getString(KHOA_MA_DANG_GHI, null);
        if (ma == null) return;

        boNho.edit().remove(KHOA_MA_DANG_GHI).apply();
        maHanhTrinhHienTai = ma;
        veLaiTuRoom(ma, "Hành trình trước bị ngắt giữa chừng, đã vẽ lại.");
    }

    // MỚI: xoá mọi đường vẽ trên bản đồ (dữ liệu trong Room vẫn giữ)
    private void xoaBanDo() {
        if (dangGhi) {
            Toast.makeText(this, "Hãy bấm Dừng trước khi xoá", Toast.LENGTH_SHORT).show();
            return;
        }
        duongDi.setPoints(new ArrayList<>());
        duongGoiY.setPoints(new ArrayList<>());
        if (markerDich != null) {
            markerDich.closeInfoWindow();
            mapView.getOverlays().remove(markerDich);
            markerDich = null;
        }
        maHanhTrinhHienTai = null;
        soDiem = 0;
        mapView.invalidate();
        tvTrangThai.setText("Chạm vào bệnh viện hoặc nhấn giữ trên bản đồ để chọn điểm đến");
    }

    // ================== QUYỀN & VỊ TRÍ HIỆN TẠI ==================

    private void kiemTraVaXinQuyenViTri() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MA_YEU_CAU_QUYEN_VI_TRI);
        } else {
            hienThiViTriHienTai();
        }
    }

    private void hienThiViTriHienTai() {
        lopViTriHienTai = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        lopViTriHienTai.enableMyLocation();
        lopViTriHienTai.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(lopViTriHienTai);

        lopViTriHienTai.runOnFirstFix(() -> runOnUiThread(() -> {
            GeoPoint viTriHienTai = lopViTriHienTai.getMyLocation();
            if (viTriHienTai != null
                    && duongDi.getActualPoints().isEmpty()
                    && duongGoiY.getActualPoints().isEmpty()) {
                mapView.getController().animateTo(viTriHienTai);
            }
            // MỚI: có vị trí lần đầu thì tải và hiện các bệnh viện xung quanh
            if (viTriHienTai != null) {
                taiVaHienThiBenhVien(viTriHienTai);
            }
        }));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MA_YEU_CAU_QUYEN_VI_TRI) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hienThiViTriHienTai();
            } else {
                Toast.makeText(this, "Cần cấp quyền vị trí để dùng tính năng này", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dangGhi) locationManager.removeUpdates(locationListener);
        if (theoDoiMang != null) connectivityManager.unregisterNetworkCallback(theoDoiMang);
        luongNen.shutdown();
        luongMang.shutdown(); // MỚI
    }
}
