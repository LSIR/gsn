# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='GSNUser',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('access_token', models.CharField(max_length=100, null=True)),
                ('refresh_token', models.CharField(max_length=100, null=True)),
                ('token_created_date', models.DateTimeField(null=True)),
                ('token_expire_date', models.DateTimeField(null=True)),
            ],
        ),
    ]
