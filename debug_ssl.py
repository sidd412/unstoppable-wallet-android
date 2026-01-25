import socket
import ssl
import sys

def test_ssl(ip, port):
    print(f"\n--- Testing SSL on {ip}:{port} ---")
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(5)
        
        # Wrap with SSL
        context = ssl.create_default_context()
        context.check_hostname = False
        context.verify_mode = ssl.CERT_NONE
        
        ssl_sock = context.wrap_socket(s, server_hostname=ip)
        ssl_sock.connect((ip, port))
        
        print("SSL Handshake Successful!")
        print(f"Cipher: {ssl_sock.cipher()}")
        
        msg = (
            "POST /json_rpc HTTP/1.1\r\n"
            f"Host: {ip}:{port}\r\n"
            "Content-Type: application/json\r\n"
            "Content-Length: 50\r\n"
            "\r\n"
            '{"jsonrpc":"2.0","id":"0","method":"get_info"}'
        )
        ssl_sock.sendall(msg.encode())
        
        response = ssl_sock.recv(4096)
        print("Received Data over SSL:")
        print(response.decode(errors='replace'))
        
        ssl_sock.close()
    except Exception as e:
        print(f"SSL Failed: {e}")

if __name__ == "__main__":
    ip = "103.214.169.20"
    test_ssl(ip, 17081)
    test_ssl(ip, 17089)
