from django.urls import path

from apps.reports.views import ReportDashboardAPIView


urlpatterns = [
    path("dashboard/", ReportDashboardAPIView.as_view(), name="report-dashboard"),
]
