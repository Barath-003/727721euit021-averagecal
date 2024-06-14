const express = require('express');
const axios = require('axios');
const app = express();
const PORT = 9877; 
const WINDOW_SIZE = 10;
const TEST_SERVER_URL = 'http://20.244.56.144/test';
const numberStorage = [];

const fetchNumbers = async (type, authToken) => {
    const urlMap = {
        'p': 'primes',
        'f': 'fibo',
        'e': 'even',
        'r': 'rand'
    };

    const url = `${TEST_SERVER_URL}/${urlMap[type]}`;
    try {
        console.log(`Fetching numbers from ${url}`);
        const response = await axios.get(url, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            },
            timeout: 5000 
        });
        console.log(`Fetched numbers: ${response.data.numbers}`);
        return response.data.numbers;
    } catch (error) {
        console.error(`Error fetching numbers: ${error}`);
        throw error;
    }
};

const calculateAverage = (numbers) => {
    if (numbers.length === 0) return 0.0;
    const sum = numbers.reduce((acc, num) => acc + num, 0);
    return sum / numbers.length;
};

app.get('/numbers/:numberid', async (req, res) => {
    const { numberid } = req.params;
    const validIds = ['p', 'f', 'e', 'r'];
    if (!validIds.includes(numberid)) {
        return res.status(400).send({ error: 'Invalid number ID' });
    }

    const authHeader = req.headers['authorization'];
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).send({ error: 'Authorization header is missing or malformed' });
    }

    const accessToken = authHeader.split(' ')[1];
    try {
        const fetchedNumbers = await fetchNumbers(numberid, accessToken);
        const uniqueNumbers = fetchedNumbers.filter(num => !numberStorage.includes(num));

        const prevWindowState = [...numberStorage];

        numberStorage.push(...uniqueNumbers);

        console.log(`Numbers before trim: ${numberStorage}`);
        if (numberStorage.length > WINDOW_SIZE) {
            numberStorage.splice(0, numberStorage.length - WINDOW_SIZE);
        }

        console.log(`Numbers after trim: ${numberStorage}`);
        const avg = calculateAverage(numberStorage);

        res.json({
            windowPrevState: prevWindowState, 
            windowCurrState: [...numberStorage], 
            numbers: fetchedNumbers,
            avg: avg.toFixed(2)
        });
    } catch (error) {
        console.error('Error processing request:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});
