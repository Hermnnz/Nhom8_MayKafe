from rest_framework import serializers

from apps.catalog.models import Category, Product
from apps.catalog.services import (
    build_asset_label,
    default_accent_color,
    normalize_image_reference,
    resolve_image_url,
    store_uploaded_product_image,
)


class CategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = Category
        fields = ("id", "name")


class ProductSerializer(serializers.ModelSerializer):
    category = serializers.CharField(source="category.name", read_only=True)
    categoryId = serializers.IntegerField(source="category_id", read_only=True)
    price = serializers.SerializerMethodField()
    assetLabel = serializers.SerializerMethodField()
    accentColorHex = serializers.SerializerMethodField()
    image = serializers.SerializerMethodField()
    imageUrl = serializers.SerializerMethodField()
    imagePath = serializers.SerializerMethodField()

    class Meta:
        model = Product
        fields = (
            "id",
            "name",
            "price",
            "category",
            "categoryId",
            "available",
            "assetLabel",
            "accentColorHex",
            "image",
            "imageUrl",
            "imagePath",
        )

    def get_price(self, obj):
        return int(obj.price)

    def get_assetLabel(self, obj):
        return build_asset_label(obj.name)

    def get_accentColorHex(self, obj):
        return default_accent_color(getattr(obj.category, "name", None))

    def get_image(self, obj):
        return self.get_imageUrl(obj)

    def get_imageUrl(self, obj):
        request = self.context.get("request")
        return resolve_image_url(obj.image, request=request)

    def get_imagePath(self, obj):
        normalized = normalize_image_reference(obj.image)
        if not normalized or normalized.startswith("/"):
            return None
        if normalized.startswith(("http://", "https://")):
            return None
        return normalized


class ProductWriteSerializer(serializers.ModelSerializer):
    categoryId = serializers.IntegerField(required=False, write_only=True)
    category = serializers.CharField(required=False, write_only=True)
    assetLabel = serializers.CharField(required=False, allow_blank=True, write_only=True)
    accentColorHex = serializers.CharField(required=False, allow_blank=True, write_only=True)
    image = serializers.CharField(required=False, allow_blank=True, allow_null=True, write_only=True)
    imageUrl = serializers.CharField(required=False, allow_blank=True, allow_null=True, write_only=True)
    imagePath = serializers.CharField(required=False, allow_blank=True, allow_null=True, write_only=True)

    class Meta:
        model = Product
        fields = (
            "id",
            "name",
            "price",
            "category",
            "categoryId",
            "available",
            "assetLabel",
            "accentColorHex",
            "image",
            "imageUrl",
            "imagePath",
        )

    def validate_price(self, value):
        if value < 0:
            raise serializers.ValidationError("Gia ban phai lon hon hoac bang 0.")
        return value

    def validate(self, attrs):
        raw_category_name = attrs.pop("category", None)
        category_id = attrs.pop("categoryId", None)
        attrs.pop("assetLabel", None)
        attrs.pop("accentColorHex", None)
        image_path = attrs.pop("imagePath", None)
        image_url = attrs.pop("imageUrl", None)
        legacy_image = attrs.get("image")
        category_name = raw_category_name.strip() if isinstance(raw_category_name, str) else raw_category_name

        if category_id:
            try:
                attrs["category"] = Category.objects.get(pk=category_id)
            except Category.DoesNotExist as exc:
                raise serializers.ValidationError({"categoryId": "Danh muc khong ton tai."}) from exc
        elif category_name:
            category, _ = Category.objects.get_or_create(name=category_name)
            attrs["category"] = category
        elif self.instance is None:
            raise serializers.ValidationError({"category": "Danh muc la bat buoc."})

        image_fields_sent = any(
            field_name in self.initial_data for field_name in ("image", "imageUrl", "imagePath")
        )
        if image_fields_sent:
            image_reference = image_path if image_path is not None else image_url
            if image_reference is None:
                image_reference = legacy_image
            attrs["image"] = normalize_image_reference(image_reference)
        else:
            attrs.pop("image", None)

        return attrs

    def to_representation(self, instance):
        return ProductSerializer(instance, context=self.context).data


class ProductImageUploadSerializer(serializers.Serializer):
    image = serializers.FileField()

    def validate_image(self, value):
        content_type = getattr(value, "content_type", "") or ""
        if content_type and not content_type.startswith("image/"):
            raise serializers.ValidationError("File upload phai la anh hop le.")
        if value.size > 5 * 1024 * 1024:
            raise serializers.ValidationError("Kich thuoc anh khong duoc vuot qua 5MB.")
        return value

    def create(self, validated_data):
        stored_path = store_uploaded_product_image(validated_data["image"])
        request = self.context.get("request")
        resolved_url = resolve_image_url(stored_path, request=request)
        return {
            "imagePath": stored_path,
            "imageUrl": resolved_url,
            "image": resolved_url,
        }
