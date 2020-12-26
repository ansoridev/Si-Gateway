from asgiref.sync import sync_to_async
from . import models as db

async def incoming_sms(socket):
    await socket.accept()
    auth = ""
    db_auth = db
    
    while True:
        try:
            msg = await socket.receive_json()
            
            if not auth:
                try:
                    auth_json = msg
                    db_auth = await sync_to_async(db.users.objects.filter, thread_sensitive=False)(key = auth_json["key"])
                    if db_auth:
                        auth = db_auth[0].phone_number
                        await socket.send_json({
                            "status": True
                        })
                    else:
                        await socket.send_json({
                            "status": False
                        })
                        break
                except:
                    await socket.send_json({
                            "status": False,
                            "message": "Your data is not JSON Type"
                        })
                    break
                
            try:
                if not 'type' in msg:
                    msg['type'] = "outgoing"
            except:
                msg = {
                    "type": "outgoing"
                }
            
            if msg['type'] == "incoming":
                
                sync_to_async(db.message(
                    is_send = True,
                    text = msg['data']['text'],
                    type = msg['type'],
                    from_number = msg['data']['from_number'],
                    to_number = auth
                ).save(), thread_sensitive=True)
                
                await socket.send_json({
                    "type": msg['type'],
                    "message": "SMS has been saved!"
                })
        except:
            pass
        
async def outgoing_sms(socket):
    await socket.accept()
    auth = ""
    
    while True:
        try:
            if not auth:
                try:
                    auth_json = await socket.receive_json()
                    print(auth_json)
                    db_auth = await sync_to_async(db.users.objects.filter, thread_sensitive=False)(key = auth_json["key"])
                    if db_auth:
                        auth = db_auth[0].phone_number
                        await socket.send_json({
                            "status": True
                        })
                        print(db_auth[0].phone_number + " Berhasil Masuk")
                    else:
                        await socket.send_json({
                            "status": False
                        })
                        print("Gagal Masuk")
                        break
                except:
                    await socket.send_json({
                            "status": False,
                            "message": "Your data is not JSON Type"
                        })
                    print("Gagal Masuk")
                    break
                
            async_msg = await sync_to_async(db.message.objects.filter, thread_sensitive=True)(is_send=False, type="outgoing", from_number=auth)
            if async_msg:
                await socket.send_json({
                    "type": async_msg[0].type,
                    "data": {
                        "from_number": async_msg[0].from_number,
                        "to_number": async_msg[0].to_number,
                        "text": async_msg[0].text
                    }
                })
                print("SMS Outgoing terkirim ke Web Sockets")
                sync_to_async(async_msg.update(is_send=True), thread_sensitive=True)
        except:
            pass

from django.views import View
from django.http import JsonResponse
import json

class SMS_Restful(View):
    def auth(self, key):
        keyQ = db.users.objects.filter(key=key)
        if keyQ:
            return keyQ
        
        return False
    
    def get(self, request, *args, **kwargs):
        user = self.auth(request.GET.get('key', ''))
        if not user:
            return JsonResponse({
                "status": False
            })
            
        msgQ = db.message.objects.filter(type = "incoming", to_number = user[0].phone_number)
        dataList = []
        for msg in msgQ:
            dataList.append({
                "type": "incoming",
                "data": {
                    "from_number": msg.from_number,
                    "to_number": msg.to_number,
                    "text": msg.text
                }
            })
        
        return JsonResponse({
            "status": True,
            "data": dataList
        }, safe=False)
    
    
    def post(self, request, *args, **kwargs):
        body = json.loads(request.body) if request.body else {}
        
        user = self.auth(body['key'])
        if not user:
            return JsonResponse({
                "status": False
            })
        
        try:
            db.message(
                type = "outgoing",
                sender = user[0],
                from_number = user[0].phone_number,
                to_number = body['data']['to_number'],
                text = body['data']['text'],
                is_send = False
            ).save()
            return JsonResponse({
                "status": True,
                "message": f"SMS has been sent to {body['data']['to_number']}" 
            })
        except:
            return JsonResponse({
                "status": False,
                "message": f"Failure when sending SMS to {body['data']['to_number']}" 
            })