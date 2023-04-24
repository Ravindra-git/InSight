import cv2
from PIL import Image
import base64
import io
import json
from os.path import dirname,join
import requests
def main(data):
    decoded_data = base64.b64decode(data)
    buff = io.BytesIO(decoded_data)
    img = Image.open(buff)
    filename=join(dirname(__file__),"hi.jpeg")
    img.save(filename)
    print(img.size)
    api_key='K85584476188957'
    image = Image.open(filename)
    image.save(filename,quality=15,optimize=True)

    payload = {'isOverlayRequired': True,
                   'apikey': api_key,
                   'language': 'eng',
                   'OCRENGINE': 5,
                   }
    with open(filename, 'rb') as f:
        r = requests.post('https://api.ocr.space/parse/image',
                          files={filename: f},
                          data=payload,
                              )
    p=r.content.decode()
    data=json.loads(p)

    total=['total amount','total','bill','due','total amount:','total amount :']
    staticZero = 0
    itemCost_item = [0]
    initText = data['ParsedResults'][staticZero]['ParsedText']
    flag=False
    chars = set('0123456789$,.')

    for index, line in enumerate(data['ParsedResults'][staticZero]['TextOverlay']['Lines']):
        if 'LineText' in line:
            t=line['LineText'].split('/n')
            if flag==True:
                if all((c in chars) for c in t[0]):
                    if ',' or '$' in t[0]:
                        t[0]=t[0].replace(',','')
                        t[0]=t[0].replace('$','')
                    itemCost_item.append(float(t[0]))
                    flag=False
                else:
                    flag=True
            if 'total' in t[0].lower():
                flag=True
    str1 = str(int(max(itemCost_item)))
    print(convert_to_words(str1))
    return convert_to_words(str1)

def convert_to_words(num):

    l = len(num)

    # Base cases
    if (l == 0):
        return ""
        return

    if (l > 4):
        return

    single_digits = ["zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"]

    two_digits = ["", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"]

    tens_multiple = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"]

    tens_power = ["hundred", "thousand"]

    print(num, ":", end=" ")

    if (l == 1):
        return single_digits[ord(num[0]) - 48]
        return

    x = 0
    while (x < len(num)):
        if (l >= 3):
            if (ord(num[x]) - 48 != 0):
                return (single_digits[ord(num[x]) - 48])
                return (tens_power[l - 3])

            l -= 1

        else:

            if (ord(num[x]) - 48 == 1):
                sum = (ord(num[x]) - 48 +
                       ord(num[x+1]) - 48)
                return (two_digits[sum])
                return

            elif (ord(num[x]) - 48 == 2 and
                  ord(num[x + 1]) - 48 == 0):
                return ("twenty")
                return

            else:
                i = ord(num[x]) - 48
                if(i > 0):
                    return (tens_multiple[i])
                else:
                    return ""
                x += 1
                if(ord(num[x]) - 48 != 0):
                    return (single_digits[ord(num[x]) - 48])
        x += 1


