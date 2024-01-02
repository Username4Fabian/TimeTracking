document.addEventListener('DOMContentLoaded', function() {
    const video = document.getElementById('video');
    const canvas = document.getElementById('canvas');
    const context = canvas.getContext('2d');
    let isMotionDetected = false;
    let prevImageData;
    let mediaRecorder;
    let recordedChunks = [];
    let motionTimeout; // Define the variable
    let motionDetectionActive = false;

    if (navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ video: true })
            .then(function(stream) {
                video.srcObject = stream;
                setupRecorder(stream);
                // Start capturing frames continuously
                requestAnimationFrame(captureFrame);
            })
            .catch(function(error) {
                console.log("Something went wrong!", error);
            });
    }

    document.getElementById('toggle-motion').addEventListener('change', function() {
        this.classList.toggle('active');
        motionDetectionActive = this.checked;
    });

    let sensitivity = 150;

    document.getElementById('sensitivity-slider').addEventListener('input', function() {
        // Subtract the slider value from the sum of the maximum and minimum values to invert it
        sensitivity = 300 - this.value;
        // console.log('Sensitivity: ', sensitivity);
    });

    function setupRecorder(stream) {
        mediaRecorder = new MediaRecorder(stream);
        mediaRecorder.ondataavailable = function(event) {
            if (event.data.size > 0) {
                recordedChunks.push(event.data);
            }
        };

        mediaRecorder.onstop = function() {
            const blob = new Blob(recordedChunks, { type: 'video/webm' });
            sendVideoToServer(blob);
            recordedChunks = [];
        };
    }

    function startRecording() {
        recordedChunks = [];
        mediaRecorder.start();
        setTimeout(() => mediaRecorder.stop(), 4000); // Record for 4 seconds
    }

    let lastSentTime = Date.now();

    async function sendVideoToServer(blob) {
        const currentTime = Date.now();
        // Check if 2 seconds have passed since the last video was sent
        if (currentTime - lastSentTime < 2000) {
            return;
        }

        const formData = new FormData();
        formData.append('video', blob, 'recorded.mp4');
        try {
            const response = await fetch('/upload', {
                method: 'POST',
                body: formData
            });
            const text = await response.text();
            console.log('Server response: ', text);
            // Update the time of the last video sent
            lastSentTime = Date.now();
        } catch (error) {
            console.error('Error sending video to server: ', error);
        }
    }

    function captureFrame() {
        if (!motionDetectionActive) {
            // If motion detection is not active, request the next animation frame and return
            requestAnimationFrame(captureFrame);
            return;
        }

        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        let currentImageData = context.getImageData(0, 0, canvas.width, canvas.height);

        if (prevImageData) {
            isMotionDetected = detectMotion(prevImageData, currentImageData);
            updateMotionIndicator();
            if (isMotionDetected && mediaRecorder.state !== "recording") {
                startRecording();
            }
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

            if ((rDiff + gDiff + bDiff) > sensitivity) { // Use the sensitivity variable
                motionPixels++;
            }
        }
        // Consider motion detected if a significant number of pixels have changed
        return motionPixels > (currentImageData.width * currentImageData.height * 0.02); // Adjust thres
    }

    function updateMotionIndicator() {
        const video = document.getElementById('video');

        if (isMotionDetected) {
            video.style.borderColor = 'red';
            if (motionTimeout) {
                clearTimeout(motionTimeout);
            }
            motionTimeout = setTimeout(() => {
                isMotionDetected = false;
                video.style.borderColor = 'green';
            }, 250); // Delay of 1 second
        }
    }
});