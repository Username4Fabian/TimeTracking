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
    const databaseEntryTime = document.getElementById('databaseEntryTime').value;

    const data = { startNr, finishTime, name, databaseEntryTime };

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
});