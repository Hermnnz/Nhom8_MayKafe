from django.db.models import Q, Sum
from django.utils.dateparse import parse_date
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from apps.common.responses import success_response
from apps.sales.models import Order
from apps.sales.serializers import CheckoutSerializer, OrderSerializer


class CheckoutAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = CheckoutSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        order = serializer.save()
        return success_response(
            data=OrderSerializer(order).data,
            message="Tao don hang thanh cong.",
            status_code=status.HTTP_201_CREATED,
        )


class OrderViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = Order.objects.prefetch_related("items__product").all()
    serializer_class = OrderSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = super().get_queryset()
        search = self.request.query_params.get("search", "").strip()
        selected_date = self.request.query_params.get("date", "").strip()
        status_value = self.request.query_params.get("status", "").strip().upper()

        if search:
            digits = "".join(ch for ch in search if ch.isdigit())
            search_filter = Q()
            if digits:
                search_filter |= Q(id=int(digits))
                search_filter |= Q(table_number=int(digits))
            queryset = queryset.filter(search_filter) if search_filter else queryset.none()

        if selected_date:
            parsed_date = parse_date(selected_date)
            if parsed_date:
                queryset = queryset.filter(paid_at__date=parsed_date)

        if status_value in {Order.STATUS_PAID, Order.STATUS_PENDING, Order.STATUS_CANCELLED}:
            queryset = queryset.filter(status=status_value)

        return queryset

    def retrieve(self, request, *args, **kwargs):
        return success_response(
            data=self.get_serializer(self.get_object()).data,
            message="Lay chi tiet don hang thanh cong.",
        )

    @action(detail=True, methods=["post"], url_path="restore")
    def restore(self, request, pk=None):
        order = self.get_object()
        if order.status == Order.STATUS_CANCELLED:
            order.status = Order.STATUS_PENDING
            order.save(update_fields=["status"])
        return success_response(
            data=self.get_serializer(order).data,
            message="Khoi phuc don hang thanh cong.",
        )

    @action(detail=False, methods=["get"], url_path=r"by-code/(?P<code>[^/.]+)")
    def by_code(self, request, code=None):
        digits = "".join(ch for ch in (code or "") if ch.isdigit())
        order = self.get_queryset().filter(id=int(digits)).first() if digits else None
        if order is None:
            from rest_framework.exceptions import NotFound

            raise NotFound("Khong tim thay don hang.")
        return success_response(
            data=self.get_serializer(order).data,
            message="Lay chi tiet don hang thanh cong.",
        )

    @action(detail=False, methods=["get"], url_path="summary")
    def summary(self, request):
        queryset = Order.objects.all()
        selected_date = request.query_params.get("date", "").strip()
        parsed_date = parse_date(selected_date) if selected_date else None
        if parsed_date:
            queryset = queryset.filter(paid_at__date=parsed_date)

        paid_queryset = queryset.filter(status=Order.STATUS_PAID)
        cancelled_queryset = queryset.filter(status=Order.STATUS_CANCELLED)
        revenue = paid_queryset.aggregate(total=Sum("total_amount"))["total"] or 0
        data = {
            "paidCount": paid_queryset.count(),
            "cancelledCount": cancelled_queryset.count(),
            "revenue": int(revenue),
        }
        return success_response(data=data, message="Lay thong ke hoa don thanh cong.")
