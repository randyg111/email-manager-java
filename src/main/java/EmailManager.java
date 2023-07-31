import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.MessagePartHeader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import org.apache.commons.codec.binary.Base64;

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
   * Directory to store email addresses whose emails should be deleted or archived.
   */
  private static final String ADDRESSES_DELETE_FILE_PATH = "addresses_delete.txt";
  private static final String ADDRESSES_ARCHIVE_FILE_PATH = "addresses_archive.txt";

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

  // Prompt to unsubscribe from unread emails
  // Remove ads
  // Add synchronization
  // Add gui
  // Chrome extension
  public static void main(String... args) throws IOException, GeneralSecurityException, InterruptedException, MessagingException {
    // Build a new authorized API client service.
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build();

    // Set user
    String user = "me";

    Message message = createMessageWithEmail(createEmail("", "", "", ""));
    try {
      // Create send message
      message = service.users().messages().send("me", message).execute();
      System.out.println("Message id: " + message.getId());
      System.out.println(message.toPrettyString());
    } catch (GoogleJsonResponseException e) {
      // TODO(developer) - handle error appropriately
      GoogleJsonError error = e.getDetails();
      System.err.println(error.getMessage());
    }

//    // Create file with email addresses whose emails should be deleted
//    Path addressesDeleteFile = Paths.get(EmailManager.class.getResource("/").getPath() + ADDRESSES_DELETE_FILE_PATH);
//    if (Files.notExists(addressesDeleteFile)) {
//      Files.createFile(addressesDeleteFile);
//    }
//    Set<String> addressesDelete = getAddresses(addressesDeleteFile);
//
//    Path addressesArchiveFile = Paths.get(EmailManager.class.getResource("/").getPath() + ADDRESSES_ARCHIVE_FILE_PATH);
//    if (Files.notExists(addressesArchiveFile)) {
//      Files.createFile(addressesArchiveFile);
//    }
//    Set<String> addressesDelete = getAddresses(addressesArchiveFile);
//
//    // Write to addresses files if necessary
//    PrintWriter writer = new PrintWriter(Files.newBufferedWriter(addressesDeleteFile));
//    PrintWriter writer = new PrintWriter(Files.newBufferedWriter(addressesArchiveFile));
//
//    // Prompt on number of days after which to delete emails
//    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//    long deletionAge = 7;
//    System.out.println("Set number of days for automatic email deletion: (enter for default of 7 days)");
//    String line = reader.readLine();
//    if (!line.equals("")) {
//      deletionAge = Long.parseLong(line);
//    }
//
//    // Set date
//    LocalDate localDate = LocalDate.now().minusDays(deletionAge);
//    String date = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
//
//    // Loop through emails, making 25 batch calls a second
//    String pageToken = null;
//    do {
//      long start = System.nanoTime();
//      ListMessagesResponse messagesResponse = null;
//      if (pageToken == null) {
//        messagesResponse = service.users().messages().list(user).setQ("before:"+date).setMaxResults(25l).execute();
//      } else {
//        messagesResponse = service.users().messages().list(user).setPageToken(pageToken).setMaxResults(25l).execute();
//      }
//      pageToken = messagesResponse.getNextPageToken();
//
//      List<Message> messages = getMessages(messagesResponse.getMessages(), service, user);
//      for (Message message : messages) {
//        // Prompt on whether to delete or archive emails
//        System.out.printf("Delete emails from %s after %l days? (enter for yes, any other input for no)%n", );
//        Map<String, String> headersMap = getHeadersMap(message);
//        System.out.printf("- %s\n", headersMap.get("From"));
//      }
//      long timeTaken = System.nanoTime() - start;
//      if (timeTaken < 1000000000) {
//        long timeSleep = 1000000000 - timeTaken;
//        Thread.sleep(timeSleep / 1000000, (int) (timeSleep % 1000000));
//      }
//    } while (pageToken != null);
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

  private static List<Message> getMessages(List<Message> partials, Gmail service, String user) throws IOException {
    List<Message> messages = new ArrayList<>();
    JsonBatchCallback<Message> callback = new JsonBatchCallback<Message>() {
      public void onSuccess(Message message, HttpHeaders responseHeaders) {
        messages.add(message);
      }

      public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
        System.err.println(e.getMessage());
      }
    };

    BatchRequest batch = service.batch();
    for (Message partial : partials) {
      service.users().messages().get(user, partial.getId()).queue(batch, callback);
    }
    batch.execute();

    return messages;
  }

  private static Map<String, String> getHeadersMap(Message message) {
    List<MessagePartHeader> headers = message.getPayload().getHeaders();
    Map<String, String> headersMap = new HashMap<>();
    for (MessagePartHeader header : headers) {
      headersMap.put(header.getName(), header.getValue());
    }
    return headersMap;
  }

  /**
   * Create a MimeMessage using the parameters provided.
   *
   * @param toEmailAddress   email address of the receiver
   * @param fromEmailAddress email address of the sender, the mailbox account
   * @param subject          subject of the email
   * @param bodyText         body text of the email
   * @return the MimeMessage to be used to send email
   * @throws MessagingException - if a wrongly formatted address is encountered.
   */
  public static MimeMessage createEmail(String toEmailAddress,
                                        String fromEmailAddress,
                                        String subject,
                                        String bodyText)
          throws MessagingException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

    MimeMessage email = new MimeMessage(session);

    email.setFrom(new InternetAddress(fromEmailAddress));
    email.addRecipient(javax.mail.Message.RecipientType.TO,
            new InternetAddress(toEmailAddress));
    email.setSubject(subject);
    email.setText(bodyText);
    return email;
  }

  /**
   * Create a message from an email.
   *
   * @param emailContent Email to be set to raw of message
   * @return a message containing a base64url encoded email
   * @throws IOException        - if service account credentials file not found.
   * @throws MessagingException - if a wrongly formatted address is encountered.
   */
  public static Message createMessageWithEmail(MimeMessage emailContent)
          throws MessagingException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    emailContent.writeTo(buffer);
    byte[] bytes = buffer.toByteArray();
    String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
    Message message = new Message();
    message.setRaw(encodedEmail);
    return message;
  }
}
