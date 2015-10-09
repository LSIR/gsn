__author__ = 'julie_000'

from django import forms


class TestForm(forms.Form):
    your_name = forms.CharField(label='Your name', max_length=100)
