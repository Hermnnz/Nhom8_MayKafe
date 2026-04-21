# Nhom8_MayKafe

Du an MayKafe gom 2 phan chinh:

- `Nhom8_MayKafe_Mobile`: ung dung Android Native Java
- `Nhom8_MayKafe_WebServer`: backend Django + Django REST Framework

He thong duoc thiet ke theo huong mobile app goi REST API. Toan bo giao dien, dieu huong man hinh, xu ly su kien va hien thi du lieu nam o Android. Backend chi tap trung vao database, nghiep vu, validation, xu ly thanh toan, media va tra JSON cho app.

## Kien truc tong quan

```text
Android Native Java
  -> BuildConfig.API_BASE_URL
  -> ApiClient (Retrofit + OkHttp)
  -> ApiService (khai bao endpoint)
  -> ApiRepository (map DTO -> model UI)
  -> SessionManager (giu token, user, cart trong RAM)

Django REST API
  -> config/urls.py
  -> apps/accounts
  -> apps/catalog
  -> apps/sales
  -> apps/reports
  -> SQLite / DB ngoai duoc map bang ORM
```

## Cau truc repo

```text
Nhom8_MayKafe/
|- Nhom8_MayKafe_Mobile/
|  |- app/
|  |  |- src/main/java/com/example/nhom8_makafe/
|  |  |  |- data/api/
|  |  |  |- data/
|  |  |  |- model/
|  |  |  |- ui/
|  |  |- build.gradle.kts
|
|- Nhom8_MayKafe_WebServer/
|  |- config/
|  |- apps/accounts/
|  |- apps/catalog/
|  |- apps/sales/
|  |- apps/reports/
|  |- apps/common/
|  |- manage.py
|  |- db.sqlite3
|  |- README.md
|
|- requirements.txt
|- README.md
```

## Chuc nang hien tai

Mobile app hien co cac luong chinh sau:

- Dang nhap
- Tai avatar branding cho man login
- Man ban hang / POS
- Tim kiem va loc mon theo danh muc
- Gio hang
- Thanh toan tien mat
- Thanh toan QR chuyen khoan
- Tao don cho thanh toan, huy don cho khi dong man checkout
- Danh sach hoa don
- Xem chi tiet hoa don
- Quan ly menu cho admin
- Dashboard bao cao cho admin

Backend da co API phuc vu cac luong tren va khong render HTML cho mobile flow.

## API duoc noi nhu the nao

### Base URL ben mobile

Base URL de app Java goi xuong backend duoc cau hinh tai:

- `D:\Hoc Tap\NAM 3\Hoc Ki 2\LaptrinhMobile_Project\Nhom8_MayKafe\Nhom8_MayKafe_Mobile\app\build.gradle.kts`

Gia tri mac dinh:

```text
http://10.0.2.2:8000/api/v1/
```

`10.0.2.2` la dia chi Android Emulator dung de goi ve may local.

### Lop goi API ben mobile

- `ApiClient.java`: tao Retrofit client
- `ApiService.java`: khai bao endpoint
- `ApiRepository.java`: goi endpoint, xu ly response, map DTO sang model UI
- `SessionManager.java`: giu token, user hien tai va cart

### Lop API ben backend

- `config/urls.py`: root URL
- `apps/accounts`: auth, branding, me, logout
- `apps/catalog`: category, product, upload anh, CRUD product
- `apps/sales`: order, invoice, pending order, cash payment, QR payment
- `apps/reports`: dashboard tong hop
- `apps/common`: response format, pagination, exception handler, permission

## Flow token dang nhap

1. Mobile goi `POST /api/v1/auth/login/`
2. Backend kiem tra thong tin dang nhap tu bang `maycafe_taikhoan`
3. Backend dong bo sang Django `auth_user` + `accounts_profile` de su dung token auth va permission
4. Backend cap `Token`
5. Mobile luu token trong `SessionManager`
6. Cac request can dang nhap se gui header:

```text
Authorization: Token <token>
```

Luu y:

- Token hien duoc giu trong RAM, chua persist xuong `SharedPreferences`
- Tat app / restart app thi can dang nhap lai

## Thanh toan hien tai

Mobile dang dung luong checkout moi:

1. Tao pending order: `POST /api/v1/orders/pending/`
2. Neu thanh toan tien mat:
   - `POST /api/v1/orders/{orderId}/payment/cash/confirm/`
3. Neu thanh toan QR:
   - `POST /api/v1/orders/{orderId}/payment/qr/`
   - hien QR cho nguoi dung quet
   - `POST /api/v1/orders/payments/{paymentId}/confirm/`
4. Neu dong man checkout khi chua thanh toan xong:
   - `POST /api/v1/orders/{orderId}/cancel/`

Endpoint `POST /api/v1/orders/checkout/` van ton tai cho truong hop thanh toan 1 buoc, nhung app hien tai uu tien dung luong pending order + confirm.

