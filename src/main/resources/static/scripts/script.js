document.addEventListener('DOMContentLoaded', function() {
    let pageLoaded = true;

    populateVideoList().then(() => {
        const videoList = document.getElementById('video-list');
        videoList.lastChild.scrollIntoView();
    });

    const video = document.getElementById('video');
    const canvas = document.getElementById('canvas');
    const context = canvas.getContext('2d');
    let isMotionDetected = false;
    let prevImageData;
    let mediaRecorder;
    let recordedChunks = [];
    let motionTimeout; // Define the variable
    let motionDetectionActive = false;

    let recordingStartTime;
    let lastSentTime = Date.now();
    let sensitivity = 150;
    let motionDetectionDelay = false;

    const deleteVideosButton = document.getElementById('deleteVideos');
    const deleteRacerButton = document.getElementById('deleteRacer');
    const autoscrollCheckbox = document.getElementById('autoscroll');
    autoscrollCheckbox.checked = true;

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
        motionDetectionDelay = true;
        setTimeout(() => motionDetectionDelay = false, 1000);
    });

    document.getElementById('sensitivity-slider').addEventListener('input', function() {
        sensitivity = 300 - this.value;
    });

    window.addEventListener('blur', function() {
        if (pageLoaded) {
            motionDetectionDelay = true;
            setTimeout(() => motionDetectionDelay = false, 2000); // 2 seconds delay
        }
    });

    document.addEventListener('visibilitychange', function() {
        if (document.visibilityState === 'visible') {
            introduceMotionDetectionDelay();
        }
    });

    deleteVideosButton.addEventListener('click', async function() {
        try {
            const response = await fetch('/delete-videos', { method: 'DELETE' });
            if (response.ok) {
                console.log('All videos deleted');
                document.getElementById('video-list').innerHTML = '';
                await fetchVideos();
            } else {
                console.error('Failed to delete videos');
            }
        } catch (error) {
            console.error('Error deleting videos: ', error);
        }
    });

    deleteRacerButton.addEventListener('click', async function() {
        try {
            const response = await fetch('/race-participants', { method: 'DELETE' });
            if (response.ok) {
                console.log('All racers deleted');
            } else {
                console.error('Failed to delete racers');
            }
        } catch (error) {
            console.error('Error deleting racers: ', error);
        }
    });

    function introduceMotionDetectionDelay() {
        motionDetectionDelay = true; // Suspend motion detection
        setTimeout(() => {
            motionDetectionDelay = false; // Resume motion detection after the delay
            prevImageData = null; // Reset previous image data
        }, 2000); // Set delay to 2 seconds
    }


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
        recordingStartTime = new Date(); // Save the start time

        setTimeout(() => mediaRecorder.stop(), 4000); // Record for 4 seconds
    }

    async function sendVideoToServer(blob) {
        const currentTime = Date.now();
        // Check if 2 seconds have passed since the last video was sent
        if (currentTime - lastSentTime < 2000) {
            return;
        }

        const formData = new FormData();
        formData.append('video', blob, 'recorded.mp4');
        formData.append('startTime', recordingStartTime.toISOString()); // Send the start time
        try {
            const response = await fetch('/upload', {
                method: 'POST',
                body: formData
            });
            const text = await response.text();
            console.log('Server response: ', text);
            lastSentTime = Date.now();
        } catch (error) {
            console.error('Error sending video to server: ', error);
        }
        setTimeout(() => {
            // Refresh the list of videos
            const videoList = document.getElementById('video-list');
            videoList.innerHTML = '';
            populateVideoList();
        },5000);
    }

    function captureFrame() {
        if (!motionDetectionActive) {
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
        /* Both the motionDetectionDelay and the document
        hasFocus () are used to prevent motion detection
        while the page is initially loading or the user is
        not focused on the page */
        if (motionDetectionDelay || !document.hasFocus()) {
            return false;
        }

        let motionPixels = 0;
        for (let i = 0; i < currentImageData.data.length; i += 4) { // loop through the image data 
            let rDiff = Math.abs(prevImageData.data[i] - currentImageData.data[i]);
            let gDiff = Math.abs(prevImageData.data[i + 1] - currentImageData.data[i + 1]);
            let bDiff = Math.abs(prevImageData.data[i + 2] - currentImageData.data[i + 2]);

            if ((rDiff + gDiff + bDiff) > sensitivity) { // check if the difference is greater than the sensitivity
                motionPixels++;
            }
        }
        // return true if more than 2% of the pixels show motion
        return motionPixels > (currentImageData.width * currentImageData.height * 0.02);
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

    async function fetchVideos() {
        try {
            const response = await fetch('/videos');
            const videos = await response.json();
            return videos;
        } catch (error) {
            console.error('Error fetching videos: ', error);
        }
    }

    async function populateVideoList() {
        const videos = await fetchVideos();
        const videoList = document.getElementById('video-list');

        for (const video of videos) {
            const listItem = document.createElement('li');

            const thumbnail = document.createElement('img');
            thumbnail.src = video.thumbnailPath.substring(video.thumbnailPath.indexOf('uploaded-thumbnails'));
            listItem.appendChild(thumbnail);

            const videoDetails = document.createElement('div');
            videoDetails.className = 'video-details';

            const nameSpan = document.createElement('span');
            nameSpan.textContent = video.name;
            nameSpan.className = 'video-name';
            videoDetails.appendChild(nameSpan);

            const sizeSpan = document.createElement('span');
            sizeSpan.textContent = `File Size: ${(video.size / (1024 * 1024)).toFixed(2)} MB`;
            sizeSpan.className = 'video-size';
            videoDetails.appendChild(sizeSpan);

            const dateSpan = document.createElement('span');
            dateSpan.textContent = `Upload Date: ${new Date(video.uploadDate).toLocaleString()}`;
            dateSpan.className = 'video-date';
            videoDetails.appendChild(dateSpan);

            listItem.appendChild(videoDetails);

            const videoLink = document.createElement('a');
            videoLink.href = `/video-player.html?video=${video.name}`;
            videoLink.target = '_blank';
            videoLink.appendChild(listItem);

            videoList.appendChild(videoLink);

            if (autoscrollCheckbox.checked) {
                videoList.lastChild.scrollIntoView({ behavior: 'smooth' });
            }
        }
    }
});

function openScoreboard() {
    window.open('/scoreboard.html', '_blank');
}