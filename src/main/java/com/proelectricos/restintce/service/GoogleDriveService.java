package com.proelectricos.restintce.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Servicio dedicado a operaciones con Google Drive:
 * creación de carpetas y subida de archivos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveService {

    private final Drive driveClient;

    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private static final String EXCEL_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Crea una carpeta dentro de una carpeta padre en Google Drive.
     *
     * @param folderName     nombre de la carpeta a crear
     * @param parentFolderId ID de la carpeta padre en Drive
     * @return ID de la carpeta creada
     */
    public String createFolder(String folderName, String parentFolderId) throws IOException {
        try {
            String existingFolderId = findFolderIdByName(folderName, parentFolderId);
            if (existingFolderId != null) {
                log.info("Carpeta ya existe en Drive: '{}' (ID: {})", folderName, existingFolderId);
                return existingFolderId;
            }

            File folderMetadata = new File()
                    .setName(folderName)
                    .setMimeType(FOLDER_MIME_TYPE)
                    .setParents(Collections.singletonList(parentFolderId));

            File created = driveClient.files().create(folderMetadata)
                    .setSupportsAllDrives(true)
                    .setFields("id")
                    .execute();

            log.info("Carpeta creada en Drive: '{}' (ID: {})", folderName, created.getId());
            return created.getId();

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.error("Carpeta padre no encontrada (ID: {}). Verifica que la carpeta exista y esté compartida con la cuenta de servicio.", parentFolderId);
                throw new IOException("La carpeta raíz de Drive (ID: " + parentFolderId + ") no fue encontrada. "
                        + "Comparte la carpeta con la cuenta de servicio en Google Drive.", e);
            }
            throw e;
        }
    }

    private static final List<String> SUBFOLDERS = List.of("OFERTA", "PROVEEDOR", "ESPECIFICACIONES", "PLANO", "CORREOS");

    /**
     * Crea las subcarpetas estándar dentro de una carpeta padre y retorna el ID de OFERTA.
     * Si ya existen, simplemente retorna el ID de OFERTA.
     */
    public String createSubFolders(String parentFolderId) throws IOException {
        String ofertaId = null;
        for (String sub : SUBFOLDERS) {
            String id = createFolder(sub, parentFolderId);
            if ("OFERTA".equals(sub)) {
                ofertaId = id;
            }
        }
        return ofertaId;
    }

    /**
     * Busca un archivo (no carpeta) por nombre en una carpeta y retorna su ID, o null si no existe.
     */
    public String findFileIdByName(String folderId, String fileName) throws IOException {
        String query = String.format(
                "name = '%s' and '%s' in parents and mimeType != '%s' and trashed = false",
                escapeDriveQueryValue(fileName),
                folderId,
                FOLDER_MIME_TYPE
        );

        log.info("Buscando archivo en Drive - carpeta: {}, nombre: '{}'", folderId, fileName);

        FileList result = driveClient.files().list()
                .setQ(query)
                .setCorpora("allDrives")
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id,name)")
                .setPageSize(1)
                .execute();

        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            log.info("Archivo NO encontrado en Drive: '{}'", fileName);
            return null;
        }
        log.info("Archivo encontrado en Drive: '{}' (ID: {})", fileName, files.get(0).getId());
        return files.get(0).getId();
    }

    /**
     * Copia un archivo existente en Drive con un nuevo nombre dentro de la carpeta destino.
     *
     * @param sourceFileId ID del archivo origen en Drive
     * @param newFileName  nombre del nuevo archivo
     * @param folderId     carpeta destino
     * @return webViewLink del archivo copiado
     */
    public String copyFile(String sourceFileId, String newFileName, String folderId) throws IOException {
        File copyMetadata = new File()
                .setName(newFileName)
                .setParents(Collections.singletonList(folderId));

        File copied = driveClient.files().copy(sourceFileId, copyMetadata)
                .setSupportsAllDrives(true)
                .setFields("id, webViewLink")
                .execute();

        log.info("Archivo copiado en Drive: '{}' -> {}", newFileName, copied.getWebViewLink());
        return copied.getWebViewLink();
    }

    /**
     * Verifica si ya existe un archivo con ese nombre en una carpeta dada de Drive.
     */
    public boolean fileExistsInFolder(String folderId, String fileName) throws IOException {
        String query = String.format(
                "name = '%s' and '%s' in parents and trashed = false",
                escapeDriveQueryValue(fileName),
                folderId
        );

        FileList result = driveClient.files().list()
                .setQ(query)
                .setCorpora("allDrives")
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id)")
                .setPageSize(1)
                .execute();

        return result.getFiles() != null && !result.getFiles().isEmpty();
    }

    /**
     * Sube un archivo Excel a una carpeta específica de Google Drive.
     *
     * @param localFilePath ruta local del archivo a subir
     * @param fileName      nombre con el que se guardará en Drive
     * @param folderId      ID de la carpeta destino en Drive
     * @return webViewLink del archivo subido
     */
    public String uploadFile(String localFilePath, String fileName, String folderId) throws IOException {
        java.io.File localFile = new java.io.File(localFilePath);
        if (!localFile.exists()) {
            throw new IOException("El archivo local no existe: " + localFilePath);
        }

        File fileMetadata = new File()
                .setName(fileName)
                .setParents(Collections.singletonList(folderId));

        FileContent mediaContent = new FileContent(EXCEL_MIME_TYPE, localFile);

        File uploaded = driveClient.files().create(fileMetadata, mediaContent)
                .setSupportsAllDrives(true)
                .setFields("id, webViewLink")
                .execute();

        log.info("Archivo subido a Drive: '{}' -> {}", fileName, uploaded.getWebViewLink());
        return uploaded.getWebViewLink();
    }

    private String findFolderIdByName(String folderName, String parentFolderId) throws IOException {
        String query = String.format(
                "name = '%s' and mimeType = '%s' and '%s' in parents and trashed = false",
                escapeDriveQueryValue(folderName),
                FOLDER_MIME_TYPE,
                parentFolderId
        );

        FileList result = driveClient.files().list()
                .setQ(query)
                .setCorpora("allDrives")
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id,name)")
                .setPageSize(1)
                .execute();

        List<File> folders = result.getFiles();
        if (folders == null || folders.isEmpty()) {
            return null;
        }
        return folders.get(0).getId();
    }

    private String escapeDriveQueryValue(String value) {
        return value == null ? "" : value.replace("'", "\\'");
    }
}
