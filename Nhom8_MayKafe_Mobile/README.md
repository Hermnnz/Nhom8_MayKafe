# Nhom8_MayKafe_Mobile

Android Native Java app cho he thong ban hang MayKafe.

App nay phu trach:

- giao dien
- dieu huong man hinh
- RecyclerView / adapter
- form nhap lieu
- xu ly su kien nguoi dung
- goi REST API toi backend Django
- hien thi du lieu, anh san pham va QR

App khong tu xu ly database server. Du lieu nghiep vu duoc lay tu backend qua JSON API.

## Cong nghe

- Android Native Java
- ViewBinding
- Retrofit 2
- Gson converter
- OkHttp + logging interceptor
- Glide
- ZXing

## Cau truc source chinh

```text
app/src/main/java/com/example/nhom8_makafe/
|- data/
|  |- SessionManager.java
|  |- api/
|     |- ApiClient.java
|     |- ApiService.java
|     |- ApiRepository.java
|     |- ApiModels.java
|     |- ApiCallback.java
|
|- model/
|- ui/
|  |- login/
|  |- sales/
|  |- cart/
|  |- invoices/
|  |- menu/
|  |- reports/
|  |- main/
|
|- util/
```

## Cac man hinh chinh

- Login
- Sales / POS
- Cart
- Checkout
- Invoices
- Invoice detail
- Menu management
- Reports

## API layer ben mobile

### 1. `ApiClient.java`

Tao Retrofit instance va gan `BuildConfig.API_BASE_URL`.

### 2. `ApiService.java`

Khai bao endpoint backend bang annotation Retrofit, vi du:

- `auth/login/`
- `catalog/products/`
- `orders/pending/`
- `orders/{orderId}/payment/qr/`
- `reports/dashboard/`

### 3. `ApiModels.java`

Chua toan bo DTO request / response dung de parse JSON.

Format response chung:

```json
{
  "success": true,
  "message": "Thong bao",
  "data": {}
}
```

### 4. `ApiRepository.java`

Lop trung gian giua UI va network:

- goi `ApiService`
- xu ly envelope response
- chuan hoa loi
- map DTO sang model UI nhu `User`, `Product`, `Invoice`, `PaymentSession`

UI fragment khong goi Retrofit truc tiep, ma di qua `ApiRepository`.

### 5. `SessionManager.java`

Giu:

- `authToken`
- `currentUser`
- gio hang tam thoi
- observer de MainActivity va cac fragment lang nghe thay doi session/cart

Luu y:

- token hien dang luu trong RAM
- restart app se mat session

## Flow goi API

### Dang nhap

1. `LoginFragment` goi `ApiRepository.login(...)`
2. `ApiRepository` goi `POST auth/login/`
3. Backend tra `token + user`
4. `SessionManager.login(token, user)`
5. `MainActivity` nhan thong bao session thay doi va dieu huong vao man hinh chinh

`LoginFragment` cung goi:

- `GET auth/branding/`

de lay avatar branding neu backend co file `media/branding/MayKafe_Avatar.jpg`.

### Lay san pham

1. `SalesFragment` goi `fetchProducts(...)`
2. `ApiRepository` gui header `Authorization: Token ...`
3. Backend tra danh sach san pham
4. App map ve `Product`
5. RecyclerView render danh sach

### Checkout hien tai

Checkout da duoc tach thanh nhieu buoc:

1. Mo `CheckoutBottomSheetDialogFragment`
2. Tao pending order:
   - `POST /api/v1/orders/pending/`
3. Neu thanh toan tien mat:
   - `POST /api/v1/orders/{orderId}/payment/cash/confirm/`
4. Neu thanh toan QR:
   - `POST /api/v1/orders/{orderId}/payment/qr/`
   - render QR bang ZXing
   - `POST /api/v1/orders/payments/{paymentId}/confirm/`
5. Neu user dong bottom sheet khi chua xong:
   - `POST /api/v1/orders/{orderId}/cancel/`

Dieu nay giup mobile va backend cung quan ly duoc trang thai don cho thanh toan.

## Cau hinh URL backend

Base URL duoc khai bao tai:

- `D:\Hoc Tap\NAM 3\Hoc Ki 2\LaptrinhMobile_Project\Nhom8_MayKafe\Nhom8_MayKafe_Mobile\app\build.gradle.kts`

Gia tri mac dinh:

```text
http://10.0.2.2:8000/api/v1/
```

Y nghia:

- Android Emulator goi ve backend local tren may tinh
- `10.0.2.2` la loopback dac biet cua emulator

Co the override bang Gradle property:

```bash
gradlew assembleDebug -PmaykafeApiBaseUrl=http://192.168.1.10:8000/api/v1/
```

## Permission va network

`AndroidManifest.xml` da co:

- `android.permission.INTERNET`
- `android:usesCleartextTraffic="true"`

Dieu nay cho phep test local qua HTTP trong giai doan demo/dev.

## Anh san pham

App hien thi anh san pham bang Glide.

Backend tra ve cac field:

- `image`
- `imageUrl`
- `imagePath`

Trong app, `ApiRepository` uu tien:

1. `imageUrl`
2. neu khong co thi fallback sang `image`

## Quyen va token

Token duoc backend cap sau dang nhap.

Moi request can auth se gui:

```text
Authorization: Token <token>
```

Role user hien tai duoc map ve enum `Role` trong app, chu yeu gom:

- `ADMIN`
- `STAFF`

Role nay anh huong den menu va man hinh hien thi cho nguoi dung.

## Danh sach endpoint app dang goi

### Auth

- `GET auth/branding/`
- `POST auth/login/`
- `GET auth/me/`
- `POST auth/logout/`

### Catalog

- `GET catalog/categories/`
- `GET catalog/products/`
- `POST catalog/products/`
- `PATCH catalog/products/{id}/`
- `PATCH catalog/products/{id}/availability/`
- `POST catalog/products/upload-image/`
- `DELETE catalog/products/{id}/`

### Orders / Payments

- `GET orders/`
- `GET orders/by-code/{code}/`
- `GET orders/summary/`
- `POST orders/checkout/`
- `POST orders/pending/`
- `POST orders/payment/qr/`
- `POST orders/payment/cash/confirm/`
- `POST orders/{orderId}/payment/qr/`
- `POST orders/{orderId}/payment/cash/confirm/`
- `POST orders/payments/{paymentId}/confirm/`
- `GET orders/payments/{paymentId}/status/`
- `POST orders/{orderId}/cancel/`

### Reports

- `GET reports/dashboard/`

## Cach chay app

1. Mo project trong Android Studio
2. Dam bao backend Django dang chay
3. Kiem tra `API_BASE_URL`
4. Build va chay tren emulator hoac thiet bi that

Neu test tren dien thoai that:

- backend can `runserver 0.0.0.0:8000`
- `ALLOWED_HOSTS` ben Django can cho phep IP LAN
- mobile phai dung IP LAN cua may tinh thay vi `10.0.2.2`

## Ghi chu hien trang

- App da co ket noi API that, khong con chi la mock UI
- Session hien la in-memory, chua co co che auto login sau khi mo lai app
- Checkout QR hien la flow confirm thu cong o phia app/backend demo, chua noi cong thanh toan that
- Luong invoice, menu management va reports da duoc map sang backend hien tai
