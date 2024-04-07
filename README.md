# Distributed Parking Management System-Mulligan

Designed and implemented 'Mulligan', a distributed parking management system. This project aimed at simplifying parking processes, demonstrating skills in Java, MongoDB, and automated testing/deployment.

how to deploy replica set in mongoDB: https://www.mongodb.com/docs/manual/tutorial/deploy-replica-set/#:~:text=Alternatively%2C%20you%20can,ip%20address(es)%3E

First we need install mongoDB shell and mongodb
open notepad in admin and go to the path of your mongoDB bin folder then open mongod.cfg in notepad (admin mode)

---replace:

# network interfaces

net:
port: 27017
bindIp: 127.0.0.1

---with:
net:
port: 27017
bindIp: 0.0.0.0

# replication settings

replication:
replSetName: "rs0"

for CMD1 (most be in administrator mode):
[go to the path of your mongoDB bin folder]
cd C:\Program Files\MongoDB\Server\6.0\bin
mongod --bind_ip 0.0.0.0 --port 27017

for CMD2 (most be in administrator mode):
cd C:\Program Files\MongoDB\Server\6.0\bin
mongod --bind_ip 0.0.0.0 --port 27017 --replSet "rs0"

path mongosh:
your path to mongosh example and start mongosh
C:\Users\slema\AppData\Local\Programs\mongosh\
go to services and look for mongoDB server then restart it
go to mongoDB shell folder then open mongosh.exe
write : rs.initiate()
then write : rs.status()
last open mongoDBCompass then you need to have port 27017 and in advance settings -> replica set name must be : rs0
![image](https://github.com/Kinneret-OSCourse/ds-ass3-5784-project404-1/assets/67858042/b93704a5-1add-47f1-bc4f-eb568c9a8d42)

and we good to go
