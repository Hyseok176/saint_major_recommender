import codecs

f_in = codecs.open(r'C:\\banghak\\noname.txt', 'r', encoding='cp949')
f_out = open(r'C:\\banghak\\temp_noname_content.txt', 'w', encoding='utf-8')
f_out.write(f_in.read())
f_in.close()
f_out.close()