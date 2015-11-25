from django.db import models
from django.contrib.auth.models import User


# Create your models here.

class GSNUser(User):
    access_token = models.CharField(max_length=100, null=True)
    refresh_token = models.CharField(max_length=100, null=True)
    token_created_date = models.DateTimeField(null=True)
    token_expire_date = models.DateTimeField(null=True)
