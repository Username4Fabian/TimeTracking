# Temporary Documentation to keep track of the progress:

## Documentation:

#### What ServerSide language to use?

- **Java/Spring Boot:** Typically faster, better for high-concurrency and enterprise-scale applications.
- **Python/Django:** Great for rapid development and data-driven apps, but generally slower in raw performance compared to Java.

#### Java in use for first tests:

Java in use, due to better scalability. Working Cat Detection on Clientside.

#### Motion Detection instead of Boat detection:

https://codersblock.com/blog/motion-detection-with-javascript/


https://developer.mozilla.org/en-US/docs/Web/API/BlobEvent


New Idea:

- Insead of a 2 second buffer and a main recording:
- 2 main Recordings at the same time. These 2 recordings are 8 seconds and run 4 seconds appart. They run all the time. Whenever motion is detected, the recording that is closer to the current motion gets used.
- This can help with the issue of not being able to edit the chunk array since this cause corupted files
- The recording is then sent to the server

### Sources:
