document.addEventListener('DOMContentLoaded', function() {
    const video = document.getElementById('video');
    const canvas = document.getElementById('canvas');
    const context = canvas.getContext('2d', { willReadFrequently: true });
    let isMotionDetected = false;
    let prevImageData;
    let mediaRecorder;
    let recordedChunks = [];
    let motionTimeout; // Define the variable


    let backupBuffer = [];

    let motionisDetected = false;
    let Recorder1;
    let Recorder2;
    let recordedChunks1 = [];
    let recordedChunks2 = [];

    let lastMotionDetectedAt; // Add this line at the top of your script

    // Access the webcam
    if (navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ video: true })
            .then(function(stream) {
                video.srcObject = stream;
                setupRecorder1(stream);
                setupRecorder2(stream);
                startRecording();
                captureFrame();
            })
            .catch(function(error) {
                console.log("Something went wrong!", error);
            });
    }

      setInterval(() => {
        if(motionisDetected) {
                onMotionDetected();
        }
    },4000);



    let recorder1StartAt = 0, recorder1StopAt = 0;
    let recorder2StartAt = 0, recorder2StopAt = 0;

    let motionStartTime = null; // Add this line at the top of your script

    let isSendingVideo = false; // Add this line at the top of your script

    let motionEnded = false; // Add this line at the top of your script

    let motionEndTime = null; // Add this line at the top of your script

    function onMotionDetected() {
        const motionTime = Date.now();
        const recorder1Diff = Math.abs((motionTime - recorder1StartAt) - 4000) + Math.abs((recorder1StopAt - motionTime) - 4000);
        const recorder2Diff = Math.abs((motionTime - recorder2StartAt) - 4000) + Math.abs((recorder2StopAt - motionTime) - 4000);

        // If motionStartTime is null, set it to the current time
        if (!motionStartTime) {
            motionStartTime = motionTime;
        }

        // Check if at least 4 seconds have passed since motionEndTime, if a video is not currently being sent, and if motion has ended
        if (motionEndTime && motionTime - motionEndTime >= 7000 && !isSendingVideo) {
            if (recorder1Diff < recorder2Diff) {
                // Send Recorder1's video
                sendVideoToServer(new Blob(recordedChunks1, { type: 'video/webm' }));
            } else {
                // Send Recorder2's video
                sendVideoToServer(new Blob(recordedChunks2, { type: 'video/webm' }));
            }
            motionisDetected = false;
            motionStartTime = null; // Reset motionStartTime
            motionEndTime = null; // Reset motionEndTime
        }
    }




    function setupRecorder1(stream) {
        Recorder1 = new MediaRecorder(stream);
        Recorder1.ondataavailable = function(event) {
            if (event.data.size > 0) {
                recordedChunks1.push(event.data);
            }
        };
        Recorder1.onstart = function() {
            recorder1StartAt = Date.now();

            setTimeout(() => {
                Recorder1.stop(200);
            } , 8000);
        };


    Recorder1.onstop = function() {
        recorder1StopAt = Date.now();

        // const blob = new Blob(recordedChunks1, { type: 'video/webm' });
        // sendVideoToServer(blob);
        recordedChunks1 = [];
        Recorder1.start(200);
        setTimeout(() => {
            Recorder1.stop();
        } , 8000);
    };
}


    function setupRecorder2(stream) {d
        Recorder2 = new MediaRecorder(stream);
        Recorder2.ondataavailable = function(event) {
            if (event.data.size > 0) {
                recordedChunks2.push(event.data);
            }
        };
        Recorder2.onstart = function() {
            recorder2StartAt = Date.now();

            setTimeout(() => {
                Recorder2.stop();
            } , 8000);
        };


        Recorder2.onstop = function() {
            recorder2StopAt = Date.now();

            // const blob = new Blob(recordedChunks2, {type: 'video/webm'});
            // sendVideoToServer(blob);
            recordedChunks2 = [];
            Recorder2.start(200);
        };
    }


    // function gets called when Motion is detected and starts mediaRecorder for 4.5 seconds
    function startRecording() {
        Recorder1.start(200);
        setTimeout(() => {
            Recorder2.start(200);
        }, 4000);
    }

   async  function sendVideoToServer(blob) {
        motionisDetected = false; // Reset the flag here
        isSendingVideo = true; // Set the flag to true when sending a video
        const formData = new FormData();
        formData.append('video', blob, 'recorded.mp4');
        try {
            const response = await fetch('/upload', {
                method: 'POST',
                body: formData
            });
            const text = await response.text();
            console.log('Server response: ', text);
        } catch (error) {
            console.error('Error sending video to server: ', error);
        }
        //recordedChunks = [];

        motionStartTime = null; // Reset motionStartTime
        isSendingVideo = false; // Set the flag back to false when the video has been sent
    }

    function captureFrame() {
        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        let currentImageData = context.getImageData(0, 0, canvas.width, canvas.height);

        if (prevImageData) {
            isMotionDetected = detectMotion(prevImageData, currentImageData);
            updateMotionIndicator();
            if (isMotionDetected) {
                motionisDetected = true;
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

            if ((rDiff + gDiff + bDiff) > 150) { // Adjust sensitivity as needed
                motionPixels++;
            }
        }
        // Consider motion detected if a significant number of pixels have changed
        let motionDetected = motionPixels > (currentImageData.width * currentImageData.height * 0.02); // Adjust threshold
        if (!motionDetected) {
            if (!motionEndTime) {
                motionEndTime = Date.now(); // Set motionEndTime to the current time if no motion is detected
            } else if (Date.now() - motionEndTime >= 4000) {
                motionEnded = true; // Set motionEnded to true if no motion is detected for 4 seconds
            }
        } else {
            motionEndTime = null; // Reset motionEndTime if motion is detected
        }
        return motionDetected;
    }

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
});