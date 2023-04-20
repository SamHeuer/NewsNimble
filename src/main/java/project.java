// for instgramClient
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

// for caption buffer encoding
import java.net.URLEncoder;

// for parsing json
import com.google.gson.*;

// for downloading image
import java.io.FileOutputStream;

// for editing image
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import java.awt.FontMetrics;
import java.awt.image.Kernel;
import java.awt.Color;
import java.io.File;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.GradientPaint;
import java.io.IOException;

// for uploading image
import com.cloudinary.*;
import com.cloudinary.utils.ObjectUtils;
import java.util.HashMap;

// for text-davinci-003
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

// for scheduling
import java.util.Timer;
import java.util.TimerTask;

class instagramAPIClient {
    private String instagramPageId;
    String facebookPageID="ADD YOUR FACEBOOK PAGE ID";
    private static final String APIKEY="ADD YOUR KEY HERE";

    private String postURL="";
    private String caption="";
    private String mediaCreationID;
    Gson gson = new Gson();

    void initInstagramBusinessAccountId() {
        try{
            String inputLine,callingURL=String.format("https://graph.facebook.com/v16.0/%s?fields=instagram_business_account&access_token=%s",facebookPageID,APIKEY);
            URL url = new URL(callingURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            System.out.println(response.toString());
            JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
            instagramPageId = jsonObject.getAsJsonObject("instagram_business_account").get("id").getAsString();
            System.out.println(instagramPageId);
        }
        catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }
    void setPostURL(String postURL){
        this.postURL=postURL;
    }
    void setCaption(String caption){
        this.caption=caption;
    }
    void initMediaContainer() {
        try{
            String callingURL =String.format("https://graph.facebook.com/v16.0/%s/media?image_url=%s&caption=%s&access_token=%s#BronzFonz",instagramPageId,postURL,caption,APIKEY);
            URL url = new URL(callingURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");

            String requestBody = "{\"key\":\"value\"}";

            OutputStream os = con.getOutputStream();
            byte[] requestBodyBytes = requestBody.getBytes("utf-8");
            os.write(requestBodyBytes, 0, requestBodyBytes.length);

            InputStreamReader inpStreamReader = new InputStreamReader(con.getInputStream(), "utf-8");
            BufferedReader br = new BufferedReader(inpStreamReader);
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            System.out.println(response.toString());

            JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
            mediaCreationID = jsonObject.get("id").getAsString();
            System.out.println("Media Creation ID : " + mediaCreationID+"\n\n");
        }
        catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    void postMediaContainer() {
        try{
            System.out.println("Posting Media Container\n");
            String inputLine,callingURL=String.format("https://graph.facebook.com/v16.0/%s/media_publish?creation_id=%s&access_token=%s",instagramPageId,mediaCreationID,APIKEY);
            URL url = new URL(callingURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);
        }
        catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }
}

class newsAPIClient {
    private static final String APIKEY = "ADD YOUR KEY HERE";

    private String caption=null;

    private String hostedUrl=null;

    private String publicImageName=null;
    Gson gson = new Gson();

    String textParserOpenAI(String jsonResponse){
        Gson gson = new Gson();
        JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);
        String text = response.getAsJsonArray("choices").get(0).getAsJsonObject().get("text").getAsString();
        return text.trim();
    }


    void textEncoder(String caption){
        try{
            this.caption= URLEncoder.encode(caption, "UTF-8");
        }
        catch (Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    String removePath(String url) {
        int lastSlashIndex = url.lastIndexOf('/');
        int lastDotIndex = url.lastIndexOf('.');
        if (lastSlashIndex == -1 || lastDotIndex == -1) {
            return url;
        }
        return url.substring(lastSlashIndex + 1, lastDotIndex);
    }

    public void newsAPIFetch(){
        try{
            String inputLine,callingURL=String.format("https://newsapi.org/v2/top-headlines?country=in&apiKey=%s",APIKEY);
            URL url = new URL(callingURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
            System.out.println("JSONOBJECT : "+jsonObject.toString());
            String  statusAPICall = jsonObject.get("status").getAsString();

            if(statusAPICall.compareTo("ok")==0){
                JsonArray articleJson = jsonObject.getAsJsonArray("articles");
                JsonObject highestPriorityArticleJSON = articleJson.get(2).getAsJsonObject();
                String sourceName="",authorName="",highestPriorityArticleDescription="",highestPriorityArticleUrlToImage="",highestPriorityArticleTitle="",highestPriorityArticleUrl="";
                if(highestPriorityArticleJSON.get("description").isJsonNull()){
                    throw new RuntimeException("Invalid description in response of newsapi.org");
                }
                else{
                    highestPriorityArticleDescription = highestPriorityArticleJSON.get("description").getAsString();
                }
                if(highestPriorityArticleJSON.get("urlToImage").isJsonNull()){
                    throw new RuntimeException("Invalid url in response of newsapi.org");
                }
                else{
                    highestPriorityArticleUrlToImage = highestPriorityArticleJSON.get("urlToImage").getAsString();
                    publicImageName = removePath(highestPriorityArticleUrlToImage) ;
                }
                if(highestPriorityArticleJSON.get("title").isJsonNull()){
                    throw new RuntimeException("Invalid title in response of newsapi.org");
                }
                else{
                    highestPriorityArticleTitle = highestPriorityArticleJSON.get("title").getAsString();
                }
                if(highestPriorityArticleJSON.get("url").isJsonNull()){
                    highestPriorityArticleUrl="Unknown";
                }
                else{
                    highestPriorityArticleUrl = highestPriorityArticleJSON.get("url").getAsString();
                }
                if(highestPriorityArticleJSON.get("source").getAsJsonObject().get("name").isJsonNull()){
                    sourceName="Unkown";
                }
                else{
                    sourceName = highestPriorityArticleJSON.get("source").getAsJsonObject().get("name").getAsString();
                }
                if(highestPriorityArticleJSON.get("author").isJsonNull()){
                    authorName="Unkown";
                }
                else{
                    authorName = highestPriorityArticleJSON.get("author").getAsString();
                }
                System.out.println(highestPriorityArticleJSON+"\n");

                System.out.println("Source : " + sourceName+"\n");
                System.out.println("Author : " + authorName+"\n\n");

                String headlinePayload = highestPriorityArticleTitle;
                String captionPayload = highestPriorityArticleDescription;
                System.out.println("Headline Payload : " + headlinePayload+"\n\n"+"Caption Payload : " + captionPayload+"\n");
                OpenAIRequest headlineOpenAIClient =new OpenAIRequest(headlinePayload);
                OpenAIRequest captionOpenAIClient =new OpenAIRequest(captionPayload);
                System.out.println("Requesting Open AI for headline\n");
                String headlineResponseOpenAI = headlineOpenAIClient.sendOpenAIRequest(20);
                Thread.sleep(10*1000);
                System.out.println("Requesting Open AI for caption\n");
                String captionResponseOpenAI = captionOpenAIClient.sendOpenAIRequest(200);

                if(headlineOpenAIClient==null || captionResponseOpenAI==null){
                    throw new RuntimeException("OpenAI.org returned an invalid response:\n ");
                }

                System.out.println(headlineResponseOpenAI+"\n");
                System.out.println(captionResponseOpenAI);


                String finalHeadline = textParserOpenAI(headlineResponseOpenAI);
                String tempCaption= textParserOpenAI(captionResponseOpenAI);

                imageTools imageObj = new imageTools();
                imageObj.downloadImage(highestPriorityArticleUrlToImage);
                imageObj.editImage(finalHeadline);
                System.out.println("Done with editing \n");

                hostedUrl = imageObj.uploadImage(publicImageName);
                Thread.sleep(15*1000);
                String finalCaption = tempCaption +"\n\n"+ "Source : " +sourceName+"\n\n"+"Author : "+authorName + "\n\n"+ "Article Source : "+ highestPriorityArticleUrl + "\n\n" + "Image Source : "+ highestPriorityArticleUrlToImage ;

                textEncoder(finalCaption);
                System.out.println("Finalized caption :\n" + finalCaption +"\n\n");
                System.out.println("Headline : " + finalHeadline+"\n\n");
                System.out.println("Public Image Name : " +publicImageName+"\n");

            }
            else{
                throw new RuntimeException("NewsAPI.org returned the following invalid response:\n "){
                    public String getMessage(){
                        return super.getMessage() + (jsonObject.getAsString());
                    }
                };
            }
        }
        catch(Exception e){
            System.out.println("Error: " + e.getMessage());
            System.exit(0);
        }
    }

    String getCaption(){
        if(this.caption!=null){
            return caption;
        }
        else{
            throw new RuntimeException("Invalid Caption\n ");
        }
    }
    String getHostedPostUrl(){
        if(this.hostedUrl!=null){
            return hostedUrl;
        }
        else{
            throw new RuntimeException("Invalid Post URL\n ");
        }
    }

    String getPublicImageName(){
        if(this.publicImageName!=null){
            return publicImageName;
        }
        else{
            throw new RuntimeException("Invalid Public Image Name URL\n ");
        }
    }
}

class imageTools {
    final String downloadPath=".\\articleImages\\downloadedImage\\image.jpg";
    final String editedPath=".\\articleImages\\editedImage\\image.jpg";
    final String logoPath=".\\articleImages\\logoBanner\\banner.png";

    static String uploadedImageName=null;

    void downloadImage(String urlString) {
        try {
            URL url = new URL(urlString);
            FileUtils.copyURLToFile(url, new File(downloadPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String uploadImage(String imageName){
        uploadedImageName=imageName;

        // Configure
        Map config = new HashMap();
        config.put("cloud_name", "ADD YOUR CLOUD NAME HERE");
        config.put("api_key", "ADD YOUR KEY HERE");
        config.put("api_secret", "ADD YOUR SECRET KEY HERE");
        Cloudinary cloudinary = new Cloudinary(config);

        // Upload
        try {
            cloudinary.uploader().upload(editedPath, ObjectUtils.asMap("public_id", uploadedImageName));
        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }

        // Transform
        String url = cloudinary.url().generate(this.uploadedImageName).toString();
        System.out.println(url);
        return url;
    }

    static void deleteImage(String imageName){
        Map config = new HashMap();
        config.put("cloud_name", "ADD YOUR CLOUD NAME HERE");
        config.put("api_key", "ADD YOUR KEY HERE");
        config.put("api_secret", "ADD YOUR SECRET KEY HERE");
        Cloudinary cloudinary = new Cloudinary(config);

        try {
            cloudinary.uploader().destroy(imageName, ObjectUtils.emptyMap());
            System.out.println("Image deleted successfully.");
        } catch (Exception e) {
            System.out.println("Error deleting image: " + e.getMessage());
        }
    }

    BufferedImage cropToSquare(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        return image.getSubimage(x, y, size, size);
    }
    BufferedImage blackGradient(BufferedImage image) {
        // Create gradient paint
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(0, 0, 0, 0),
                0, image.getHeight(), new Color(0, 0, 0, 255));

        // Draw gradient over bottom portion of image
        BufferedImage gradientImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = gradientImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();

        return gradientImage;
    }
    int wordLength(String word, FontMetrics fm){
        int count=0;
        for(int i=0;i<word.length();i++){
            count+=fm.charWidth(word.charAt(i));
        }
        return count;
    }
    String textWrapper(String text, Font font, int availableWidth) {
        FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).getGraphics().getFontMetrics(font);
        String[] words = text.split("\\s+");
        String op = "";
        int i = 0;
        while (i < words.length) {
            String line = "";
            int lineWidth = 0;
            while (i < words.length) {
                String word = words[i];
                int wordWidth = fm.stringWidth(word);
                if (lineWidth + wordWidth > availableWidth) {
                    break;
                }
                line += word + " ";
                lineWidth += wordWidth + fm.stringWidth(" ");
                i++;
            }
            if (line.isEmpty()) {
                // If a word is too long to fit in a single line, just add it to the output string without any wrapping
                op += words[i] + " ";
                i++;
            } else {
                op += line.trim() + "\n";
            }
        }
        System.out.println(op);
        return op;
    }

    BufferedImage resizeImage(BufferedImage image, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }
    BufferedImage addLogoToImage(BufferedImage image) throws IOException {
        // Load logo image
        BufferedImage logo = ImageIO.read(new File(logoPath));

        BufferedImage combinedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combinedImage.createGraphics();

        g.drawImage(image, 0, 0, null);

        // Ensure that logo fits within original image dimensions, and draw it onto combined image
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        int logoX = (int) (image.getWidth() * 0.02);
        int logoY = (int) (image.getHeight() * 0.02);
        double aspectRatio = (double) logo.getWidth() / logo.getHeight();
        int logoMaxWidth = (int) (image.getWidth() * 0.25);
        int logoMaxHeight = (int) (image.getHeight() * 0.25);
        int logoWidth = Math.min(logoMaxWidth, (int) (logoMaxHeight * aspectRatio));
        int logoHeight = Math.min(logoMaxHeight, (int) (logoMaxWidth / aspectRatio));
        g.drawImage(logo, logoX, logoY, logoWidth, logoHeight, null);


        g.dispose();
        return combinedImage;
    }

    void editImage(String text) {
        try {
            File imageFile = new File(downloadPath);
            BufferedImage image = ImageIO.read(imageFile);

            image = cropToSquare(image);
            image = blackGradient(image);
            image = addLogoToImage(image);

            int width = image.getWidth();
            int height = image.getHeight();

            Graphics2D g2 = image.createGraphics();

            int fontSize = (int) (Math.min(width, height) * 0.05);
            Font font = new Font("Roboto", Font.BOLD, fontSize);
            g2.setFont(font);
            g2.setColor(Color.WHITE);

            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);

            String wrappedText = textWrapper(text,font,image.getWidth()-10);

            String[] lines = wrappedText.split("\n");
            int x = 10;
            int y = (((int)(height*1.5) - fm.getHeight() * lines.length) / 2) + fm.getAscent();
            for (String line : lines) {
                g2.drawString(line, x, y);
                y += fm.getHeight();
            }

            ImageIO.write(image, "png", new File(editedPath));

            g2.dispose();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
class OpenAIRequest {
    private static final String APIKEY = "ADD YOUR KEY HERE";
    private static final String urlString = "https://api.openai.com/v1/engines/davinci/completions";
    private final HttpClient httpClient;
    String prompt="";
    OpenAIRequest(String prompt) {
        this.prompt=prompt;
        this.httpClient = HttpClient.newHttpClient();
    }
    String sendOpenAIRequest(int maxTokens){
        try {
            String payload = "{\"prompt\": \"" + prompt + "\", \"max_tokens\": " + maxTokens + ", \"temperature\": 0.5, \"top_p\": 0.5, \"frequency_penalty\": 1.0, \"presence_penalty\": 1.2}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + APIKEY)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
}

class project{
    public static void main(String args[]){
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    newsAPIClient newsObj = new newsAPIClient();
                    newsObj.newsAPIFetch();
                    String encodedCaption = newsObj.getCaption();
                    String hostedImageName = newsObj.getPublicImageName();
                    String hostedPostUrl = newsObj.getHostedPostUrl();
                    instagramAPIClient instagramObj = new instagramAPIClient();
                    instagramObj.initInstagramBusinessAccountId();
                    instagramObj.setPostURL(hostedPostUrl);
                    instagramObj.setCaption(encodedCaption);
                    instagramObj.initMediaContainer();
                    instagramObj.postMediaContainer();

                    System.out.println("Deleting cloudinary hosted file in 40 seconds");
                    Thread.sleep(40 * 1000);
                    imageTools.deleteImage(hostedImageName);
                    System.out.println("Program will run again in 2 hours");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(task, 0, 2 * 60 * 60 * 1000);
    }
}