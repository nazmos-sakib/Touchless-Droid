import socket
import time

while True:
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client:
            client.connect(("192.168.2.102", 8080))
            response = client.makefile().readline().strip()
            print(response)

    except OSError:
        print("Server unavailable")

    time.sleep(0.1)