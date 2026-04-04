PRAGMA foreign_keys = ON;

-- =========================
-- 1) Bảng TàiKhoản
-- =========================
CREATE TABLE IF NOT EXISTS TaiKhoan (
    ID_TaiKhoan INTEGER PRIMARY KEY AUTOINCREMENT,
    MatKhau     TEXT NOT NULL,
    ChucVu      TEXT
);

-- =========================
-- 2) Bảng DanhMục
-- =========================
CREATE TABLE IF NOT EXISTS DanhMuc (
    ID_DanhMuc   INTEGER PRIMARY KEY AUTOINCREMENT,
    TenDanhMuc   TEXT NOT NULL
);

-- =========================
-- 3) Bảng Món
-- =========================
CREATE TABLE IF NOT EXISTS Mon (
    ID_Mon       INTEGER PRIMARY KEY AUTOINCREMENT,
    TenMon       TEXT NOT NULL,
    DonGia       REAL NOT NULL CHECK (DonGia >= 0),
    HinhAnh      TEXT,
    TrangThai    TEXT,
    ID_DanhMuc   INTEGER NOT NULL,
    FOREIGN KEY (ID_DanhMuc) REFERENCES DanhMuc(ID_DanhMuc)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================
-- 4) Bảng BánHàng
-- =========================
CREATE TABLE IF NOT EXISTS BanHang (
    ID_HD                INTEGER PRIMARY KEY AUTOINCREMENT,
    NgayThanhToan        TEXT NOT NULL, -- lưu kiểu ISO: YYYY-MM-DD HH:MM:SS
    TongTien             REAL NOT NULL DEFAULT 0 CHECK (TongTien >= 0),
    KhuyenMai            REAL NOT NULL DEFAULT 0 CHECK (KhuyenMai >= 0),
    TongThanhTien        REAL NOT NULL DEFAULT 0 CHECK (TongThanhTien >= 0),
    HinhThucThanhToan    TEXT,
    TrangThai            TEXT,
    TienKhachDua         REAL NOT NULL DEFAULT 0 CHECK (TienKhachDua >= 0),
    TienTraLai           REAL NOT NULL DEFAULT 0 CHECK (TienTraLai >= 0),
    SoBan                INTEGER
);

-- =========================
-- 5) Bảng ChiTiet_BanHang
-- =========================
CREATE TABLE IF NOT EXISTS CT_BanHang (
    ID_HD       INTEGER NOT NULL,
    ID_Mon      INTEGER NOT NULL,
    DonGia      REAL NOT NULL CHECK (DonGia >= 0),
    SoLuong     INTEGER NOT NULL CHECK (SoLuong > 0),
    ThanhTien   REAL NOT NULL CHECK (ThanhTien >= 0),
    GhiChu      TEXT,
    PRIMARY KEY (ID_HD, ID_Mon),
    FOREIGN KEY (ID_HD) REFERENCES BanHang(ID_HD)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (ID_Mon) REFERENCES Mon(ID_Mon)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================
-- 6) Index để tăng tốc truy vấn
-- =========================
CREATE INDEX IF NOT EXISTS idx_mon_id_danhmuc
ON Mon(ID_DanhMuc);

CREATE INDEX IF NOT EXISTS idx_ct_banhang_id_mon
ON CT_BanHang(ID_Mon);