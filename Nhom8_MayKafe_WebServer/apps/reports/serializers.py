from rest_framework import serializers


class ReportPointSerializer(serializers.Serializer):
    label = serializers.CharField()
    revenue = serializers.IntegerField()
    orders = serializers.IntegerField()


class ReportSummarySerializer(serializers.Serializer):
    revenueChange = serializers.CharField()
    ordersChange = serializers.CharField()
    averageChange = serializers.CharField()
    customersChange = serializers.CharField()
    customers = serializers.IntegerField()
    revenueUp = serializers.BooleanField()
    ordersUp = serializers.BooleanField()
    averageUp = serializers.BooleanField()
    customersUp = serializers.BooleanField()


class ReportItemSerializer(serializers.Serializer):
    name = serializers.CharField()
    count = serializers.IntegerField()
    revenue = serializers.IntegerField()
    assetLabel = serializers.CharField()
    accentColorHex = serializers.CharField()


class CategoryShareSerializer(serializers.Serializer):
    name = serializers.CharField()
    percent = serializers.IntegerField()
    colorHex = serializers.CharField()
