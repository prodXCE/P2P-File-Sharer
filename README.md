# Core Java P2P File Sharer

A **command-line peer-to-peer (P2P) file sharing application** built entirely from scratch in **Core Java**. This application allows me to send and receive files of any size directly between devices on a local network or over the internet.

---

## ✨ Features

This project was built incrementally, focusing on **core networking and concurrency concepts**. The key features include:

* ✅ **Direct P2P File Transfer**
  Utilizes **TCP sockets** for reliable, connection-oriented transfer of large files. Data is streamed in chunks to handle files of any size without high memory consumption.

* ✅ **Automatic Peer Discovery**
  Uses **UDP broadcasting** to automatically discover other users running the application on the same local network, removing the need to manually enter IP addresses.

* ✅ **Web Server Fallback**
  Includes an embedded **HTTP server** to generate a temporary download link for a file. This allows me to send files to anyone on the network with a web browser, even if they don’t have the application installed.

* ✅ **Internet Sharing Capability**
  Detects the public IP address and provides a **shareable link** for internet-based transfers, with clear instructions about the necessity of **port forwarding**.

* ✅ **Strong AES Encryption**
  Features optional, password-protected **AES-256 encryption** for secure file transfers. It uses a standard key derivation function (**PBKDF2**) to convert a user's password into a strong cryptographic key.

* ✅ **Interactive Command-Line Interface (CLI)**
  A clean, user-friendly **menu system** guides me through sending and receiving files.

---

## 🧱 Project Structure

```
src/
└── com/
    └── p2pfilesharer/
        ├── Main.java             // Main entry point
        ├── cli/                  // Handles all user interaction
        │   └── CliHandler.java
        ├── discovery/            // Logic for UDP peer discovery
        │   └── PeerDiscovery.java
        ├── encryption/           // AES encryption utilities
        │   └── CryptoUtils.java
        ├── network/              // Core TCP file sender/receiver
        │   ├── FileSender.java
        │   └── FileReceiver.java
        └── web/                  // Embedded HTTP server
            └── HttpFileServer.java
```

---

## ⚙️ Setup and Installation

### ✅ Prerequisites

* Java Development Kit (**JDK 11** or newer)
* Git for cloning the repository
* An IDE like **IntelliJ IDEA**, **VS Code**, or just a terminal

---

### 📥 Steps

1. **Clone the repository**

```bash
git clone https://github.com/prodXCE/P2P-File-Sharer.git
cd P2P-File-Sharer
```

2. **Compile the project**

From the root directory of the project, compile all Java files into an output directory (`out`):

```bash
javac -d out $(find src -name "*.java")
```

---

## 🚀 How to Use

Navigate to the output directory:

```bash
cd out
```

---

### 1️⃣ Receive a File (P2P)

You must start the receiver **first**:

```bash
java com.p2pfilesharer.Main
```

* Choose option `2` → **"Receive a file (P2P)"**
* If expecting an encrypted file:

  * Choose `y` and enter the agreed-upon password
* Otherwise, choose `n`
* Enter the directory to save the incoming file (e.g., `~/Downloads`)
* The application will now wait for a sender to connect

---

### 2️⃣ Send a File (P2P)

Run in a **new terminal**:

```bash
java com.p2pfilesharer.Main
```

* Choose option `1` → **"Send a file (P2P)"**
* The app will automatically search for peers on the local network
* Choose from the discovered peers or enter an IP address manually
* Choose whether to enable encryption (`y/n`)
* Enter the full path to the file to send
* Transfer will begin

---

### 3️⃣ Send a File (Web Link)

This option allows sending a file to someone **without the application**.

```bash
java com.p2pfilesharer.Main
```

* Choose option `3` → **"Send a file (Web Link)"**
* Enter the full path to the file
* The app will start a web server and display two links:

```
Local Link    → http://<local-ip>:8080
Internet Link → http://<public-ip>:8080
```

* The **Local Link** works for anyone on the same Wi-Fi network
* The **Internet Link** only works if **port 8080 is forwarded** on the router
* The server remains active until I press `Enter` in the terminal

---

## 🔐 Notes on Security

* AES encryption is **optional but recommended** for sensitive files.
* Passwords are never stored. A secure key is generated using PBKDF2.
* For internet sharing, **use with caution**: anyone with the link can download the file.

---

## 📌 Final Notes

This is a pure Core Java project focused on building a **real-world file-sharing system** using **sockets, concurrency, encryption, and networking principles**—with **no external dependencies**.

---
