from django.db import models


# Create your models here.

class User(models.Model):
    code = models.CharField(max_length= 100, primary_key=True)
    access_token = models.CharField(max_length=100)
    token_created_date = models.DateTimeField()
    token_expire_date = models.DateTimeField()
    refresh_token = models.CharField(max_length=100)
