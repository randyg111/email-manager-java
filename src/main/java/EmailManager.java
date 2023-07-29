import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ListMessagesResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/* class to demonstrate use of Gmail list labels API */
public class EmailManager {
  /**
   * Application name.
   */
  private static final String APPLICATION_NAME = "Email Manager";
  /**
   * Global instance of the JSON factory.
   */
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  /**
   * Directory to store authorization tokens for this application.
   */
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  /**
   * Global instance of the scopes required by this quickstart.
   * If modifying these scopes, delete your previously saved tokens/ folder.
   */
  private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
  private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

  /**
   * Directory to store email addresses whose emails should be deleted.
   */
  private static final String ADDRESSES_FILE_PATH = "addresses.txt";

  /**
   * Length of time to wait before email deletion.
   */
  private static final int EMAIL_DELETION_TIME = 7;

  /**
   * Creates an authorized Credential object.
   *
   * @param HTTP_TRANSPORT The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
      throws IOException {
    // Load client secrets.
    InputStream in = EmailManager.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
    }
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    //returns an authorized Credential object.
    return credential;
  }

  // Prompt for each address and deletion interval
  // Prompt to unsubscribe from unread emails
  public static void main(String... args) throws IOException, GeneralSecurityException {
    // Build a new authorized API client service.
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build();

    // Print the labels in the user's account.
    String user = "me";
    ListLabelsResponse listResponse = service.users().labels().list(user).execute();
    List<Label> labels = listResponse.getLabels();
    if (labels.isEmpty()) {
      System.out.println("No labels found.");
    } else {
      System.out.println("Labels:");
      for (Label label : labels) {
        System.out.printf("- %s\n", label.getName());
      }
    }

    // Create file with email addresses whose emails should be deleted
    Path addressesFile = Paths.get(EmailManager.class.getResource("/").getPath() + ADDRESSES_FILE_PATH);
    if (Files.notExists(addressesFile)) {
      Files.createFile(addressesFile);
    }
    Set<String> addresses = getAddresses(addressesFile);

    // Read emails
    ListMessagesResponse messagesResponse = service.users().messages().list(user).setQ("from:nytdirect@nytimes.com").setMaxResults(10l).execute();
    List<Message> messageIDs = messagesResponse.getMessages();
    List<Message> messages = new ArrayList<>();
    for (Message messageID : messageIDs) {
      messages.add(service.users().messages().get(user, messageID.getId()).execute());
    }
    for (Message message : messages) {
      System.out.printf("- %s\n", message.getSnippet());
    }

    // Write to addresses file if necessary
//    PrintWriter writer = new PrintWriter(Files.newBufferedWriter(addressesFile));
//
//    writer.close();
  }

  private static Set<String> getAddresses(Path addressesFile) throws IOException {
    BufferedReader reader = Files.newBufferedReader(addressesFile);
    Set<String> addresses = new HashSet<>();
    String line = reader.readLine();
    while ((line = reader.readLine()) != null) {
      addresses.add(line);
    }
    return addresses;
  }
}
