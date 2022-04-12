import numpy as np
import os
import re
import sys
from sklearn.feature_extraction.text import TfidfVectorizer
from nltk.stem.porter import PorterStemmer
from sklearn.metrics.pairwise import cosine_similarity

def load_text_data(filePath):
  f = open(filePath, 'r', encoding='UTF8')
  data = f.read()
  return data

def camel_case_split(str):
    words = [[str[0]]]
  
    for c in str[1:]:
        if words[-1][-1].islower() and c.isupper():
            words.append(list(' '+c))
        else:
            words[-1].append(c)

    words = [''.join(word) for word in words]
    words = [''.join(words)]

    return words

def stemmed_words(doc):
    stemmer = PorterStemmer()
    analyzer = TfidfVectorizer().build_analyzer()
    return (stemmer.stem(w) for w in analyzer(doc))

if __name__ == "__main__":
    file1 = load_text_data(sys.argv[1])
    file2 = load_text_data(sys.argv[2])

    camel = camel_case_split(file_1)
    came2 = camel_case_split(file_2)
    merge_code = camel + came2

    tfidf_vectorizer = TfidfVectorizer(lowercase = False, analyzer=stemmed_words)
    tfidv = tfidf_vectorizer.fit(merge_code)
    tfidfArray = tfidv.transform(merge_code).toarray()
    score = cosine_similarity(tfidfArray[0:1],tfidfArray[1:2]).squeeze()

    print(score)
