# See https://docs.djangoproject.com/en/1.8/ref/settings/#databases

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
    'CLIENT_ID': 'web-gui-public',
    'CLIENT_SECRET': 'web-gui-secret',
    'SERVICE_URL_PUBLIC': 'http://localhost:9000/ws/', # used for in-browser redirects
    'SERVICE_URL_LOCAL': 'http://localhost:9000/ws/',  # used for on-server direct calls
    'WEBUI_URL': 'http://127.0.0.1:8000/',             # used for in-browser redirects
    'MAX_QUERY_SIZE': 5000,
}

