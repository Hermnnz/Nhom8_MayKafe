# Nhom8_MayKafe_WebServer

Backend Django + Django REST Framework cho du an MayKafe.

Backend nay duoc to chuc theo huong API-first de phuc vu ung dung Android Native Java. Mobile app goi REST API, con backend phu trach:

- ket noi database
- truy van va cap nhat du lieu
- validation dau vao
- xu ly logic nghiep vu
- quan ly token dang nhap
- tra JSON cho frontend
- phuc vu URL anh san pham va branding khi chay local

Backend khong render giao dien HTML cho mobile flow. Django admin van ton tai de ho tro quan tri, nhung khong phai luong chinh cua app.

## Cong nghe

- Python
- Django
- Django REST Framework
- django-cors-headers
- SQLite cho local development
- Co san cau truc env de doi sang PostgreSQL/MySQL sau nay

## Cau truc chinh

```text
config/
|- settings/
|  |- base.py
|  |- dev.py
|  |- prod.py
|- urls.py
|- wsgi.py
|- asgi.py

apps/
|- accounts/
|- catalog/
|- sales/
|- reports/
|- common/
```

### Y nghia tung app

- `apps/accounts`: login, logout, me, branding, bridge auth tu bang `maycafe_taikhoan`
- `apps/catalog`: danh muc, san pham, upload anh, CRUD product
- `apps/sales`: order, invoice, pending order, thanh toan tien mat, thanh toan QR
- `apps/reports`: dashboard tong hop theo ky
- `apps/common`: response format, pagination, exceptions, permissions, health check

## Database va mapping hien tai

Backend dang uu tien tan dung schema nghiep vu co san trong SQLite:

- `maycafe_taikhoan`
- `maycafe_danhmuc`
- `maycafe_mon`
- `maycafe_banhang`
- `maycafe_ct_banhang`
- `maycafe_thanhtoan`

Ngoai ra Django can them cac bang ho tro nhu:

- `auth_user`
- `accounts_profile`
- `authtoken_token`
- `django_content_type`
- `django_session`

Luu y quan trong:

- credentials dang nhap that duoc doc tu `maycafe_taikhoan`
- backend van dong bo sang `auth_user` + `accounts_profile` de su dung token auth va permission cua Django/DRF

## Auth hien tai

### Nguon du lieu dang nhap

Backend xac thuc username/password tu bang:

- `maycafe_taikhoan`

Model mapping:

- `LegacyAccount` trong `apps/accounts/models.py`

Sau khi xac thuc thanh cong:

1. backend dong bo user sang Django `auth_user`
2. tao/cap nhat `accounts_profile`
3. cap `Token` trong `authtoken_token`
4. tra `token + user payload` cho mobile

### Role

Role tu bang `maycafe_taikhoan.ChucVu` duoc map ve:

- `ADMIN`
- `STAFF`

Gia tri nhu `admin`, `quanly`, `manager` se duoc map thanh `ADMIN`.

### Token flow

Mobile gui header:

```text
Authorization: Token <token>
```

DRF dung `TokenAuthentication` de nhan dien `request.user`.

## Response format

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

Danh sach co them:

```json
{
  "meta": {
    "pagination": {
      "page": 1,
      "page_size": 10,
      "total_pages": 1,
      "total_items": 3,
      "has_next": false,
      "has_previous": false
    }
  }
}
```

## Media va hinh anh

Backend da cau hinh:

- `MEDIA_URL = /media/`
- `MEDIA_ROOT = BASE_DIR / "media"`

Khi `DEBUG=True`, Django se phuc vu media local.

### Anh san pham

Backend luu reference anh qua cot `HinhAnh` cua bang mon.

Gia tri nay co the la:

- URL ngoai
- duong dan media tuong doi, vi du `products/2026/04/example.jpg`

API tra ve:

- `image`
- `imageUrl`
- `imagePath`

### Branding avatar cho man login

Endpoint:

- `GET /api/v1/auth/branding/`

Backend tim file:

- `media/branding/MayKafe_Avatar.jpg`

Neu file ton tai, backend tra `loginAvatarUrl` de mobile load bang Glide.

## Cau hinh moi truong

Tao file `.env` tu `.env.example`.

Bien chinh:

```env
DJANGO_SECRET_KEY=change-me
DJANGO_DEBUG=True
DJANGO_ALLOWED_HOSTS=127.0.0.1,localhost,10.0.2.2
DJANGO_TIME_ZONE=Asia/Bangkok

DB_ENGINE=sqlite
SQLITE_NAME=db.sqlite3

CORS_ALLOW_ALL_ORIGINS=True
CORS_ALLOWED_ORIGINS=http://127.0.0.1:3000,http://localhost:3000
CSRF_TRUSTED_ORIGINS=http://127.0.0.1:3000,http://localhost:3000
API_PAGE_SIZE=10
```

Backend hien tai cung ho tro cac bien thanh toan, neu khong khai bao se dung default trong `config/settings/base.py`:

- `PAYMENT_RECEIVER_BANK_NAME`
- `PAYMENT_RECEIVER_BANK_BIN`
- `PAYMENT_RECEIVER_ACCOUNT_NUMBER`
- `PAYMENT_RECEIVER_ACCOUNT_NAME`
- `PAYMENT_TRANSFER_CONTENT`
- `PAYMENT_QR_EXPIRES_MINUTES`

## Chay local

### 1. Tao moi truong ao

```bash
python -m venv .venv
.venv\Scripts\activate
```

### 2. Cai dependency

Tai repo root co `requirements.txt`, vi vay co the cai bang:

```bash
pip install -r ..\requirements.txt
```

Neu muon dung file cung cap theo module backend thi co the tao them file rieng sau, nhung hien tai repo dang dung `..\requirements.txt`.

### 3. Tao `.env`

Copy `.env.example` thanh `.env` va dieu chinh neu can.

### 4. Migrate

Neu dang tan dung `db.sqlite3` co san:

```bash
python manage.py migrate --fake-initial
```

Neu ban vua thay `db.sqlite3` bang mot DB moi duoc copy tu noi khac:

```bash
python manage.py migrate --fake-initial
```

Buoc nay rat quan trong de bo sung cac bang Django support. No khong nham thay the bang nghiep vu `maycafe_*`, ma de backend co the chay auth/token/admin on dinh.

### 5. Chay server

```bash
python manage.py runserver 0.0.0.0:8000
```

Base URL thuong dung:

- Local browser/Postman: `http://127.0.0.1:8000/api/v1/`
- Android Emulator: `http://10.0.2.2:8000/api/v1/`
- Dien thoai that cung LAN: `http://<LAN_IP>:8000/api/v1/`

## ALLOWED_HOSTS va CORS

Project dang doc host tu `DJANGO_ALLOWED_HOSTS`.

Mac dinh local/demo da ho tro:

- `127.0.0.1`
- `localhost`
- `10.0.2.2`

Khi `DEBUG=True`, backend con co co che bo sung them IP LAN cua may de thuan tien test tren dien thoai that.

CORS dang duoc cau hinh qua:

- `CORS_ALLOW_ALL_ORIGINS`
- `CORS_ALLOWED_ORIGINS`

## Danh sach endpoint

Tat ca endpoint duoc prefix:

```text
/api/v1/
```

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

Filter list san pham:

- `search`
- `category`
- `available`
- `page`
- `page_size`

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
- `GET /api/v1/orders/{id}/`
- `GET /api/v1/orders/by-code/{code}/`
- `GET /api/v1/orders/summary/`
- `POST /api/v1/orders/{id}/restore/`

Filter list order:

- `search`
- `date`
- `status`

`status` hop le:

- `PAID`
- `PENDING`
- `CANCELLED`

### Reports

- `GET /api/v1/reports/dashboard/?period=week|month|year`

### Health

- `GET /api/v1/health/`

## Flow nghiep vu checkout

### Luong app hien tai

1. Mobile tao pending order
2. User chon cash hoac QR
3. Neu cash:
   - backend xac nhan so tien khach dua
   - tao payment method cash
   - cap nhat order thanh `PAID`
4. Neu QR:
   - backend tao hoac refresh payment QR
   - backend tra thong tin ngan hang, noi dung chuyen khoan, QR content
   - app hien ma QR
   - khi xac nhan thanh toan, backend cap nhat payment `paid` va order `PAID`
5. Neu user huy man hinh checkout, backend huy pending order

### Luong 1 buoc

`POST /api/v1/orders/checkout/` van duoc giu lai de phuc vu cach goi 1 request cho checkout don gian.

## Vi du request / response

### Login

Request:

```http
POST /api/v1/auth/login/
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Response:

```json
{
  "success": true,
  "message": "Dang nhap thanh cong.",
  "data": {
    "token": "your_token_here",
    "user": {
      "username": "admin",
      "displayName": "admin",
      "role": "ADMIN",
      "avatarInitials": "AD",
      "avatarColorHex": "#6B3F2A"
    }
  }
}
```

### Branding avatar

```http
GET /api/v1/auth/branding/
```

Response:

```json
{
  "success": true,
  "message": "Lay thong tin avatar dang nhap thanh cong.",
  "data": {
    "loginAvatarUrl": "http://127.0.0.1:8000/media/branding/MayKafe_Avatar.jpg"
  }
}
```

### Lay danh sach san pham

```http
GET /api/v1/catalog/products/?available=true&page_size=100
Authorization: Token your_token_here
```

### Tao pending order

```http
POST /api/v1/orders/pending/
Authorization: Token your_token_here
Content-Type: application/json

{
  "tableNumber": "Ban moi",
  "discountPercent": 10,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "note": "It da"
    }
  ]
}
```

### Lay QR thanh toan cho order

```http
POST /api/v1/orders/15/payment/qr/
Authorization: Token your_token_here
```

Response se chua:

- `paymentId`
- `orderId`
- `orderCode`
- `amount`
- `bankName`
- `accountNumber`
- `accountName`
- `transferContent`
- `qrContent`
- `status`
- `expiresAt`

## Assumptions va ghi chu hien trang

- Mobile la Android Native Java, khong phai web frontend.
- Backend tra JSON, khong render template cho luong mobile.
- `Invoice` duoc map tu `Order`.
- `tableNumber` tu mobile la string, backend parse phan so de luu vao `SoBan`.
- Khi bang `maycafe_taikhoan` thay doi du lieu, login se phan anh theo bang nay.
- QR payment hien la luong demo/noi bo, chua ket noi cong thanh toan tu dong de doi soat giao dich that.
- Admin site co tai `/admin/` de xem va quan ly du lieu khi can.

## File quan trong nen doc

- `config/settings/base.py`
- `config/urls.py`
- `apps/accounts/models.py`
- `apps/accounts/services.py`
- `apps/accounts/views.py`
- `apps/catalog/serializers.py`
- `apps/catalog/views.py`
- `apps/sales/models.py`
- `apps/sales/services.py`
- `apps/sales/views.py`
- `apps/reports/views.py`
