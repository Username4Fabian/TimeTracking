let isMotionDetected = false;
let prevImageData;

document.addEventListener('DOMContentLoaded', function() {
    const video = document.getElementById('video');
    const canvas = document.getElementById('canvas');
    const context = canvas.getContext('2d');

    // Access the webcam
    if (navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ video: true })
            .then(function(stream) {
                video.srcObject = stream;
            })
            .catch(function(error) {
                console.log("Something went wrong!");
            });
    }

    function captureFrame() {
        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        let currentImageData = context.getImageData(0, 0, canvas.width, canvas.height);

        if (prevImageData) {
            isMotionDetected = detectMotion(prevImageData, currentImageData);
            updateMotionIndicator();
        }

        prevImageData = currentImageData;
        requestAnimationFrame(captureFrame);
    }

    function detectMotion(prevImageData, currentImageData) {
        let motionPixels = 0;
        for (let i = 0; i < currentImageData.data.length; i += 4) {
            let rDiff = Math.abs(prevImageData.data[i] - currentImageData.data[i]);
            let gDiff = Math.abs(prevImageData.data[i + 1] - currentImageData.data[i + 1]);
            let bDiff = Math.abs(prevImageData.data[i + 2] - currentImageData.data[i + 2]);

            if ((rDiff + gDiff + bDiff) > 150) { // Adjust sensitivity as needed
                motionPixels++;
            }
        }
        // Consider motion detected if a significant number of pixels have changed
        return motionPixels > (currentImageData.width * currentImageData.height * 0.02); // Adjust threshold as needed
    }

    let motionTimeout;

    function updateMotionIndicator() {
        const indicator = document.getElementById('motionIndicator');

        if (isMotionDetected) {
            indicator.style.backgroundColor = 'red';
            if (motionTimeout) {
                clearTimeout(motionTimeout);
            }
            motionTimeout = setTimeout(() => {
                isMotionDetected = false;
                indicator.style.backgroundColor = 'green';
            }, 250); // Delay of 1 second
        }
    }

    captureFrame();
});
