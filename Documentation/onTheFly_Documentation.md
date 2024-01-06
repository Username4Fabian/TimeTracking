# Temporary Documentation to keep track of the progress:

## Documentation:

#### What ServerSide language to use?

- **Java/Spring Boot:** Typically faster, better for high-concurrency and enterprise-scale applications.
- **Python/Django:** Great for rapid development and data-driven apps, but generally slower in raw performance compared to Java.

#### Java in use for first tests:

Java in use, due to better scalability. Working Cat Detection on Clientside.

#### Motion Detection instead of Boat detection:

https://codersblock.com/blog/motion-detection-with-javascript/

### Using React:

https://www.youtube.com/watch?v=Tn6-PIqc4UM

https://legacy.reactjs.org/docs/faq-structure.html


### Thumbnail Generation:



To generate video thumbnails in a Spring Boot application, you can use the `Xuggler` library, which is a Java interface to FFmpeg. Here's a step-by-step guide:

1. Add the `Xuggler` dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>xuggle</groupId>
    <artifactId>xuggle-xuggler</artifactId>
    <version>5.4</version>
</dependency>
```

2. In your `VideoController`, add a method to generate a thumbnail after the video file has been saved:

```java
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.IContainer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

// ...

private void generateThumbnail(String videoPath, String thumbnailPath) {
    IMediaReader mediaReader = ToolFactory.makeReader(videoPath);
    mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
    mediaReader.addListener(new MediaListenerAdapter() {
        @Override
        public void onVideoPicture(IVideoPictureEvent event) {
            if (event.getTimeStamp() == 2) { // get frame at 2 seconds
                File outputfile = new File(thumbnailPath);
                try {
                    ImageIO.write(event.getImage(), "jpg", outputfile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaReader.removeListener(this); // remove listener to stop processing
            }
        }
    });
    while (mediaReader.readPacket() == null) ; // start processing
}
```

3. Call this method in your video upload handler, after the video file has been saved:

```java
String thumbnailPath = "/uploaded-thumbnails/" + fileName + ".jpg";
generateThumbnail(video.getPath(), thumbnailPath);
video.setThumbnailPath(thumbnailPath);
```

4. In your `Video` entity, add a field to store the thumbnail path:

```java
@Column(name = "thumbnail_path")
private String thumbnailPath;
```

5. In your client-side JavaScript, use this thumbnail path when creating the `img` element for the video thumbnail:

```javascript
thumbnail.src = video.thumbnailPath;
```

Please note that this is a simplified example and might need adjustments based on your actual application structure and requirements. Also, generating video thumbnails server-side would require additional computational resources.

### Sources:
