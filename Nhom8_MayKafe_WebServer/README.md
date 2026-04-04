# Nhom8_MayKafe_WebServer

Backend Django + Django REST Framework cho du an MayKafe. Backend nay duoc to chuc theo huong API-first de phuc vu Android Native Java app, khong render giao dien server-side cho mobile flow.

## Cong nghe

- Python
- Django
- Django REST Framework
- django-cors-headers
- SQLite cho local development

## Cau truc chinh

- `config/`: settings, root URLs, ASGI/WSGI
- `apps/accounts/`: login, logout, ho so nguoi dung, phan quyen
- `apps/catalog/`: danh muc, san pham, upload anh
- `apps/sales/`: checkout, hoa don, chi tiet hoa don
- `apps/reports/`: bao cao tong hop
- `apps/common/`: response format, pagination, exceptions, permissions

## Chay local

1. Tao virtual environment moi neu `.venv` hien tai khong dung duoc:

```bash
python -m venv .venv
```

2. Kich hoat moi truong:

```bash
.venv\Scripts\activate
```

3. Cai dependency:

```bash
pip install -r requirements.txt
```

4. Tao file `.env` tu `.env.example`.

5. Chay migrate.

Neu ban muon tan dung cac bang `maycafe_*` da co san trong `db.sqlite3`, nen dung:

```bash
python manage.py migrate --fake-initial
```

Neu ban muon tao database moi hoan toan, co the xoa `db.sqlite3` truoc roi chay:

```bash
python manage.py migrate
```

6. Seed du lieu demo:

```bash
python manage.py seed_demo_data --with-orders
```

7. Chay server:

```bash
python manage.py runserver
```

API base URL:

```text
http://127.0.0.1:8000/api/v1/
```

Neu test tu Android Emulator, thuong dung:

```text
http://10.0.2.2:8000/api/v1/
```

Neu test tren dien thoai that cung mang LAN, them IP may tinh vao `DJANGO_ALLOWED_HOSTS`.

## Media

- Media URL local: `http://127.0.0.1:8000/media/...`
- Anh san pham duoc luu trong DB qua cot `HinhAnh`.
- Backend ho tro 2 kieu gia tri cho anh:
  - URL ngoai, vi du `https://...`
  - duong dan media local, vi du `products/2026/04/example.jpg`
- API se tra ve:
  - `imageUrl`: URL frontend Android co the load truc tiep
  - `imagePath`: duong dan tuong doi trong media, huu ich khi upload truoc roi moi gan vao san pham
  - `image`: alias giu tuong thich nguoc, gia tri giong `imageUrl`

Khi `DEBUG=True`, Django se phuc vu file media local qua `MEDIA_URL`.

## Tai khoan demo

- `admin / 123456`
- `nhanvien / 123456`

## Danh sach endpoint

### Auth

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

### Orders / Invoices

- `POST /api/v1/orders/checkout/`
- `GET /api/v1/orders/`
- `GET /api/v1/orders/{id}/`
- `GET /api/v1/orders/by-code/{code}/`
- `GET /api/v1/orders/summary/`
- `POST /api/v1/orders/{id}/restore/`

### Reports

- `GET /api/v1/reports/dashboard/?period=week|month|year`

## Vi du API

### 1. Login

Request:

```http
POST /api/v1/auth/login/
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
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
      "displayName": "Nguyen Van An",
      "role": "ADMIN",
      "avatarInitials": "NV",
      "avatarColorHex": "#6B3F2A"
    }
  }
}
```

### 2. Lay danh sach mon

Request:

```http
GET /api/v1/catalog/products/?search=ca&page=1&page_size=10
Authorization: Token your_token_here
```

Response:

```json
{
  "success": true,
  "message": "Lay danh sach mon thanh cong.",
  "data": [
    {
      "id": 1,
      "name": "Ca phe den",
      "price": 25000,
      "category": "Ca phe",
      "categoryId": 1,
      "available": true,
      "assetLabel": "CF",
      "accentColorHex": "#6B3F2A",
      "image": "http://127.0.0.1:8000/media/products/2026/04/cf_den.jpg",
      "imageUrl": "http://127.0.0.1:8000/media/products/2026/04/cf_den.jpg",
      "imagePath": "products/2026/04/cf_den.jpg"
    }
  ],
  "meta": {
    "pagination": {
      "page": 1,
      "page_size": 10,
      "total_pages": 1,
      "total_items": 1,
      "has_next": false,
      "has_previous": false
    }
  }
}
```

### 3. Upload anh san pham

Request:

```http
POST /api/v1/catalog/products/upload-image/
Authorization: Token your_token_here
Content-Type: multipart/form-data

image=<binary>
```

Response:

```json
{
  "success": true,
  "message": "Tai anh san pham thanh cong.",
  "data": {
    "imagePath": "products/2026/04/4b6ef4a96aaf4b7983db7d8fa6be4f30.jpg",
    "imageUrl": "http://127.0.0.1:8000/media/products/2026/04/4b6ef4a96aaf4b7983db7d8fa6be4f30.jpg",
    "image": "http://127.0.0.1:8000/media/products/2026/04/4b6ef4a96aaf4b7983db7d8fa6be4f30.jpg"
  }
}
```

Sau do gan `imagePath` hoac `imageUrl` vao request tao/cap nhat san pham.

### 4. Tao san pham

Request:

```http
POST /api/v1/catalog/products/
Authorization: Token your_token_here
Content-Type: application/json

{
  "name": "Ca phe muoi",
  "price": 42000,
  "category": "Ca phe",
  "available": true,
  "assetLabel": "CM",
  "accentColorHex": "#8A5A44",
  "imagePath": "products/2026/04/4b6ef4a96aaf4b7983db7d8fa6be4f30.jpg"
}
```

### 5. Checkout

Request:

```http
POST /api/v1/orders/checkout/
Authorization: Token your_token_here
Content-Type: application/json

{
  "tableNumber": "Ban 1",
  "discountPercent": 10,
  "paymentMethod": "CASH",
  "cashReceived": 100000,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "note": "It da"
    },
    {
      "productId": 11,
      "quantity": 1,
      "note": ""
    }
  ]
}
```

### 6. Dashboard reports

Request:

```http
GET /api/v1/reports/dashboard/?period=week
Authorization: Token your_token_here
```

## Assumptions khi map tu frontend

- Frontend mobile la Android native Java. Backend nay chi phuc vu API JSON, khong phuc vu giao dien server-side cho app.
- `Invoice` duoc map tu `Order`.
- `id` hien thi cho hoa don dung format `HD0001`, con route detail chinh van dung khoa so `pk`.
- `customers` trong bao cao hien duoc uoc luong bang so don thanh toan, vi frontend chua co entity khach hang rieng.
- `tableNumber` tu mobile la string nhu `Ban 1`; backend hien parse phan so de luu vao cot `SoBan`.
- Auth dung Django `User` + `Profile`, khong dung bang `maycafe_taikhoan` cu vi schema do chua du `username/displayName` cho frontend.

## Luu y

- CORS da duoc bat trong settings. Moi truong dev mac dinh cho phep tat ca origin.
- Neu muon xiet CORS, chinh lai `.env`.
- Admin site co tai `/admin/`.
- Trong local development, Django se phuc vu file media qua `MEDIA_URL` khi `DEBUG=True`.
