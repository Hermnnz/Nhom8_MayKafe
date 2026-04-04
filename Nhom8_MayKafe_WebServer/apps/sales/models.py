from django.db import models

from apps.catalog.models import Product


class Order(models.Model):
    STATUS_PAID = "PAID"
    STATUS_PENDING = "PENDING"
    STATUS_CANCELLED = "CANCELLED"
    STATUS_CHOICES = (
        (STATUS_PAID, "Paid"),
        (STATUS_PENDING, "Pending"),
        (STATUS_CANCELLED, "Cancelled"),
    )

    PAYMENT_CASH = "CASH"
    PAYMENT_QR = "QR"
    PAYMENT_CHOICES = (
        (PAYMENT_CASH, "Cash"),
        (PAYMENT_QR, "QR"),
    )

    id = models.AutoField(primary_key=True, db_column="ID_HD")
    paid_at = models.DateTimeField(db_column="NgayThanhToan")
    subtotal = models.FloatField(db_column="TongTien", default=0)
    discount_amount = models.FloatField(db_column="KhuyenMai", default=0)
    total_amount = models.FloatField(db_column="TongThanhTien", default=0)
    payment_method = models.CharField(max_length=50, db_column="HinhThucThanhToan", choices=PAYMENT_CHOICES)
    status = models.CharField(max_length=50, db_column="TrangThai", choices=STATUS_CHOICES, default=STATUS_PENDING)
    cash_received = models.FloatField(db_column="TienKhachDua", default=0)
    change_amount = models.FloatField(db_column="TienTraLai", default=0)
    table_number = models.PositiveIntegerField(db_column="SoBan", default=0)

    class Meta:
        db_table = "maycafe_banhang"
        ordering = ["-paid_at", "-id"]
        verbose_name = "Order"
        verbose_name_plural = "Orders"

    def __str__(self) -> str:
        return self.public_code

    @property
    def public_code(self) -> str:
        return f"HD{self.id:04d}"

    @property
    def payment_method_label(self) -> str:
        mapping = {
            self.PAYMENT_CASH: "Tiền mặt",
            self.PAYMENT_QR: "Chuyển khoản QR",
        }
        return mapping.get(self.payment_method, self.payment_method)

    @property
    def table_label(self) -> str:
        if self.table_number <= 0:
            return "Bàn mới"
        return f"Bàn {self.table_number}"

    @property
    def note(self) -> str:
        return ""


class OrderItem(models.Model):
    id = models.BigAutoField(primary_key=True)
    order = models.ForeignKey(
        Order,
        on_delete=models.CASCADE,
        related_name="items",
        db_column="ID_HD_id",
    )
    product = models.ForeignKey(
        Product,
        on_delete=models.PROTECT,
        related_name="order_items",
        db_column="ID_Mon_id",
    )
    unit_price = models.FloatField(db_column="DonGia")
    quantity = models.PositiveIntegerField(db_column="SoLuong")
    line_total = models.FloatField(db_column="ThanhTien")

    class Meta:
        db_table = "maycafe_ct_banhang"
        ordering = ["id"]
        verbose_name = "Order item"
        verbose_name_plural = "Order items"
        constraints = [
            models.UniqueConstraint(fields=["order", "product"], name="sales_order_product_unique"),
        ]

    def __str__(self) -> str:
        return f"{self.order.public_code} - {self.product.name}"

    @property
    def note(self) -> str:
        return ""
