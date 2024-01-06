const urlParams = new URLSearchParams(window.location.search);
const videoName = urlParams.get('video');
const videoPlayer = document.getElementById('video-player');
videoPlayer.src = `/uploaded-videos/${videoName}`;
videoPlayer.type = 'video/mp4';

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