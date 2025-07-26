import codecs

f = codecs.open('C:\\banghak\\noname.txt', 'r', encoding='cp949')
print(f.read())
f.close()