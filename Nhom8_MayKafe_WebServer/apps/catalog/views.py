from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import IsAuthenticated

from apps.catalog.models import Category, Product
from apps.catalog.serializers import (
    CategorySerializer,
    ProductImageUploadSerializer,
    ProductSerializer,
    ProductWriteSerializer,
)
from apps.common.permissions import IsAdminRole
from apps.common.responses import success_response


class CategoryViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = Category.objects.all()
    serializer_class = CategorySerializer
    permission_classes = [IsAuthenticated]
    pagination_class = None

    def list(self, request, *args, **kwargs):
        serializer = self.get_serializer(self.get_queryset(), many=True)
        return success_response(data=serializer.data, message="Lay danh sach danh muc thanh cong.")


class ProductViewSet(viewsets.ModelViewSet):
    queryset = Product.objects.select_related("category").all()
    permission_classes = [IsAuthenticated]

    def get_serializer_class(self):
        if self.action in {"create", "update", "partial_update"}:
            return ProductWriteSerializer
        if self.action == "upload_image":
            return ProductImageUploadSerializer
        return ProductSerializer

    def get_permissions(self):
        if self.action in {"create", "update", "partial_update", "destroy", "toggle_availability", "upload_image"}:
            return [IsAdminRole()]
        return super().get_permissions()

    def get_queryset(self):
        queryset = super().get_queryset()
        search = self.request.query_params.get("search", "").strip()
        category_id = self.request.query_params.get("category_id") or self.request.query_params.get("categoryId")
        category_name = self.request.query_params.get("category", "").strip()
        available = self.request.query_params.get("available")

        if search:
            queryset = queryset.filter(name__icontains=search)
        if category_id and category_id.isdigit():
            queryset = queryset.filter(category_id=int(category_id))
        elif category_name:
            queryset = queryset.filter(category__name__iexact=category_name)
        if available is not None:
            queryset = queryset.filter(available=available.lower() in {"1", "true", "yes"})
        return queryset

    def list(self, request, *args, **kwargs):
        queryset = self.filter_queryset(self.get_queryset())
        page = self.paginate_queryset(queryset)
        if page is not None:
            serializer = self.get_serializer(page, many=True)
            response = self.get_paginated_response(serializer.data)
            response.data["message"] = "Lay danh sach mon thanh cong."
            return response

        serializer = self.get_serializer(queryset, many=True)
        return success_response(data=serializer.data, message="Lay danh sach mon thanh cong.")

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        instance = serializer.save()
        return success_response(
            data=ProductSerializer(instance, context={"request": request}).data,
            message="Tao mon thanh cong.",
            status_code=status.HTTP_201_CREATED,
        )

    def retrieve(self, request, *args, **kwargs):
        serializer = ProductSerializer(self.get_object(), context={"request": request})
        return success_response(data=serializer.data, message="Lay chi tiet mon thanh cong.")

    def update(self, request, *args, **kwargs):
        partial = kwargs.pop("partial", False)
        instance = self.get_object()
        serializer = self.get_serializer(instance, data=request.data, partial=partial, context={"request": request})
        serializer.is_valid(raise_exception=True)
        instance = serializer.save()
        return success_response(
            data=ProductSerializer(instance, context={"request": request}).data,
            message="Cap nhat mon thanh cong.",
        )

    def destroy(self, request, *args, **kwargs):
        instance = self.get_object()
        instance.delete()
        return success_response(message="Xoa mon thanh cong.")

    @action(detail=True, methods=["patch"], url_path="availability")
    def toggle_availability(self, request, pk=None):
        instance = self.get_object()
        available = request.data.get("available")
        if available is None:
            instance.available = not instance.available
        else:
            instance.available = str(available).lower() in {"1", "true", "yes"}
        instance.save(update_fields=["available"])
        serializer = ProductSerializer(instance, context={"request": request})
        return success_response(data=serializer.data, message="Cap nhat trang thai ban thanh cong.")

    @action(
        detail=False,
        methods=["post"],
        url_path="upload-image",
        parser_classes=[MultiPartParser, FormParser],
    )
    def upload_image(self, request):
        serializer = self.get_serializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        payload = serializer.save()
        return success_response(
            data=payload,
            message="Tai anh san pham thanh cong.",
            status_code=status.HTTP_201_CREATED,
        )
