# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
import jsonfield.fields
from django.conf import settings


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name='GSNUser',
            fields=[
                ('id', models.AutoField(primary_key=True, verbose_name='ID', auto_created=True, serialize=False)),
                ('access_token', models.CharField(null=True, max_length=100, blank=True)),
                ('refresh_token', models.CharField(null=True, max_length=100, blank=True)),
                ('token_created_date', models.DateTimeField(null=True, blank=True)),
                ('token_expire_date', models.DateTimeField(null=True, blank=True)),
                ('favorites', jsonfield.fields.JSONField(default=dict)),
                ('user', models.OneToOneField(null=True, to=settings.AUTH_USER_MODEL)),
            ],
        ),
    ]
