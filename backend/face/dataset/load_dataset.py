import ntpath
import os
import pickle
from itertools import groupby
import cv2
import numpy as np
from imutils import paths
import hashlib
from load_face_detection.opencv_dnns import detect

def hash_files(file_list):
    hasher = hashlib.md5()
    for filename in sorted(file_list):
        with open(filename, 'rb') as f:
            buf = f.read()
            hasher.update(buf)
    return hasher.hexdigest()

def load_images(input_path, min_size):
    data_dir = ntpath.dirname(ntpath.abspath(__file__))
    data_file = ntpath.sep.join([data_dir, "faces.pickle"])
    hash_file = ntpath.sep.join([data_dir, "data_hash.txt"])
    
    image_paths = list(paths.list_images(input_path))
    current_hash = hash_files(image_paths)
    
    if os.path.exists(hash_file):
        with open(hash_file, "r") as f:
            saved_hash = f.read()
        if current_hash == saved_hash and os.path.exists(data_file):
            with open(data_file, "rb") as f:
                (faces, labels) = pickle.load(f)
            return faces, labels, False
    
    groups = groupby(image_paths, key=lambda path: ntpath.normpath(path).split(os.path.sep)[-2])
    faces = []
    labels = []
    
    for name, group_image_paths in groups:
        group_image_paths = list(group_image_paths)
        if len(group_image_paths) < min_size:
            continue
        
        for imagePath in group_image_paths:
            img = cv2.imread(imagePath)
            if img is None:
                print(f"Warning: Failed to load image '{imagePath}'. Skipping...")
                continue
            
            rects = detect(img)
            for rect in rects:
                (x, y, w, h) = rect["box"]
                roi = img[y:y + h, x:x + w]
                roi = cv2.resize(roi, (96, 96))
                faces.append(roi)
                labels.append(name)
    
    faces = np.array(faces)
    labels = np.array(labels)
    
    with open(data_file, "wb") as f:
        pickle.dump((faces, labels), f)
    
    with open(hash_file, "w") as f:
        f.write(current_hash)
    
    return faces, labels, True
