const urlParams = new URLSearchParams(window.location.search);
const videoName = urlParams.get('video');
const videoPlayer = document.getElementById('video-player');
const videoHeadline = document.getElementById('video-headline');
const prevVideoButton = document.getElementById('prev-video');
const nextVideoButton = document.getElementById('next-video');
const form = document.getElementById('race-participant-form');

let videos = [];

const setVideoSource = (videoName) => {
    videoPlayer.src = `/uploaded-videos/${videoName}`;
    videoPlayer.type = 'video/mp4';
    videoHeadline.textContent = `Playing:  ${videoName}`;
}

const loadVideo = (videoIndex) => {
    if (videoIndex >= 0 && videoIndex < videos.length) {
        const video = videos[videoIndex];
        window.location.href = `/video-player.html?video=${video.name}`;
    }
}

const loadPrevVideo = () => {
    const currentIndex = videos.findIndex(video => video.name === videoName);
    loadVideo(currentIndex - 1);
}

const loadNextVideo = () => {
    const currentIndex = videos.findIndex(video => video.name === videoName);
    loadVideo(currentIndex + 1);
}

const submitForm = async (event) => {
    event.preventDefault();

    const startNr = document.getElementById('startNr').value;
    const finishTime = document.getElementById('finishTime').value;
    const name = document.getElementById('name').value;

    const video = videos.find(video => video.name === videoName);
    if (video) {
        const uploadDate = new Date(video.uploadDate);
        const finishTimeParts = finishTime.split(':');
        const hours = parseInt(finishTimeParts[0]);
        const minutes = parseInt(finishTimeParts[1]);
        const seconds = parseInt(finishTimeParts[2].split('.')[0]);
        const milliseconds = parseInt(finishTimeParts[2].split('.')[1]);

        uploadDate.setHours(hours, minutes, seconds, milliseconds);
        const combinedDateTime = uploadDate.toISOString();

        const formData = { startNr, finishTime: combinedDateTime, name };

        try {
            const response = await fetch('/race-participants', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            });
            const responseData = await response.json();
            console.log('Success:', responseData);
        } catch (error) {
            console.error('Error:', error);
        }
    }
}

const formatUploadDate = (dateString) => {
    let date = new Date(dateString);
    date.setSeconds(date.getSeconds() - 4); // Subtract 4 seconds processing time

    let hours = date.getHours();
    let minutes = date.getMinutes();
    let seconds = date.getSeconds();
    let milliseconds = date.getMilliseconds();

    // Pad the hours, minutes, seconds and milliseconds with leading zeros if they are less than 10
    hours = hours < 10 ? '0' + hours : hours;
    minutes = minutes < 10 ? '0' + minutes : minutes;
    seconds = seconds < 10 ? '0' + seconds : seconds;
    milliseconds = milliseconds < 100 ? (milliseconds < 10 ? '00' + milliseconds : '0' + milliseconds) : milliseconds;

    return `${hours}:${minutes}:${seconds}.${milliseconds}`; // Return the time part in the format HH:MM:SS.sss
}

const initializePage = async () => {
    try {
        const response = await fetch('/videos');
        const data = await response.json();
        videos = data;
        const video = videos.find(video => video.name === videoName);
        if (video) {
            const finishTimeInput = document.getElementById('finishTime');
            finishTimeInput.value = formatUploadDate(video.uploadDate);

            const startNrInput = document.getElementById('startNr');
            startNrInput.value = video.startNr;

            // Display a toast notification if the number is 0
            if (video.startNr === 0) {
                toastr.warning('The start number is 0. No number was detected automatically. Please enter the start number manually.');
            }
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

const openScoreboard = () => {
    window.open('/scoreboard.html', '_blank');
}

// Event listeners
prevVideoButton.addEventListener('click', loadPrevVideo);
nextVideoButton.addEventListener('click', loadNextVideo);
form.addEventListener('submit', submitForm);
window.onload = initializePage;

setVideoSource(videoName);

toastr.options = {
    "positionClass": "toast-bottom-full-width"
}