
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': 'db.sqlite3', }
}

GSN = {
    'SERVER_URL': 'http://opensense.epfl.ch/',
    'CLIENT_ID': 'web-gui-dev-local-public',
    'CLIENT_SECRET': 'web-gui-dev-local-jAzg',
    'REDIRECTION_URL': 'http://127.0.0.1:8000/gsn/profile/',
    'SENSORS_URL': 'http://opensense.epfl.ch/ws/api/sensors',
    'AUTH_URL': 'http://opensense.epfl.ch/ws/oauth2/auth',
    'TOKEN_URL': 'http://opensense.epfl.ch/ws/oauth2/token',
    'USER_INF_URL': 'http://opensense.epfl.ch/ws/api/user',
    'MAX_QUERY_SIZE': 5000
}