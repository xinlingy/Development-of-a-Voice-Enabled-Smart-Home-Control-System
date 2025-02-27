import ntpath
import sys
import pickle
import argparse
import numpy as np
import os
import time
from urllib.request import urlretrieve
import cv2
from imutils.video import WebcamVideoStream
from imutils.video import FPS
from sklearn.preprocessing import LabelEncoder
from sklearn.svm import SVC
import shutil
import warnings
warnings.filterwarnings("ignore", category=UserWarning, module='sklearn')

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
main_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
from load_face_detection.opencv_dnns import detect
from face_dataset.load_dataset import load_images
from load_face_detection.utils import *

embedder_model = main_dir + "/nn4.small2.v1.t7"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument(
        "-i",
        "--input",
        type=str,
        default=main_dir + "/face_dataset/caltech_faces",
        help=main_dir + "/face_dataset/caltech_faces",
    )
    ap.add_argument(
        "--image_path",
        type=str,
        help="Path to the image for face recognition",
    )
    args = vars(ap.parse_args())

    embedder = cv2.dnn.readNetFromTorch(embedder_model)

    print("[INFO] Loading dataset....")
    (faces, names, _) = load_images(args["input"], min_size=10)
    print(f"[INFO] {len(faces)} images in dataset")

    recognizer_path = os.path.join(main_dir, "recognizer.pickle")
    le_path = os.path.join(main_dir, "le.pickle")
    pickle_files_exist = os.path.exists(recognizer_path) and os.path.exists(le_path)

    if not pickle_files_exist:
        print("[ERROR] Model files not found. Please train the model first.")
        return

    with open(recognizer_path, "rb") as f:
        recognizer = pickle.load(f)
    with open(le_path, "rb") as f:
        le = pickle.load(f)

    if args["image_path"]:
        image_path = args["image_path"]
        if not os.path.exists(image_path):
            print(f"[ERROR] Image path {image_path} does not exist.")
            return

        print(f"[INFO] Recognizing faces in image: {image_path}")
        frame = cv2.imread(image_path)

        rects = detect(frame)
        recognized_name = None
        for rect in rects:
            (x, y, w, h) = rect["box"]
            roi = frame[y : y + h, x : x + w]
            faceBlob = cv2.dnn.blobFromImage(
                roi, 1.0 / 255, (96, 96), (0, 0, 0), swapRB=True, crop=False
            )
            embedder.setInput(faceBlob)
            vec = embedder.forward()

            preds = recognizer.predict_proba(vec)[0]
            i = np.argmax(preds)
            proba = preds[i]
            name = le.classes_[i]
            if proba < 0.70:
                recognized_name = "unknown"
            else:
                recognized_name = name

            text = "{}: {:.2f}%".format(name, proba * 100)
            _y = y - 10 if y - 10 > 10 else y + 10
            cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 0, 255), 2)
            cv2.putText(
                frame, text, (x, _y), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 0, 255), 2
            )

        if recognized_name:
            print(recognized_name)

if __name__ == "__main__":
    main()

