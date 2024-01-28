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

            // Create a cell for the finish time
            const finishTimeCell = createCell(row, '', { display: 'flex', justifyContent: 'space-between' });

            // Create a span for the finish time and append it to the finish time cell
            createSpan(finishTimeCell, finishTime);

            // Create a span for the finish date and append it to the finish time cell
            createSpan(finishTimeCell, finishDate, { fontSize: 'x-small', color: 'gray' });
        });
    }

    const initializePage = async () => {
        const participants = await fetchParticipants();
        populateTable(participants);
    }

    window.onload = initializePage;