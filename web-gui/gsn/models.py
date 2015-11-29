from django.db import models
from django.contrib.auth.models import User
from django.db.models import signals


# Create your models here.


class GSNUser(models.Model):
    user = models.OneToOneField(User, null=True)
    access_token = models.CharField(max_length=100, null=True, blank=True)
    refresh_token = models.CharField(max_length=100, null=True, blank=True)
    token_created_date = models.DateTimeField(null=True, blank=True)
    token_expire_date = models.DateTimeField(null=True, blank=True)

    def save(self, *args, **kwargs):
        for var in vars(self):
            if not var.startswith('_'):
                if self.__dict__[var] == '':
                    self.__dict__[var] = None
        super(GSNUser, self).save(*args, **kwargs)


def create_gsn_user(sender, instance, created, **kwargs):
    if created:
        GSNUser.objects.create(user=instance)


signals.post_save.connect(create_gsn_user, sender=User, weak=False, dispatch_uid='models.create_model_b')
