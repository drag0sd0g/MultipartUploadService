package com.tools.fsserver.storage;

import com.tools.fsserver.exception.FileNameNotPresentOnServerException;
import com.tools.fsserver.exception.FileNamePresentOnServerException;
import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Startup
@ApplicationScoped
public class FileSystemStorageService implements IStorageService {

  private static final Logger LOG = Logger.getLogger(FileSystemStorageService.class);
  private final Path permanentStoragePath;

  @Inject
  public FileSystemStorageService(
      @ConfigProperty(name = "fsserver.uploadedFilesPath") String permanentStoragePath)
      throws IOException {
    this.permanentStoragePath = Paths.get(permanentStoragePath);
    if (Files.notExists(this.permanentStoragePath)) {
      Files.createDirectories(this.permanentStoragePath);
    }
  }

  public Set<String> listUploadedFiles() throws IOException {
    try (Stream<Path> stream = Files.list(this.permanentStoragePath)) {
      Set<String> uploadedFileNames =
          stream
              .filter(file -> !Files.isDirectory(file))
              .map(java.nio.file.Path::getFileName)
              .map(java.nio.file.Path::toString)
              .collect(Collectors.toSet());
      LOG.debug("Returning list of uploaded files: " + uploadedFileNames);
      return uploadedFileNames;
    }
  }

  public void storeFile(String fileName, Path uploadSourcePath)
      throws FileNamePresentOnServerException, IOException {
    java.nio.file.Path destinationPath = Paths.get(this.permanentStoragePath.toString(), fileName);
    LOG.debug(
        "Copying file from source path "
            + uploadSourcePath.toAbsolutePath()
            + " to destination path "
            + destinationPath.toAbsolutePath());
    Path uploadedFinalPath;
    try {
      uploadedFinalPath = Files.copy(uploadSourcePath, destinationPath);
    } catch (FileAlreadyExistsException faex) {
      String errMsg = "There already exists a file called " + destinationPath.getFileName();
      LOG.error(errMsg);
      throw new FileNamePresentOnServerException(errMsg);
    }
    LOG.debug("Uploaded file at path " + uploadedFinalPath.toAbsolutePath());
  }

  public void deleteFile(String fileNameToDelete)
      throws FileNameNotPresentOnServerException, IOException {
    java.nio.file.Path pathToFile =
        Paths.get(this.permanentStoragePath.toString(), fileNameToDelete);
    LOG.debug("Attempting to delete uploaded file at location " + pathToFile);
    boolean deleted = Files.deleteIfExists(pathToFile);
    if (deleted) {
      LOG.debug("Successfully deleted file at location " + pathToFile);
    } else {
      String errMsg = "There is no already uploaded file called " + fileNameToDelete;
      LOG.error(errMsg);
      throw new FileNameNotPresentOnServerException(errMsg);
    }
  }
}
