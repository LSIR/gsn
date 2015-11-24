# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
from django.conf import settings
import django.contrib.auth.models


class Migration(migrations.Migration):

    dependencies = [
        ('auth', '0006_require_contenttypes_0002'),
        ('gsn', '0002_delete_user'),
    ]

    operations = [
        migrations.CreateModel(
            name='GSNUser',
            fields=[
                ('user_ptr', models.OneToOneField(serialize=False, parent_link=True, auto_created=True, to=settings.AUTH_USER_MODEL, primary_key=True)),
                ('access_token', models.CharField(max_length=100)),
                ('refresh_token', models.CharField(max_length=100)),
                ('token_created_date', models.DateTimeField()),
                ('token_expire_date', models.DateTimeField()),
            ],
            options={
                'verbose_name': 'user',
                'abstract': False,
                'verbose_name_plural': 'users',
            },
            bases=('auth.user',),
            managers=[
                ('objects', django.contrib.auth.models.UserManager()),
            ],
        ),
    ]
