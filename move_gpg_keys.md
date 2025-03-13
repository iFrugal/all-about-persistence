# 🚀 Moving GPG Keys to a New Device for `mvn deploy`

If you need to **deploy to Maven Central (`mvn deploy`) from a new device**, you must **transfer your GPG keys** securely.

---

## **🔹 Step 1: Find Your GPG Key on the Old Device**
Run:
```sh
gpg --list-secret-keys --keyid-format LONG
```
You'll see output like:
```
/Users/yourname/.gnupg/pubring.kbx
------------------------------------------------
sec   rsa4096/ABC1234567890DEF 2023-04-01 [SC]
      ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890
uid           [ultimate] Your Name <your.email@example.com>
ssb   rsa4096/1234567890ABCDEF 2023-04-01 [E]
```
- The **key ID** is the part after `rsa4096/`, e.g., `ABC1234567890DEF`.

---

## **🔹 Step 2: Export the GPG Key from the Old Device**
### **2.1 Export the Private (Secret) Key**
```sh
gpg --export-secret-keys --armor ABC1234567890DEF > private-key.asc
```
🔹 This **exports your private key**, which is required for signing artifacts.

### **2.2 Export the Public Key**
```sh
gpg --export --armor ABC1234567890DEF > public-key.asc
```
🔹 This **exports your public key**, which can be shared with others.

---

## **🔹 Step 3: Transfer the Keys to the New Device**
Move the `private-key.asc` and `public-key.asc` **securely** to the new device.  
🚨 **DO NOT use email or cloud storage** unless encrypted.

- **Recommended methods**:
  - USB drive
  - **SSH** (e.g., `scp`):
    ```sh
    scp private-key.asc public-key.asc new-device:/home/youruser/
    ```
  - **Encrypted ZIP**:
    ```sh
    zip -e gpg-keys.zip private-key.asc public-key.asc
    ```

---

## **🔹 Step 4: Import the GPG Key on the New Device**
On the **new device**, run:
```sh
gpg --import public-key.asc
gpg --import private-key.asc
```
Then, verify the import:
```sh
gpg --list-secret-keys --keyid-format LONG
```
Ensure the **same key ID** appears.

---

## **🔹 Step 5: Trust Your Key (Required for Signing)**
If your key is **not trusted**, sign it manually:
```sh
gpg --edit-key ABC1234567890DEF
```
Then type:
```sh
trust
```
Choose `5` (**Ultimate Trust**) and confirm with `y`.  
Then type `quit`.

---

## **🔹 Step 6: Enable GPG Agent for Maven Signing**
Ensure **GPG agent is running**:
```sh
gpgconf --launch gpg-agent
```
If Maven still asks for a passphrase:
```sh
echo "use-agent" >> ~/.gnupg/gpg.conf
echo "pinentry-mode loopback" >> ~/.gnupg/gpg-agent.conf
```
Then restart:
```sh
gpgconf --kill gpg-agent
gpgconf --launch gpg-agent
```

---

## **🔹 Step 7: Test `mvn deploy` on the New Device**
Try signing an artifact manually:
```sh
gpg --sign --armor test.txt
```
If this works, Maven should work too.

Then, test:
```sh
mvn clean deploy -Dgpg.passphrase="<your-passphrase>"
```

🚀 **Now, you can deploy from the new device!** 🚀
