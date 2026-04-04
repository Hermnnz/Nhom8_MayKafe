from django.db import models

from apps.catalog.services import build_asset_label, default_accent_color, resolve_image_url


class Category(models.Model):
    id = models.AutoField(primary_key=True, db_column="ID_DanhMuc")
    name = models.CharField(max_length=50, db_column="TenDanhMuc")

    class Meta:
        db_table = "maycafe_danhmuc"
        ordering = ["id"]
        verbose_name = "Category"
        verbose_name_plural = "Categories"

    def __str__(self) -> str:
        return self.name


class Product(models.Model):
    id = models.AutoField(primary_key=True, db_column="ID_Mon")
    name = models.CharField(max_length=100, db_column="TenMon")
    price = models.FloatField(db_column="DonGia")
    image = models.CharField(max_length=500, db_column="HinhAnh", blank=True, null=True)
    available = models.BooleanField(db_column="TrangThai", default=True)
    category = models.ForeignKey(
        Category,
        on_delete=models.PROTECT,
        related_name="products",
        db_column="ID_DanhMuc_id",
    )

    class Meta:
        db_table = "maycafe_mon"
        ordering = ["id"]
        verbose_name = "Product"
        verbose_name_plural = "Products"

    def __str__(self) -> str:
        return self.name

    @property
    def asset_label(self) -> str:
        return build_asset_label(self.name)

    @property
    def accent_color_hex(self) -> str:
        category_name = self.category.name if getattr(self, "category_id", None) else None
        return default_accent_color(category_name)

    def get_image_url(self, request=None) -> str | None:
        return resolve_image_url(self.image, request=request)
