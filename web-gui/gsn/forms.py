__author__ = 'julie_000'

from django import forms
from gsn.models import GSNUser
from  django.contrib.auth.forms import UserCreationForm, PasswordChangeForm


class GSNUserCreationForm(UserCreationForm):
    class Meta:
        model = GSNUser
        fields = ('username', 'email')

    pass


class ProfileForm(forms.ModelForm):
    email = forms.EmailField()
    access_token = forms.CharField(max_length=100, required=False)
    refresh_token = forms.CharField(max_length=100, required=False)

    class Meta:
        model = GSNUser
        fields = ('email', 'access_token', 'refresh_token')

    pass