## Chay backend local

Di vao:

```bash
cd D:\Hoc Tap\NAM 3\Hoc Ki 2\LaptrinhMobile_Project\Nhom8_MayKafe\Nhom8_MayKafe_WebServer
```

Tao moi truong:

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r ..\requirements.txt
```

Tao `.env` tu `.env.example`, sau do migrate:

```bash
python manage.py migrate --fake-initial
```

Neu ban vua copy mot `db.sqlite3` moi tu noi khac sang:

- van nen chay `python manage.py migrate --fake-initial`
- muc dich la bo sung cac bang Django support nhu `auth_user`, `authtoken_token`, `django_content_type`, `django_session`...
- cac bang nghiep vu `maycafe_*` se duoc tan dung tiep

Chay server:

```bash
python manage.py runserver 0.0.0.0:8000
```

## Chay mobile local

Mo folder:

```text
D:\Hoc Tap\NAM 3\Hoc Ki 2\LaptrinhMobile_Project\Nhom8_MayKafe\Nhom8_MayKafe_Mobile
```

Neu dung Android Emulator va backend chay tren may local, giu mac dinh:

```text
http://10.0.2.2:8000/api/v1/
```

Neu muon doi base URL, co the truyen Gradle property:

```bash
gradlew assembleDebug -PmaykafeApiBaseUrl=http://192.168.1.10:8000/api/v1/
```

Hoac sua gia tri mac dinh trong:

- `D:\Hoc Tap\NAM 3\Hoc Ki 2\LaptrinhMobile_Project\Nhom8_MayKafe\Nhom8_MayKafe_Mobile\app\build.gradle.kts`

## Danh sach endpoint chinh

### Auth

- `GET /api/v1/auth/branding/`
- `POST /api/v1/auth/login/`
- `GET /api/v1/auth/me/`
- `POST /api/v1/auth/logout/`

### Catalog

- `GET /api/v1/catalog/categories/`
- `GET /api/v1/catalog/products/`
- `GET /api/v1/catalog/products/{id}/`
- `POST /api/v1/catalog/products/`
- `PATCH /api/v1/catalog/products/{id}/`
- `PATCH /api/v1/catalog/products/{id}/availability/`
- `POST /api/v1/catalog/products/upload-image/`
- `DELETE /api/v1/catalog/products/{id}/`

### Orders / Payments

- `POST /api/v1/orders/checkout/`
- `POST /api/v1/orders/pending/`
- `POST /api/v1/orders/payment/qr/`
- `POST /api/v1/orders/payment/cash/confirm/`
- `POST /api/v1/orders/{orderId}/payment/qr/`
- `POST /api/v1/orders/{orderId}/payment/cash/confirm/`
- `POST /api/v1/orders/payments/{paymentId}/confirm/`
- `GET /api/v1/orders/payments/{paymentId}/status/`
- `POST /api/v1/orders/{orderId}/cancel/`
- `GET /api/v1/orders/`
- `GET /api/v1/orders/by-code/{code}/`
- `GET /api/v1/orders/summary/`
- `POST /api/v1/orders/{id}/restore/`

### Reports

- `GET /api/v1/reports/dashboard/?period=week|month|year`

## Dinh dang response

Thanh cong:

```json
{
  "success": true,
  "message": "Lay du lieu thanh cong.",
  "data": {}
}
```

Loi:

```json
{
  "success": false,
  "message": "Noi dung loi",
  "errors": {}
}
```

Danh sach co them `meta.pagination`.

## Hinh anh va media

Backend tra URL anh de mobile load bang Glide.

- Media URL local: `/media/`
- Product response tra:
  - `image`
  - `imageUrl`
  - `imagePath`
- Login branding su dung:
  - `GET /api/v1/auth/branding/`
  - file mac dinh backend tim tai `media/branding/MayKafe_Avatar.jpg`

## Ghi chu thuc te trong qua trinh phat trien

- Frontend khong con chi dung mock data. Hien tai da co lop goi API that.
- Dang nhap hien doc tu bang `maycafe_taikhoan`, khong doc truc tiep tu `auth_user`.
- Backend van dung Django auth/token de quan ly phien va permission.
- Admin site van co tai `/admin/`, nhung day khong phai luong chinh cua mobile app.
- Mot so file build / IDE co the dang duoc git track tu truoc. Can kiem tra `.gitignore` neu muon lam sach repo.

## Tai lieu theo tung module

- Mobile README:
  - `D:\Hoc Tap\NAM 3\Hoc Ki 2\LaptrinhMobile_Project\Nhom8_MayKafe\Nhom8_MayKafe_Mobile\README.md`
- Backend README:
  - `D:\Hoc Tap\NAM 3\Hoc Ki 2\LaptrinhMobile_Project\Nhom8_MayKafe\Nhom8_MayKafe_WebServer\README.md`
