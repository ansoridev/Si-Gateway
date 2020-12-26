from django.contrib import admin
from . import models as db
# Register your models here.

@admin.register(db.users)
class userClass(admin.ModelAdmin):
    search_fields = ['phone_number']
    list_display = ['phone_number', 'key']
    list_per_page = 10
    
@admin.register(db.message)
class msgClass(admin.ModelAdmin):
    search_fields = ['text', 'type', 'sender' , 'from_number', 'to_number']
    list_display = ['text', 'type', 'sender', 'from_number', 'to_number', 'is_send']
    list_per_page = 10