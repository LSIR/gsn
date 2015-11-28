from django.contrib.auth.models import User

__author__ = 'julie_000'

from django import forms
from gsn.models import GSNUser
from  django.contrib.auth.forms import UserCreationForm, AuthenticationForm


class GSNUserCreationForm(UserCreationForm):
    pass


class LoginForm(AuthenticationForm):
    pass


class ProfileForm(forms.ModelForm):
    email = forms.EmailField()
    # access_token = forms.CharField(max_length=100, required=False)
    # refresh_token = forms.CharField(max_length=100, required=False)

    class Meta:
        model = GSNUser
        fields = ('email', 'access_token', 'refresh_token', 'token_expire_date')

    pass
