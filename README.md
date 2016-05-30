# FileCryptorApp
Console application that uses standard Java classes (_javax.crypto.*_) for encrypting and decrypting files.

Download latest version [here](https://github.com/rstetsenko/FileCryptorApp/blob/master/Release/FileCryptorApp.jar?raw=true)

### Usage:
First parameter: -e (encrypt) or -d (decrypt);  
Second parameter: your password;  
Third parameter: path to file.

Example: 
>\>java -jar JNCryptorApp.jar -e MyPassword path/to/file/example.txt

Encrypted or decrypted file will be created at the same directory as the source file.
