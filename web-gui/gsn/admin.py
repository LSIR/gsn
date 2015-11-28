from django.contrib import admin
from gsn.models import GSNUser


# Register your models here.


class GSNUserAdmin(admin.ModelAdmin):
    list_display = ['get_username', ]

    def get_username(self, obj):
        return obj.user.username
        pass

    pass


admin.site.register(GSNUser, GSNUserAdmin)
