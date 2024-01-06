const urlParams = new URLSearchParams(window.location.search);
const videoName = urlParams.get('video');
const videoPlayer = document.getElementById('video-player');
videoPlayer.src = `/uploaded-videos/${videoName}`;
videoPlayer.type = 'video/mp4';

const videoHeadline = document.getElementById('video-headline');
videoHeadline.textContent = `Playing:  ${videoName}`;

const prevVideoButton = document.getElementById('prev-video');
const nextVideoButton = document.getElementById('next-video');

prevVideoButton.addEventListener('click', loadPrevVideo);
nextVideoButton.addEventListener('click', loadNextVideo);

let videos = [];

fetch('/videos')
    .then(response => response.json())
    .then(data => {
        videos = data;
    })
    .catch((error) => {
        console.error('Error:', error);
    });

function loadPrevVideo() {
    const currentIndex = videos.findIndex(video => video.name === videoName);
    if (currentIndex > 0) {
        const prevVideo = videos[currentIndex - 1];
        window.location.href = `/video-player.html?video=${prevVideo.name}`;
    }
}

function loadNextVideo() {
    const currentIndex = videos.findIndex(video => video.name === videoName);
    if (currentIndex < videos.length - 1) {
        // There is a next video in the array, load it
        const nextVideo = videos[currentIndex + 1];
        window.location.href = `/video-player.html?video=${nextVideo.name}`;
    } else {
        // The current video is the last one in the array, fetch the videos from the server again
        fetch('/videos')
            .then(response => response.json())
            .then(data => {
                const newVideos = data;
                if (newVideos.length > videos.length) {
                    // There is a new video, add it to the array and load it
                    videos = newVideos;
                    const nextVideo = videos[currentIndex + 1];
                    window.location.href = `/video-player.html?video=${nextVideo.name}`;
                }
                // If there isn't a new video, do nothing
            })
            .catch((error) => {
                console.error('Error:', error);
            });
    }
}

function fetchVideos() {
    fetch('/videos')
        .then(response => response.json())
        .then(data => {
            if (data.length === 0) {
                // The database with the videos is cleared, clear the video list
                videos = [];
                alert('The database has been cleared. There are no videos to play.');
            } else {
                videos = data;
            }
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

// Attach the fetchVideos function to the click event of the buttons
prevVideoButton.addEventListener('click', fetchVideos);
nextVideoButton.addEventListener('click', fetchVideos);

const form = document.getElementById('race-participant-form');
form.addEventListener('submit', function(event) {
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

        const data = { startNr, finishTime: combinedDateTime, name };

        fetch('/race-participants', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        })
            .then(response => response.json())
            .then(data => {
                console.log('Success:', data);
            })
            .catch((error) => {
                console.error('Error:', error);
            });
    }
});

function formatUploadDate(dateString) {
    const date = new Date(dateString);
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

window.onload = function() {
    fetch('/videos')
        .then(response => response.json())
        .then(data => {
            videos = data;
            const video = videos.find(video => video.name === videoName);
            if (video) {
                const finishTimeInput = document.getElementById('finishTime');
                finishTimeInput.value = formatUploadDate(video.uploadDate);
            }
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function openScoreboard() {
    window.open('/scoreboard.html', '_blank');
}