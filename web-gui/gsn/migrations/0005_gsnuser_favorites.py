# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import jsonfield.fields


class Migration(migrations.Migration):

    dependencies = [
        ('gsn', '0004_auto_20151128_2038'),
    ]

    operations = [
        migrations.AddField(
            model_name='gsnuser',
            name='favorites',
            field=jsonfield.fields.JSONField(default=dict),
        ),
    ]
