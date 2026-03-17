package com.proelectricos.restintce.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class GoogleDriveConfig {

    private static final String APPLICATION_NAME = "RestIntCE";
    private static final String CREDENTIALS_FILE_PATH = "C:/Users/C-016/Documents/java/RestIntCE/credentials.json"; // Ruta absoluta al archivo de credenciales

    @Bean
    public Drive googleDriveService() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS_FILE_PATH))
                .createScoped(DriveScopes.DRIVE);

        return new Drive.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
