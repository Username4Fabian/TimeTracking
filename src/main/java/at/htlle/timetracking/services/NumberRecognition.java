package at.htlle.timetracking.services;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.json.JSONObject;

/**
 * This class is responsible for recognizing numbers from an image.
 */
public class NumberRecognition {

    private final String apiKey;
    private final String imagePath;
    private final String customPrompt;

    /**
     * Constructor for the NumberRecognition class.
     *
     * @param apiKey       The API key for the OpenAI API.
     * @param imagePath    The path to the image file.
     * @param customPrompt The custom prompt for the OpenAI API.
     */
    public NumberRecognition(String apiKey, String imagePath, String customPrompt) {
        this.apiKey = apiKey;
        this.imagePath = imagePath;
        this.customPrompt = customPrompt;
    }

    /**
     * Encodes the image file to a Base64 string.
     *
     * @return The Base64 string of the image file.
     * @throws IOException If an I/O error occurs.
     */
    public String encodeImage() throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    /**
     * Gets the number from the image by sending a request to the OpenAI API.
     *
     * @return The number recognized from the image.
     * @throws IOException If an I/O error occurs.
     */
    public String getNumberFromImage() throws IOException {
        String base64Image = encodeImage();

        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);

        String payload = buildJsonPayload(base64Image);
        byte[] outputBytes = payload.getBytes(StandardCharsets.UTF_8);
        connection.getOutputStream().write(outputBytes);

        String response = new String(connection.getInputStream().readAllBytes());
        connection.disconnect();

        return extractNumberFromResponse(response);
    }

    /**
     * Builds the JSON payload for the OpenAI API request.
     *
     * @param base64Image The Base64 string of the image file.
     * @return The JSON payload as a string.
     */
    private String buildJsonPayload(String base64Image) {
        JSONObject payload = new JSONObject();
        payload.put("model", "gpt-4-vision-preview");
        payload.put("max_tokens", 3);

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", new JSONObject[]{
                new JSONObject().put("type", "text").put("text", customPrompt),
                new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", "data:image/jpeg;base64," + base64Image))
        });

        payload.put("messages", new JSONObject[]{message});
        return payload.toString();
    }

    /**
     * Extracts the number from the OpenAI API response.
     *
     * @param response The response from the OpenAI API.
     * @return The number recognized from the image.
     */
    private String extractNumberFromResponse(String response) {
        JSONObject responseJson = new JSONObject(response);
        return responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }
}
