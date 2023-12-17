let video = document.getElementById("videoInput");
let canvas = document.getElementById("canvasOutput");
let context = canvas.getContext("2d");
let src, dst, cap, catCascade;
let FPS = 30; // Define FPS at the top of your

let scaleFactor = 1.1;
let minNeighbors = 8;
let minSize = new cv.Size(60, 60);
let maxSize = new cv.Size(200, 200);

// Function to load a file using AJAX
function loadCascadeFile(url) {
    return new Promise((resolve, reject) => {
        let xhr = new XMLHttpRequest();
        xhr.open("GET", url, true);
        xhr.responseType = "arraybuffer";
        xhr.onload = function (e) {
            if (xhr.status === 200) {
                let data = new Uint8Array(xhr.response);
                resolve(data);
            } else {
                reject("Failed to load file");
            }
        };
        xhr.send(null);
    });
}

// ... (earlier part of the script remains unchanged)

// Initialize OpenCV objects and load the cascade file
function onOpenCvReady() {
    src = new cv.Mat(video.height, video.width, cv.CV_8UC4);
    dst = new cv.Mat(video.height, video.width, cv.CV_8UC1);
    cap = new cv.VideoCapture(video);

    // Load the cat face cascade file using AJAX
    loadCascadeFile('haarcascade_frontalcatface_extended.xml')
        .then(data => {
            cv.FS_createDataFile('/', 'haarcascade_frontalcatface_extended.xml', data, true, false, false);
            catCascade = new cv.CascadeClassifier();
            catCascade.load('haarcascade_frontalcatface_extended.xml');

            // Start the video after the cascade file is loaded
            startVideo();
        })
        .catch(error => console.error("An error occurred while loading the cascade file:", error));
}

// Start video stream from the webcam
function startVideo() {
    navigator.mediaDevices.getUserMedia({ video: true, audio: false })
        .then(stream => {
            video.srcObject = stream;
            processVideo(); // Removed catCascade from the argument list
        })
        .catch(err => console.error("An error occurred: " + err));
}

function processVideo() {
    let begin = Date.now();

    // Start processing.
    cap.read(src);
    cv.cvtColor(src, dst, cv.COLOR_RGBA2GRAY);
    let faces = new cv.RectVector();
    let numDetections = new cv.IntVector();

    // Adjust the parameters to decrease certainty and detect smaller objects
    let minNeighbors = 5; // Decrease the number of neighbors
    let minSize = new cv.Size(10, 10); // Decrease the minimum size

    catCascade.detectMultiScale2(dst, faces, numDetections, scaleFactor, minNeighbors, 0, minSize, maxSize);
    // Draw borders around detected cats
    for (let i = 0; i < faces.size(); ++i) {
        let face = faces.get(i);
        let detectionCount = numDetections.get(i);
        // Only consider detections with a high certainty (e.g., detectionCount > 20)
        if (detectionCount > 20) {
            let point1 = new cv.Point(face.x, face.y);
            let point2 = new cv.Point(face.x + face.width, face.y + face.height);
            cv.rectangle(src, point1, point2, [255, 0, 0, 255]);
            // Print the detection count on the image
            let textPoint = new cv.Point(face.x, face.y > 20 ? face.y - 10 : face.y + 10);
            cv.putText(src, "Certainty: " + detectionCount, textPoint, cv.FONT_HERSHEY_SIMPLEX, 0.5, [255, 0, 0, 255]);
        }
    }

    cv.imshow("canvasOutput", src);
    faces.delete();
    numDetections.delete();

    // Schedule the next frame processing
    let delay = 1000/FPS - (Date.now() - begin);
    setTimeout(processVideo, delay);
}

// Load OpenCV.js
cv['onRuntimeInitialized'] = onOpenCvReady;
