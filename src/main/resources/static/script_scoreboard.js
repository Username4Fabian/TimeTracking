fetch('/race-participants')
    .then(response => response.json())
    .then(data => {
        const table = document.getElementById('scoreboard');
        data.forEach((participant, index) => {
            const row = table.insertRow(-1);
            row.insertCell(0).textContent = index + 1;
            row.insertCell(1).textContent = participant.name;
            row.insertCell(2).textContent = participant.startNr;

            // Split the finish time into time and date
            const finishTimeParts = participant.finishTime.split('T');
            const finishTime = finishTimeParts[1];
            const finishDate = finishTimeParts[0];

            // Create a cell for the finish time
            const finishTimeCell = row.insertCell(3);
            finishTimeCell.style.display = 'flex';
            finishTimeCell.style.justifyContent = 'space-between';

            // Create a span for the finish time and append it to the finish time cell
            const finishTimeSpan = document.createElement('span');
            finishTimeSpan.textContent = finishTime;
            finishTimeCell.appendChild(finishTimeSpan);

            // Create a span for the finish date and append it to the finish time cell
            const finishDateSpan = document.createElement('span');
            finishDateSpan.textContent = finishDate;
            finishDateSpan.style.fontSize = 'x-small';
            finishDateSpan.style.color = 'gray';
            finishTimeCell.appendChild(finishDateSpan);
        });
    });