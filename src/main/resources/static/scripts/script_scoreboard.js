    const fetchParticipants = async () => {
        try {
            const response = await fetch('/race-participants');
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('Error:', error);
        }
    }

    const createCell = (row, textContent, styles = {}) => {
        const cell = row.insertCell(-1);
        cell.textContent = textContent;
        Object.assign(cell.style, styles);
        return cell;
    }

    const createSpan = (parent, textContent, styles = {}) => {
        const span = document.createElement('span');
        span.textContent = textContent;
        Object.assign(span.style, styles);
        parent.appendChild(span);
        return span;
    }

    const populateTable = (participants) => {
        const table = document.getElementById('scoreboard');
        participants.forEach((participant, index) => {
            const row = table.insertRow(-1);
            createCell(row, index + 1);
            createCell(row, participant.name);
            createCell(row, participant.startNr);

            // Split the finish time into time and date
            const finishTimeParts = participant.finishTime.split('T');
            const finishTime = finishTimeParts[1];
            const finishDate = finishTimeParts[0];

            const adjustedFinishTime = adjustTimeForWinter(finishDate, finishTime);

            // Create a cell for the finish time
            const finishTimeCell = createCell(row, '', { display: 'flex', justifyContent: 'space-between' });

            // Create a span for the finish time and append it to the finish time cell
            createSpan(finishTimeCell, adjustedFinishTime);

            // Create a span for the finish date and append it to the finish time cell
            createSpan(finishTimeCell, finishDate, { fontSize: 'x-small', color: 'gray' });
        });
    }

    const initializePage = async () => {
        const participants = await fetchParticipants();
        populateTable(participants);
    }

    window.onload = initializePage;

    function adjustTimeForWinter(dateString, timeString) {
        const dateParts = dateString.split('-'); // Assuming the date format is YYYY-MM-DD
        const timeParts = timeString.split(':'); // Assuming the time format is HH:MM:SS
    
        const date = new Date(dateParts[0], dateParts[1] - 1, dateParts[2]);
        const year = date.getFullYear();
    
        // Calculate the start and end dates of DST for the given year in Austria
        const dstStart = new Date(year, 2, 31 - (new Date(year, 2, 31).getDay())); // Last Sunday in March
        const dstEnd = new Date(year, 9, 31 - (new Date(year, 9, 31).getDay())); // Last Sunday in October
    
        // Check if the date is outside DST period
        const isOutsideDST = date < dstStart || date >= dstEnd;
    
        if (isOutsideDST) {
            let hour = parseInt(timeParts[0], 10) + 1;
            if (hour === 24) hour = 0; // Reset hour if it exceeds 23
            timeParts[0] = hour.toString().padStart(2, '0'); // Adjust hour and ensure it's a 2-digit string
        }
    
        return timeParts.join(':'); // Return the adjusted time as a string
    }
    