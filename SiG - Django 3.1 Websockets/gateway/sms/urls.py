from django.urls import path
from . import views

urlpatterns = [
    path("ws/incoming/", views.incoming_sms),
    path("ws/outgoing/", views.outgoing_sms),
]