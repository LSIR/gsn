from django.db import models
from django.contrib.auth.models import AbstractUser
from jsonfield import JSONField


class GSNUser(AbstractUser):
    access_token = models.CharField(max_length=100, null=True, blank=True)
    refresh_token = models.CharField(max_length=100, null=True, blank=True)
    token_created_date = models.DateTimeField(null=True, blank=True)
    token_expire_date = models.DateTimeField(null=True, blank=True)
    favorites = JSONField()
