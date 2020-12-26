from django.db import models

# Create your models here.
class users(models.Model):
    phone_number = models.CharField(default="", max_length=255)
    key = models.CharField(max_length=255, default="KEY")
    
    def __str__(self):
        return str(self.phone_number)
    
class message(models.Model):
    type_choose = (
        ('incoming', 'incoming'),
        ('outgoing', 'outgoing')
    )
    text = models.TextField()
    type = models.CharField(max_length=255, choices=type_choose)
    sender = models.ForeignKey(users, on_delete=models.CASCADE, blank=True, null=True)
    from_number = models.CharField(max_length=255)
    to_number = models.CharField(max_length=255)
    is_send = models.BooleanField(default=False)
    
    def __str__(self):
        return self.type