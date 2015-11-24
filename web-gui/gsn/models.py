from django.db import models
from django.contrib.auth.models import User


# Create your models here.

class GSNUser(User):
    access_token = models.CharField(max_length=100)
    refresh_token = models.CharField(max_length=100)
    token_created_date = models.DateTimeField()
    token_expire_date = models.DateTimeField()
