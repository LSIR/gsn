from django.contrib import admin
from gsn.models import GSNUser


# Register your models here.


class GSNUserAdmin(admin.ModelAdmin):
    pass


admin.site.register(GSNUser, GSNUserAdmin)
