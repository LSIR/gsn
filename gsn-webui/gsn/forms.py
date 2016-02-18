from django.contrib.auth.models import User


from django import forms
from gsn.models import GSNUser
from django.contrib.auth.forms import UserCreationForm, AuthenticationForm


class GSNUserCreationForm(UserCreationForm):
    email = forms.EmailField()

    def __init__(self, *args, **kwargs):
        super(GSNUserCreationForm, self).__init__(*args, **kwargs)
        self.fields.keyOrder = ['username', 'email', 'password1', 'password2', ]

    class Meta:
        model = User
        fields = ("username", "email")

    def clean_email(self):

        email = self.cleaned_data.get("email")
        if User.objects.filter(email=email).exists():
            raise forms.ValidationError('Email already in use')
        return email


class LoginForm(AuthenticationForm):
    pass


class ProfileForm(forms.ModelForm):
    username = forms.CharField(max_length=30, required=False)
    email = forms.EmailField()

    # access_token = forms.CharField(max_length=100, required=False)
    # refresh_token = forms.CharField(max_length=100, required=False)

    class Meta:
        model = GSNUser
        fields = ('username', 'email', 'access_token', 'refresh_token', 'token_expire_date')

    def clean_email(self):
        email = self.cleaned_data.get('email')
        username = self.cleaned_data.get('username')
        if User.objects.filter(email=email).exists() and User.objects.get(email=email).username != username:
            raise forms.ValidationError('Email addresses must be unique.')
        return email
