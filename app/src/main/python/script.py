import spacy
from spacy.pipeline import EntityRuler
import csv
from os.path import dirname,join
import requests
import bs4
import pandas as pd
import sqlite3
import json

class Commands:
    db_dir=join(dirname(__file__),"med.db")
    connection = sqlite3.connect(db_dir)
    connection.row_factory = sqlite3.Row
    cursor = connection.cursor()
    def __init__(self):
        create_medlists_table = f"""CREATE TABLE IF NOT EXISTS
        med_data(id INTEGER PRIMARY KEY, name TEXT NOT NULL,description TEXT)"""
        self.cursor.execute(create_medlists_table)

    def insert_data(self):
        #self.cursor.execute(
                #f"INSERT INTO med_data VALUES (NULL, '{name}','{desc}')"

        med = pd.read_csv(join(dirname(__file__),"med_names.csv"))

        med.to_sql('med_data', self.connection, if_exists='replace', index=False)
           # )
        #self.connection.commit()
    def insert_desc(self,txt,desc):

            self.cursor.execute("UPDATE med_data SET description = ? WHERE name = ?",(desc,txt))
            self.connection.commit()

    def dict_from_row(self):
                return [dict(row) for row in self.cursor.fetchall()]

    def get_med(self,txt1):
            self.cursor.execute(f"SELECT distinct * FROM med_data where name LIKE '{txt1+' '+'%'}'")
            return self.cursor.fetchall()
            #return json.dumps(self.dict_from_row())




# filename=join(dirname(__file__),"med1.csv")
# file = open(filename, "r")
# data = list(csv.reader(file, delimiter=","))
# file.close()
# names= [row[4].replace('-',' ').split()[0].lower() for row in data]
# name = [row[4] for row in data]
# description = [row[5].lower() for row in data]

# filename=join(dirname(__file__),"meds.csv")
# file = open(filename, "r")
# data = list(csv.reader(file, delimiter=","))
# file.close()
# names= [row[0].replace('-',' ').split()[0].lower() for row in data]
# name = [row[0] for row in data]
# description = [row[1].lower() for row in data]
# print(names[9]+" "+description[9])





def find_desc(text):
    url = 'https://google.com/search?q=' + text
    request_result=requests.get( url )
    soup = bs4.BeautifulSoup(request_result.text,"html.parser")
    a =soup.findAll("a")
    for ast in a:
        url = ast.get("href")
        if url.startswith("/url?") and ('1mg' in url):
            url1 = url[7:url.index('&')]
            #print(url1)
            result1 = requests.get(url1)
            doc = bs4.BeautifulSoup(result1.text, "html.parser")
            res1 = doc.find_all("div",class_="col-6 marginTop-8 GeneralDescription__htmlNodeWrapper__h23K3")
            if len(res1)>3:
                return res1[3].text

def main(txt):
    obj = Commands()
    if(len(txt.replace('-',' ').split()[0])>3):
        fet_data = obj.get_med(txt.replace('-',' ').split()[0])
        for fet in fet_data:
            words=fet[0]
            result={}
            op={}
            print(words)
            if(fet[1] is None):
                obj.insert_desc(words,find_desc(words))

            if(fet[1] is not None):
                desc=fet[1]
                result[words]=desc
            if fet[0]!='':
                #ndx = names.index(res.lower())
                #text= name[ndx]
                #des=find_desc(text)
                for key , value in result.items():
                #print(key)
                #print(value)
                    return key,value

            else:
                return '',''


