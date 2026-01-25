import socket
import sys

def test_port(ip, port):
    print(f"\n--- Testing {ip}:{port} ---")
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(5)
        s.connect((ip, port))
        
        # Try standard JSON RPC Request
        req = (
            "POST /json_rpc HTTP/1.1\r\n"
            f"Host: {ip}:{port}\r\n"
            "Content-Type: application/json\r\n"
            "Content-Length: 50\r\n"
            "\r\n"
            '{"jsonrpc":"2.0","id":"0","method":"get_info"}'
        )
        s.sendall(req.encode())
        
        response = b""
        while True:
            try:
                chunk = s.recv(4096)
                if not chunk:
                    break
                response += chunk
            except socket.timeout:
                break
        
        if response:
            print("Received Data:")
            print(response.decode(errors='replace'))
        else:
            print("Connected, but received NO data (Empty Reply).")
            
        s.close()
    except Exception as e:
        print(f"Connection failed: {e}")

if __name__ == "__main__":
    ip = "103.214.169.20"
    test_port(ip, 17080)
    test_port(ip, 17081)
