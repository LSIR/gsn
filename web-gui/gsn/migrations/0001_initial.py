# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='User',
            fields=[
                ('code', models.CharField(primary_key=True, serialize=False, max_length=100)),
                ('access_token', models.CharField(max_length=100)),
                ('token_created_date', models.DateTimeField()),
                ('token_expire_date', models.DateTimeField()),
                ('refresh_token', models.CharField(max_length=100)),
            ],
        ),
    ]
