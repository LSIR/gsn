
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': '/tmp/gsn-webui.sqlite3', },
#     'default': {
#        'ENGINE': 'django.db.backends.', # Add 'postgresql_psycopg2', 'mysql', 'sqlite3' or 'oracle'.
#        'NAME': '',                      # Or path to database file if using sqlite3.
#        'USER': '',                      # Not used with sqlite3.
#        'PASSWORD': '',                  # Not used with sqlite3.
#        'HOST': '',                      # Set to empty string for localhost. Not used with sqlite3.
#        'PORT': '',                      # Set to empty string for default. Not used with sqlite3.
#    },
}

GSN = {
    'SERVER_URL': 'http://localhost:9000/',
    'WEBSOCKET_URL': 'ws://localhost:9000/',
    'CLIENT_ID': 'web-gui-public',
    'CLIENT_SECRET': 'web-gui-secret',
    'REDIRECTION_URL': 'http://127.0.0.1:8000/profile/',
    'SENSORS_URL': 'http://localhost:9000/ws/api/sensors',
    'AUTH_URL': 'http://localhost:9000/ws/oauth2/auth',
    'TOKEN_URL': 'http://localhost:9000/ws/oauth2/token',
    'USER_INF_URL': 'http://localhost:9000/ws/api/user',
    'MAX_QUERY_SIZE': 5000
}
