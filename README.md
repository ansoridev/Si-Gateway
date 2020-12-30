# Si Gateway - SMS Gateway

<img src="https://i.ibb.co/ThJLfNF/si.png" width="150" />

[![Build Status](https://travis-ci.org/joemccann/dillinger.svg?branch=master)](https://travis-ci.org/joemccann/dillinger)

Si Gateway merupakan project berbasis Android yang menjadi penghubung dengan server untuk mengrimkan dan menerima sms
Project ini menggunakan Teknologi antara lain

- Android
  - Java Language
  - OkHttp3 Library
  - Push Notification
- Web Technology
  - Django 3.1 Web Framework
  - Gunicorn Uvicorn ASGI Running
  - Django ASGI Websockets 

# Fitur!
  - Dapat mengirimkan SMS secara realtime menggunakan protokol WebSockets
  - Dapat mengimkan SMS ke server tiap sms masuk

### Instalasi Django

Si Gateway membutuhkan [Python 3](https://python.org/) untuk berjalan

Install librari yang diperlukan untuk menjalan Django dengan cara
Membuat environment terlebih dahulu serta masuk ke folder project Django
```
> cd SiG - Django 3.1 Websockets
> python3 -m venv venv 
```

dan Masuk ke Environment dengan cara
- Windows
```
> "venv/Scripts/activate"
> pip install -r req.txt
```
- Linux
```
$ source "venv/bin/activate"
$ pip install -r req.txt
```

Lalu anda bisa menjalankan proses migrasi dengan cara sebagai berikut

```
> cd gateway
> python3 m migrate
```

Anda dapat mengsetup Pengguna administrator untuk login di
http://urlproject/admin

```
> python3 m createsuperuser
```

Setelah itu anda dapat menjalankan Django WSGI terlebih dahulu
```
> python3 m runserver 0.0.0.0:1400 --insecure
```
maka project Django HTTP anda akan berjalan pada http://localhost:1400

Belum cukup sampai disini, anda harus menjalankan GUNICORN UVICORN untuk menjalankan Django ASGI
Jalankan dengan terminal yang berbeda dari yang sebelumnya
Jangan lupa masuk ke environment terlebih dahulu, dan directori project Django
Jalankan command berikut di direktori yang terdapat file m (manage.py)
```
> gunicorn gateway.asgi:application -k uvicorn.workers.UvicornWorker -b 0.0.0.0:4390
```

Maka sekarang Project Django anda akan berjalan di Protokol ASGI dan WSGI (WebSockets) serta (HTTP)
Anda dapat mengcustom value di Aplikasi Si Gatewaynya

Download APK di [Si Gateway](https://github.com/ansoridev/Si-Gateway/raw/main/sigateway.apk)
atau anda dapat membuildnya sendiri di direktori source code Android di /SiG - SMS Gateway

Terima Kasih

License
----
Commons Clause

**Repositori ini bersifat sumber terbuka, Namun anda dilarang keras untuk memperjual belikannya**
